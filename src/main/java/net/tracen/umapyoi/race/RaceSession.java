package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.block.BlockRegistry;
import net.tracen.umapyoi.block.Gate;
import net.tracen.umapyoi.block.GateDoor;
import net.tracen.umapyoi.race.track.TrackBuilder;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.utils.ThreeBlockPart;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * One race being run on the race world's course, with any number of players and computer runners
 * ({@link RaceOpponent}). The fixed start is the finish line in front of the stand, so a race as long as
 * the lap starts and ends there. Everyone waits in a starting stall while a countdown runs, the gates
 * open, and they are taken off the course a few seconds later. There is at most one at a time.
 * <p>
 * Distance is counted, not inferred from where a runner stands: every tick adds how far they moved along
 * the course, and a jump of more than {@link RaceRunner#MAX_STEP} blocks (a teleport, a cut through the
 * infield) counts for nothing. Running backwards takes distance off. So skipping part of the course never
 * helps, which is what checkpoints would be for.
 * <p>
 * Stamina: sprinting costs one point per block, each jump costs a few more, walking (cruising)
 * costs nothing, and standing still for a moment slowly brings some back. Below the low line the
 * Umamusume speed bonus drops ({@link RaceStats#bonusLeft}), the plain player's speed doesn't. With
 * none left the runner is exhausted until some of it is back: cruising slows further, sprinting is
 * hardly faster than cruising, and there is no jumping and no action point recovery. So the last
 * point of stamina is worth keeping. The action points are filled up before the start.
 * The client gets the result every tick and moves at that speed ({@link RaceSpeed}).
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
final class RaceSession {
    static final int STALLS = 18;
    /** Standing in a stall, the middle of which is half a block behind the line. */
    static final double START = -0.5;
    /** How fast a plain player sprints, in blocks per second: what computer runners are built on. */
    static final double PLAIN_SPRINT = 0.1 * 1.3 * RaceSpeed.BLOCKS_PER_SECOND;
    /** The race goes on this long after the last one is in, so they're seen crossing the line. */
    private static final int LINGER_TICKS = 40;
    /** Gives up after this long. */
    private static final int TIME_LIMIT_TICKS = 20 * 60 * 10;
    /** Keeps the whole course loaded during a race. */
    private static final TicketType<ChunkPos> COURSE_TICKET = TicketType.create("umapyoi_race",
            Comparator.comparingLong(ChunkPos::toLong), 100);

    /** What whoever holds the race wants to hear about. */
    interface Listener {
        /** The gates open. */
        default void opened() {
        }

        /** A player crossed the line. */
        default void finished(ServerPlayer player, int place, double time) {
        }

        /** A player left, or ran out of time. */
        default void gaveUp(ServerPlayer player) {
        }
    }

    /**
     * Someone's place in the field.
     *
     * @param time   seconds; below zero if they didn't finish
     * @param player null for a computer runner
     */
    record Place(Component name, double time, double progress, int lane, @Nullable ServerPlayer player) {
    }

    @Nullable
    private static RaceSession current;

    private final ServerLevel level;
    private final TrackGeometry track;
    private final BlockPos origin;
    private final RaceSetup setup;
    private final double distance;
    private final double metersPerBlock;
    private final RaceLayout layout;
    private final int stragglerTicks;
    private final Listener listener;
    private final List<BlockPos> gateBlocks = new ArrayList<>();
    private final List<BlockPos> doors = new ArrayList<>();
    private final List<Racer> racers = new ArrayList<>();
    private final List<RaceOpponent> opponents = new ArrayList<>();
    private int ticks;
    /** Ticks until the gates open; below zero while nobody counts. */
    private int countdown = -1;
    private int openTick = -1;
    /** When the winner, and the last one, crossed the line. */
    private int firstTick = -1;
    private int allTick = -1;
    private boolean over;
    private boolean gathered;

    /** @param stragglerTicks how long the rest get once the winner is in; 0 = as long as they like */
    RaceSession(ServerLevel level, TrackGeometry track, BlockPos origin, RaceSetup setup, int stragglerTicks, Listener listener) {
        this.level = level;
        this.track = track;
        this.origin = origin;
        this.setup = setup;
        this.metersPerBlock = UmapyoiConfig.RACE_METERS_PER_BLOCK.get();
        this.distance = setup.distanceMeters() / metersPerBlock;
        this.layout = new RaceLayout(track, distance);
        this.stragglerTicks = stragglerTicks;
        this.listener = listener;
        for (int lane = 0; lane < stalls(track); lane++) {
            double offset = stallOffset(track, lane);
            TrackGeometry.Point stall = track.pointAt(track.lap() - 0.5, offset);
            TrackGeometry.Point door = track.pointAt(0.5, offset);
            placeStall(cell(stall), cell(door), Direction.fromYRot(stall.yaw()));
        }
        current = this;
    }

    /** How many stalls there's room for. */
    static int stalls(TrackGeometry track) {
        return Math.min(STALLS, (int) track.width());
    }

    /** The middle of a stall (0 = innermost), as an offset from the course's reference line. */
    static double stallOffset(TrackGeometry track, int lane) {
        return track.inner() + 0.5 + lane;
    }

    @Nullable
    static RaceSession current() {
        return current;
    }

    /** Whether this player is running a race and out of stamina. */
    static boolean exhausted(ServerPlayer player) {
        Racer racer = current == null ? null : current.racer(player);
        return racer != null && racer.runner.exhausted();
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (current == null || !(event.getEntity() instanceof ServerPlayer player))
            return;
        Racer racer = current.racer(player);
        if (racer != null)
            racer.jump();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (current != null)
            current.cleanUp();
    }

    // ---- the field

    RaceSetup setup() {
        return setup;
    }

    double distance() {
        return distance;
    }

    RaceLayout layout() {
        return layout;
    }

    boolean opened() {
        return openTick >= 0;
    }

    /** Seconds since the gates opened. */
    double clock() {
        return openTick < 0 ? 0 : (ticks - openTick) / 20.0;
    }

    /** Ticks left until whoever is still running is too late; below zero if nobody is waiting for that. */
    int stragglerTicksLeft() {
        return firstTick < 0 || stragglerTicks <= 0 || allTick >= 0 ? -1 : Math.max(0, stragglerTicks - (ticks - firstTick));
    }

    void add(RaceOpponent opponent) {
        opponents.add(opponent);
        if (gathered)
            opponent.appear(level, track, origin, START);
    }

    /** The computer runners turn up: in their stalls, or walking up to them. */
    void gather(boolean walk) {
        if (gathered)
            return;
        gathered = true;
        for (RaceOpponent opponent : opponents)
            if (walk)
                opponent.comeUp(level, track, origin, START);
            else
                opponent.appear(level, track, origin, START);
    }

    void remove(RaceOpponent opponent) {
        opponents.remove(opponent);
        opponent.disappear();
    }

    /** Puts a player into a stall. */
    void add(ServerPlayer player, int lane) {
        racers.add(new Racer(player, lane));
    }

    /** Whether this player is in the race and hasn't finished or left it. */
    boolean running(ServerPlayer player) {
        return racer(player) != null;
    }

    boolean finished(ServerPlayer player) {
        return racers.stream().anyMatch(racer -> racer.player == player && racer.time >= 0);
    }

    /** Anyone among the players who finished. */
    boolean anyFinished() {
        return racers.stream().anyMatch(racer -> racer.time >= 0);
    }

    @Nullable
    private Racer racer(ServerPlayer player) {
        for (Racer racer : racers)
            if (racer.player == player && racer.running())
                return racer;
        return null;
    }

    /** Takes a player out of the race. @return false if they weren't running in it */
    boolean giveUp(ServerPlayer player) {
        Racer racer = racer(player);
        if (racer == null)
            return false;
        racer.out = true;
        racer.release();
        if (openTick < 0)
            racers.remove(racer);
        listener.gaveUp(player);
        return true;
    }

    /** Everyone in the order they came in, or are lying now. */
    List<Place> places() {
        List<Place> places = new ArrayList<>();
        for (Racer racer : racers)
            places.add(new Place(racer.player.getDisplayName(), racer.out ? -1 : racer.time, racer.out ? -1 : racer.runner.progress(), racer.lane, racer.player));
        for (RaceOpponent opponent : opponents)
            places.add(new Place(opponent.name(), opponent.time(), opponent.runner().progress(), opponent.lane(track), null));
        places.sort((a, b) -> a.time() >= 0 && b.time() >= 0 ? Double.compare(a.time(), b.time())
                : a.time() >= 0 ? -1 : b.time() >= 0 ? 1 : Double.compare(b.progress(), a.progress()));
        return places;
    }

    private int fieldSize() {
        return racers.size() + opponents.size();
    }

    /** Everyone still running. */
    private List<RaceTraffic.Body> bodies() {
        List<RaceTraffic.Body> bodies = new ArrayList<>();
        for (Racer racer : racers)
            if (racer.running())
                bodies.add(racer.body());
        for (RaceOpponent opponent : opponents)
            if (!opponent.finished())
                bodies.add(opponent.body());
        return bodies;
    }

    private List<RaceRunner> runners() {
        List<RaceRunner> runners = new ArrayList<>();
        for (Racer racer : racers)
            if (racer.running())
                runners.add(racer.runner);
        for (RaceOpponent opponent : opponents)
            if (!opponent.finished() && opponent.runner().started())
                runners.add(opponent.runner());
        return runners;
    }

    /** Everyone saves up for the last leg, by their place in the field. */
    static void chargeUp(List<RaceRunner> running) {
        List<RaceRunner> order = new ArrayList<>(running);
        order.sort(Comparator.comparingDouble(RaceRunner::progress).reversed());
        for (int i = 0; i < order.size(); i++)
            order.get(i).chargeUp(i + 1, order.size(), i == 0 ? 0 : order.get(i - 1).progress() - order.get(i).progress());
    }

    // ---- running it

    /** Starts the countdown to the gates opening. */
    void begin(int ticks) {
        countdown = ticks;
    }

    /** One tick. @return true when the race is over */
    boolean tick() {
        ticks++;
        if (ticks % 40 == 1)
            keepCourseLoaded();
        for (Racer racer : List.copyOf(racers))
            if (racer.running() && (racer.player.hasDisconnected() || !RaceWorld.isRaceWorld(racer.player.level())))
                giveUp(racer.player);
        // Working out everyone's pacing takes a moment: one of them per tick.
        if (openTick < 0)
            for (RaceOpponent opponent : opponents)
                if (opponent.plan(setup, PLAIN_SPRINT, track) && countdown != 0)
                    break;
        if (openTick < 0) {
            for (RaceOpponent opponent : opponents)
                opponent.walkIn(START);
            if (countdown >= 0)
                countdown();
            return false;
        }

        int sinceOpen = ticks - openTick;
        if (sinceOpen == UmapyoiConfig.RACE_GATE_CLEAR_SECONDS.get() * 20)
            removeGates(true);
        if (sinceOpen % 10 == 0)
            showFinishLine();

        List<RaceTraffic.Body> everyone = bodies();
        chargeUp(runners());
        for (RaceOpponent opponent : opponents)
            opponent.tick(track, distance, sinceOpen, START, everyone);
        for (Racer racer : racers)
            if (racer.running())
                racer.tick(sinceOpen, everyone);
        // Only now is it known who of those who crossed the line this tick was first.
        for (Racer racer : racers)
            if (racer.time >= 0 && !racer.told)
                racer.finish();

        boolean all = racers.stream().noneMatch(Racer::running) && opponents.stream().allMatch(RaceOpponent::finished);
        if (firstTick < 0 && (anyFinished() || opponents.stream().anyMatch(RaceOpponent::finished)))
            firstTick = ticks;
        if (all && allTick < 0)
            allTick = ticks;
        if (allTick >= 0)
            over = ticks - allTick >= LINGER_TICKS || fieldSize() == 1;
        else if (sinceOpen > TIME_LIMIT_TICKS || firstTick >= 0 && stragglerTicks > 0 && ticks - firstTick >= stragglerTicks)
            over = true;
        if (over)
            for (Racer racer : racers)
                if (racer.running())
                    giveUp(racer.player);
        return over;
    }

    /** After the race: computer runners run on, slowly. */
    void coast() {
        ticks++;
        if (ticks % 40 == 1)
            keepCourseLoaded();
        for (RaceOpponent opponent : opponents)
            opponent.tick(track, distance, ticks - openTick, START, List.of());
    }

    private void countdown() {
        for (Racer racer : racers)
            racer.hold();
        if (countdown > 0) {
            if (countdown % 20 == 0 && countdown / 20 <= UmapyoiConfig.RACE_COUNTDOWN_SECONDS.get())
                for (ServerPlayer player : level.players()) {
                    title(player, Component.literal(String.valueOf(countdown / 20)), Component.empty());
                    player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            countdown--;
            return;
        }
        for (BlockPos door : doors) {
            BlockState state = level.getBlockState(door);
            if (state.getBlock() instanceof GateDoor)
                level.setBlock(door, state.setValue(GateDoor.OPEN, true), Block.UPDATE_CLIENTS);
        }
        BlockPos middle = doors.get(doors.size() / 2);
        if (level.getBlockState(middle).getBlock() instanceof GateDoor gateDoor)
            gateDoor.playSound(null, level, middle, true);
        for (ServerPlayer player : level.players()) {
            title(player, Component.translatable("umapyoi.race.trial.go"), Component.empty());
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
        }
        openTick = ticks;
        for (Racer racer : racers)
            racer.open();
        listener.opened();
    }

    private static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private void keepCourseLoaded() {
        ChunkPos from = new ChunkPos(origin.offset((int) -track.halfLengthX(4), 0, (int) -track.halfLengthZ(4)));
        ChunkPos to = new ChunkPos(origin.offset((int) track.halfLengthX(4), 0, (int) track.halfLengthZ(4)));
        ChunkPos.rangeClosed(from, to).forEach(pos -> level.getChunkSource().addRegionTicket(COURSE_TICKET, pos, 2, pos));
    }

    /** Where a race that isn't a whole number of laps ends: a line of sparkles across the surface. */
    private void showFinishLine() {
        double at = track.wrap(distance);
        if (at < 1 || at > track.lap() - 1)
            return;
        for (double offset = track.inner() + 0.5; offset < track.outer(); offset += 1) {
            TrackGeometry.Point point = track.pointAt(at, offset);
            level.sendParticles(ParticleTypes.END_ROD, origin.getX() + point.x(), origin.getY() + 1.3,
                    origin.getZ() + point.z(), 1, 0, 0.2, 0, 0);
        }
    }

    /** Takes everything off the course and lets everyone go. */
    void cleanUp() {
        removeGates(false);
        opponents.forEach(RaceOpponent::disappear);
        racers.forEach(Racer::release);
        if (current == this)
            current = null;
    }

    // ---- the stalls

    private BlockPos cell(TrackGeometry.Point point) {
        return new BlockPos(origin.getX() + (int) Math.floor(point.x()), origin.getY() + 1,
                origin.getZ() + (int) Math.floor(point.z()));
    }

    private void placeStall(BlockPos stall, BlockPos door, Direction facing) {
        BlockState gate = BlockRegistry.GATE.get().defaultBlockState().setValue(Gate.FACING, facing);
        ThreeBlockPart[] parts = ThreeBlockPart.values();
        for (int h = 0; h < parts.length; h++) {
            BlockPos pos = stall.above(h);
            level.setBlock(pos, gate.setValue(Gate.PART, parts[h]), TrackBuilder.FLAGS);
            gateBlocks.add(pos);
        }
        // Opened by setting its state, not by redstone; left unpowered so no neighbour update closes it again.
        BlockState closed = BlockRegistry.GATE_DOOR.get().defaultBlockState().setValue(GateDoor.FACING, facing)
                .setValue(GateDoor.OPEN, false).setValue(GateDoor.POWERED, false);
        level.setBlock(door, closed, TrackBuilder.FLAGS);
        gateBlocks.add(door);
        doors.add(door);
    }

    private void removeGates(boolean puff) {
        for (BlockPos pos : gateBlocks) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof Gate || state.getBlock() instanceof GateDoor)
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), TrackBuilder.FLAGS);
        }
        if (puff)
            for (BlockPos door : doors)
                level.sendParticles(ParticleTypes.CLOUD, door.getX() + 0.5, door.getY() + 1.0, door.getZ() + 0.5,
                        6, 0.6, 0.8, 0.4, 0.02);
        gateBlocks.clear();
    }

    static String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        return String.format("%d:%05.2f", minutes, seconds - minutes * 60);
    }

    /** A player in the race. */
    private final class Racer {
        private final ServerPlayer player;
        private final int lane;
        private final RaceStats stats;
        private final RaceRunner runner;
        private final double plainSprint;
        private final GameType previousMode;
        private final double stallX;
        private final double stallZ;
        private double lastX;
        private double lastZ;
        private double lastSpot;
        private double offset;
        /** Their speed along the course, smoothed, in blocks per second: what others behind them are held to. */
        private double pace;
        /** Their time in seconds; below zero while still running. */
        private double time = -1;
        private boolean told;
        private boolean out;
        private boolean released;

        Racer(ServerPlayer player, int lane) {
            this.player = player;
            this.lane = lane;
            this.previousMode = player.gameMode.getGameModeForPlayer();
            ItemStack soul = UmapyoiAPI.getUmaSoul(player);
            this.stats = RaceStats.of(soul, RaceStyle.of(soul, level), setup.surface(), setup.category());
            this.plainSprint = PLAIN_SPRINT;
            this.runner = new RaceRunner(setup, stats, layout, plainSprint, RaceStyle.aptitudes(soul, level));
            if (UmaSoulUtils.hasSoulData(soul))
                UmaSoulUtils.setActionPoint(soul, UmaSoulUtils.getMaxActionPoint(soul));
            TrackGeometry.Point stall = track.pointAt(track.lap() - 0.5, stallOffset(track, lane));
            BlockPos cell = cell(stall);
            this.stallX = cell.getX() + 0.5;
            this.stallZ = cell.getZ() + 0.5;
            if (previousMode != GameType.ADVENTURE)
                player.setGameMode(GameType.ADVENTURE);
            player.teleportTo(level, stallX, origin.getY() + 1, stallZ, stall.yaw(), 0);
            player.setDeltaMovement(0, 0, 0);
        }

        boolean running() {
            return time < 0 && !out;
        }

        /** Stay in the stall: the back of it is open. */
        void hold() {
            if (Math.hypot(player.getX() - stallX, player.getZ() - stallZ) > 0.35)
                player.connection.teleport(stallX, origin.getY() + 1, stallZ, player.getYRot(), player.getXRot());
        }

        void open() {
            lastX = player.getX();
            lastZ = player.getZ();
            TrackGeometry.Spot spot = here();
            lastSpot = spot.progress();
            offset = spot.offset();
            // Standing just behind the line: a little below zero.
            runner.start(spot.progress() > track.lap() / 2 ? spot.progress() - track.lap() : spot.progress());
            PacketDistributor.sendToPlayer(player, runner.state(skill(), RaceTraffic.FREE));
        }

        void tick(int sinceOpen, List<RaceTraffic.Body> everyone) {
            double before = runner.progress();
            TrackGeometry.Spot spot = here();
            double step = spot.progress() - lastSpot;
            double lap = track.lap();
            if (step > lap / 2)
                step -= lap;
            else if (step < -lap / 2)
                step += lap;
            lastSpot = spot.progress();
            offset = spot.offset();
            if (Math.abs(step) <= RaceRunner.MAX_STEP) {
                runner.advance(step);
                pace += (step * 20 - pace) * 0.3;
            }
            RaceStatePayload state = runner.state(skill(), RaceTraffic.limit(body(), everyone));
            runner.move(state, player.isSprinting());
            double moved = Math.hypot(player.getX() - lastX, player.getZ() - lastZ);
            lastX = player.getX();
            lastZ = player.getZ();
            if (runner.useStamina(state, moved))
                gasp();
            PacketDistributor.sendToPlayer(player, state);

            if (runner.progress() >= distance && step > 0) {
                // Crossed during this tick: work out when, between the last tick and this one.
                double fraction = (distance - before) / step;
                time = (sinceOpen - 1 + fraction) / 20.0;
                return;
            }
            showHud(state, sinceOpen);
        }

        RaceTraffic.Body body() {
            return new RaceTraffic.Body(player, runner.progress(), offset, pace);
        }

        private TrackGeometry.Spot here() {
            return track.locate(player.getX() - origin.getX(), player.getZ() - origin.getZ());
        }

        /** Speed skills give the Speed effect; in a race it multiplies only the Umamusume bonus. */
        private double skill() {
            MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
            return speed == null ? 1 : 1 + UmapyoiConfig.RACE_SKILL_SPEED_PER_LEVEL.get() * (speed.getAmplifier() + 1);
        }

        private void gasp() {
            player.playNotifySound(SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 0.8F);
        }

        void jump() {
            if (openTick >= 0 && runner.jump())
                gasp();
        }

        /** Where they are in the field right now (1 = first). */
        private int rank() {
            int rank = 1;
            for (Racer other : racers)
                if (other != this && !other.out && ahead(other.time, other.runner.progress()))
                    rank++;
            for (RaceOpponent opponent : opponents)
                if (ahead(opponent.time(), opponent.runner().progress()))
                    rank++;
            return rank;
        }

        private boolean ahead(double otherTime, double otherProgress) {
            return time >= 0 ? otherTime >= 0 && otherTime < time : otherTime >= 0 || otherProgress > runner.progress();
        }

        void finish() {
            told = true;
            int rank = rank();
            String clock = formatTime(time);
            String meters = String.format("%.0f", setup.distanceMeters());
            boolean alone = fieldSize() == 1;
            title(player, alone ? Component.literal(clock) : Component.translatable("umapyoi.race.rank", rank, fieldSize()).append("  " + clock),
                    Component.translatable(alone ? "umapyoi.race.trial.finish_subtitle" : "umapyoi.race.result.header", meters));
            if (alone)
                player.sendSystemMessage(Component.translatable("umapyoi.race.trial.result", meters, clock));
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            PacketDistributor.sendToPlayer(player, RaceStatePayload.INACTIVE);
            if (!alone)
                player.displayClientMessage(Component.translatable("umapyoi.race.result.waiting"), true);
            listener.finished(player, rank, time);
        }

        /** Back to normal running. */
        void release() {
            if (released)
                return;
            released = true;
            if (player.hasDisconnected())
                return;
            PacketDistributor.sendToPlayer(player, RaceStatePayload.INACTIVE);
            if (player.gameMode.getGameModeForPlayer() != previousMode)
                player.setGameMode(previousMode);
        }

        /**
         * The action bar, with what helps while testing: phase and place on the course, the style
         * they run in now, their aptitude for it and what they've saved up, whether they are speeding up
         * or slowing down, and the speed being aimed at, then distance left and time.
         */
        private void showHud(RaceStatePayload state, int sinceOpen) {
            RacePhase phase = runner.phase();
            boolean sprinting = player.isSprinting();
            String motionKey = RaceMotion.blocked(state, sprinting) ? "umapyoi.race.motion.blocked"
                    : runner.exhausted() ? "umapyoi.race.motion.exhausted"
                    : runner.accelerating(state, sprinting) ? "umapyoi.race.motion.accelerating"
                    : runner.decelerating(state, sprinting) ? "umapyoi.race.motion.decelerating"
                    : sprinting ? "umapyoi.race.motion.full" : "umapyoi.race.motion.cruising";
            double target = runner.wanted(state, sprinting);
            double remaining = Math.max(0, distance - runner.progress()) * metersPerBlock;
            MutableComponent place = fieldSize() == 1 ? Component.empty()
                    : Component.translatable("umapyoi.race.rank", rank(), fieldSize()).append(" | ");
            player.displayClientMessage(place.append(Component.translatable(phase.key()))
                    .append(" · ").append(Component.translatable(layout.sectionKey(runner.progress())))
                    .append(" | ").append(Component.translatable(RaceStyle.key(runner.zone()))).append(" ").append(runner.zoneAptitude().styledComponent())
                    .append(" ").append(Component.translatable(runner.lostTouch() ? "umapyoi.race.charge.lost" : "umapyoi.race.charge", Math.round(runner.charge() * 100)))
                    .append(" | ").append(Component.translatable(motionKey))
                    .append(" → ").append(Component.translatable("umapyoi.race.speed", String.format("%.2f", target)))
                    .append(" | ").append(Component.translatable("umapyoi.race.trial.hud",
                            String.format("%.0f", remaining), formatTime(sinceOpen / 20.0))), true);
        }
    }
}
