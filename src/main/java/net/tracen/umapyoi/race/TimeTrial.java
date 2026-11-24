package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
 * A race on the race world's course: a player alone against the clock, or with computer runners
 * ({@link RaceOpponent}). The course is repainted first if the race's surface or
 * handedness isn't what is there. The fixed start is the finish line in front of the stand, so a race
 * as long as the lap starts and ends there. The runner waits in a starting stall while a countdown
 * runs, the gates open, and they are taken off the course a few seconds later.
 * <p>
 * Distance is counted, not inferred from where the runner stands: every tick adds how far they
 * moved along the course, and a jump of more than {@link RaceRunner#MAX_STEP} blocks (a teleport, a cut
 * through the infield) counts for nothing. Running backwards takes distance off. So skipping
 * part of the course never helps, which is what checkpoints would be for.
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
public final class TimeTrial {
    static final int STALLS = 18;
    /** Once the player is in, computer runners still out there get this long. */
    private static final int STRAGGLER_TICKS = 20 * 40;
    /** Keeps the whole course loaded during a race with computer runners. */
    private static final TicketType<ChunkPos> COURSE_TICKET = TicketType.create("umapyoi_race",
            Comparator.comparingLong(ChunkPos::toLong), 100);
    /** Gives up after this long without finishing. */
    private static final int TIME_LIMIT_TICKS = 20 * 60 * 10;

    public enum Start {
        STARTED, PREPARING, BUSY, NOT_THERE, NO_COURSE
    }

    @Nullable
    private static Run current;
    /** The race the course is being repainted for, and who will run it; null when there's none. */
    @Nullable
    private static RaceSetup preparing;
    @Nullable
    private static ServerPlayer waiting;

    private TimeTrial() {
    }

    /**
     * Starts a race, repainting the course first if it isn't laid out for it.
     *
     * @param opponents how many computer runners
     * @param statLevel what their stats are around; 0 means like the player's
     */
    public static Start start(ServerPlayer player, RaceSetup setup, int opponents, int statLevel) {
        if (current != null || TrackBuilder.busy())
            return Start.BUSY;
        if (!RaceWorld.isRaceWorld(player.level()))
            return Start.NOT_THERE;
        ServerLevel level = (ServerLevel) player.level();
        RacecourseData data = RacecourseData.get(level);
        TrackGeometry track = data.track();
        if (!data.built() || track == null)
            return Start.NO_COURSE;
        boolean dressing = RaceWorld.dress(level, setup,
                percent -> {
                    if (!player.hasDisconnected())
                        player.displayClientMessage(Component.translatable("umapyoi.race.trial.preparing", percent), true);
                },
                () -> {
                    if (waiting != player)
                        return; // cancelled in the meantime
                    preparing = null;
                    waiting = null;
                    if (current == null && !player.hasDisconnected() && player.level() == level)
                        current = new Run(player, level, data.track(), data.origin(), setup, opponents, statLevel);
                });
        if (dressing) {
            preparing = setup;
            waiting = player;
            return Start.PREPARING;
        }
        current = new Run(player, level, track, data.origin(), setup, opponents, statLevel);
        return Start.STARTED;
    }

    public static boolean running() {
        return current != null;
    }

    /** Whether this player is running a trial and out of stamina. */
    public static boolean exhausted(ServerPlayer player) {
        return current != null && current.player == player && current.runner.exhausted();
    }

    /** The weather of the race being run or prepared; sunny otherwise. */
    public static RaceWeather weather() {
        return current != null ? current.setup.weather() : preparing != null ? preparing.weather() : RaceWeather.SUNNY;
    }

    /** @return false if this player has no trial running or being prepared */
    public static boolean cancel(ServerPlayer player) {
        if (waiting == player) {
            preparing = null;
            waiting = null;
            if (!player.hasDisconnected())
                player.sendSystemMessage(Component.translatable("umapyoi.race.trial.cancelled"));
            return true;
        }
        if (current == null || current.player != player)
            return false;
        current.cancel();
        current = null;
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (current != null && current.tick())
            current = null;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            cancel(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            cancel(player);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            cancel(player);
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (current != null && !event.getEntity().level().isClientSide() && event.getEntity() == current.player)
            current.jump();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        preparing = null;
        waiting = null;
        if (current != null) {
            current.cleanUp();
            current = null;
        }
    }

    static String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        return String.format("%d:%05.2f", minutes, seconds - minutes * 60);
    }

    private static final class Run {
        /** Standing in a stall, the middle of which is half a block behind the line. */
        private static final double START = -0.5;

        private final ServerPlayer player;
        private final ServerLevel level;
        private final TrackGeometry track;
        private final BlockPos origin;
        private final RaceSetup setup;
        private final double distance;
        private final double metersPerBlock;
        private final GameType previousMode;
        private final List<BlockPos> gateBlocks = new ArrayList<>();
        private final List<BlockPos> doors = new ArrayList<>();
        private final double stallX;
        private final double stallZ;
        private final float yaw;
        private final int countdownTicks;
        private final RaceStats stats;
        private final RaceRunner runner;
        private final double plainSprint;
        private final List<RaceOpponent> opponents;
        private double lastX;
        private double lastZ;
        private int ticks;
        private int openTick = -1;
        private double lastSpot;
        private double offset;
        /** The player's time in seconds; below zero while they're still running. */
        private double time = -1;
        private int finishTick;

        Run(ServerPlayer player, ServerLevel level, TrackGeometry track, BlockPos origin, RaceSetup setup, int opponentCount, int statLevel) {
            this.player = player;
            this.level = level;
            this.track = track;
            this.origin = origin;
            this.metersPerBlock = UmapyoiConfig.RACE_METERS_PER_BLOCK.get();
            this.setup = setup;
            this.distance = setup.distanceMeters() / metersPerBlock;
            this.previousMode = player.gameMode.getGameModeForPlayer();
            this.countdownTicks = UmapyoiConfig.RACE_COUNTDOWN_SECONDS.get() * 20;
            ItemStack soul = UmapyoiAPI.getUmaSoul(player);
            this.stats = RaceStats.of(soul, RaceStyle.of(soul, level), setup.surface(), setup.category());
            var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            this.plainSprint = (attribute == null ? 0.1 : attribute.getBaseValue()) * 1.3 * RaceSpeed.BLOCKS_PER_SECOND;
            RaceLayout layout = new RaceLayout(track, distance);
            this.runner = new RaceRunner(setup, stats, layout, plainSprint);
            if (UmaSoulUtils.hasSoulData(soul))
                UmaSoulUtils.setActionPoint(soul, UmaSoulUtils.getMaxActionPoint(soul));

            // A row of stalls standing on the finish line, doors just past it, innermost stall first. The
            // field draws the inner ones by lot; a lone runner gets the innermost.
            double lap = track.lap();
            int stalls = Math.min(STALLS, (int) track.width());
            List<Integer> draw = new ArrayList<>();
            for (int lane = 0; lane <= Math.min(opponentCount, stalls - 1); lane++)
                draw.add(lane);
            for (int i = draw.size() - 1; i > 0; i--)
                Collections.swap(draw, i, level.getRandom().nextInt(i + 1));
            int own = draw.removeFirst();
            TrackGeometry.Point mine = null;
            for (int lane = 0; lane < stalls; lane++) {
                double offset = track.inner() + 0.5 + lane;
                TrackGeometry.Point stall = track.pointAt(lap - 0.5, offset);
                TrackGeometry.Point door = track.pointAt(0.5, offset);
                if (lane == own)
                    mine = stall;
                placeStall(cell(stall), cell(door), Direction.fromYRot(stall.yaw()));
            }
            BlockPos ownCell = cell(mine);
            this.stallX = ownCell.getX() + 0.5;
            this.stallZ = ownCell.getZ() + 0.5;
            this.yaw = mine.yaw();

            List<Double> theirs = draw.stream().map(lane -> track.inner() + 0.5 + lane).toList();
            this.opponents = RaceOpponent.field(level, track, setup, layout, distance, plainSprint,
                    statLevel > 0 ? statLevel : levelOf(soul), theirs,
                    soul, level.getRandom());
            keepCourseLoaded();
            for (RaceOpponent opponent : opponents)
                opponent.appear(level, track, origin, START);

            if (previousMode != GameType.ADVENTURE)
                player.setGameMode(GameType.ADVENTURE);
            player.teleportTo(level, stallX, origin.getY() + 1, stallZ, yaw, 0);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
            sendStats();
        }

        /** The average of a soul's stats. */
        private static int levelOf(ItemStack soul) {
            return UmaSoulUtils.hasSoulData(soul)
                    ? (int) Math.round(Arrays.stream(UmaSoulUtils.getProperty(soul).toArray()).average().orElse(1)) : 1;
        }

        private void keepCourseLoaded() {
            if (opponents.isEmpty())
                return;
            ChunkPos from = new ChunkPos(origin.offset((int) -track.halfLengthX(4), 0, (int) -track.halfLengthZ(4)));
            ChunkPos to = new ChunkPos(origin.offset((int) track.halfLengthX(4), 0, (int) track.halfLengthZ(4)));
            ChunkPos.rangeClosed(from, to).forEach(pos -> level.getChunkSource().addRegionTicket(COURSE_TICKET, pos, 2, pos));
        }

        /** The race, and what it makes of this soul: shown once at the start, for testing. */
        private void sendStats() {
            RaceGround ground = setup.ground();
            player.sendSystemMessage(Component.translatable("umapyoi.race.trial.setup",
                    String.format("%.0f", setup.distanceMeters()),
                    Component.translatable("tooltip.umapyoi.race.distance." + setup.category().name().toLowerCase(Locale.ROOT)),
                    Component.translatable(surfaceKey()),
                    Component.translatable(setup.leftHanded() ? "umapyoi.race.direction.left" : "umapyoi.race.direction.right"),
                    Component.translatable(setup.weather().key()),
                    Component.translatable(ground.key()),
                    String.format("%.2f", ground.accel()),
                    String.format("%.2f", ground.drain())));
            player.sendSystemMessage(Component.translatable("umapyoi.race.trial.stats",
                    String.format("%.1f", stats.bonusRatio() * 100),
                    String.format("%.2f", stats.accel() * ground.accel()),
                    String.format("%.0f", stats.stamina()),
                    String.format("%.0f", stats.emptyFloor() * 100),
                    String.format("%.2f", stats.mood()),
                    Component.translatable(surfaceKey()),
                    String.format("%.2f", stats.surfaceFactor()),
                    String.format("%.2f", stats.distanceFactor())));
        }

        private String surfaceKey() {
            return "umapyoi.race.surface." + setup.surface().name().toLowerCase(Locale.ROOT);
        }

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

        /** @return true when the trial is over */
        boolean tick() {
            ticks++;
            if (player.hasDisconnected() || !RaceWorld.isRaceWorld(player.level())) {
                cancel();
                return true;
            }
            if (openTick < 0)
                return countdown();

            int sinceOpen = ticks - openTick;
            if (sinceOpen == UmapyoiConfig.RACE_GATE_CLEAR_SECONDS.get() * 20)
                removeGates(true);
            if (sinceOpen % 10 == 0)
                showFinishLine();
            if (sinceOpen > TIME_LIMIT_TICKS) {
                cancel();
                return true;
            }

            runOpponents(sinceOpen);
            if (time >= 0)
                return waitForOthers();

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
            if (Math.abs(step) <= RaceRunner.MAX_STEP)
                runner.advance(step);
            RaceStatePayload state = runner.state(skill());
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
                finish((sinceOpen - 1 + fraction) / 20.0);
                return opponents.isEmpty();
            }
            showHud(state, sinceOpen);
            return false;
        }

        private void runOpponents(int sinceOpen) {
            if (opponents.isEmpty())
                return;
            if (ticks % 40 == 0)
                keepCourseLoaded();
            List<RaceOpponent.Other> everyone = new ArrayList<>();
            if (time < 0)
                everyone.add(new RaceOpponent.Other(runner.progress(), offset));
            for (RaceOpponent opponent : opponents)
                everyone.add(opponent.place());
            for (RaceOpponent opponent : opponents) {
                RaceOpponent.Other self = opponent.place();
                opponent.tick(track, distance, sinceOpen, START, everyone.stream().filter(other -> !other.equals(self)).toList());
            }
        }

        /** The player is in; the rest have a while to come in too. @return true when it's all over */
        private boolean waitForOthers() {
            boolean all = opponents.stream().allMatch(RaceOpponent::finished);
            if (!all && ticks - finishTick < STRAGGLER_TICKS) {
                if (ticks % 20 == 0)
                    player.displayClientMessage(Component.translatable("umapyoi.race.result.waiting"), true);
                return false;
            }
            // A little longer, so the last ones are seen crossing the line.
            if (all && ticks - finishTick < 40)
                return false;
            showResult();
            cleanUp();
            return true;
        }

        /** Where the player is in the field right now (1 = first). */
        private int rank() {
            int rank = 1;
            for (RaceOpponent opponent : opponents)
                if (time >= 0 ? opponent.finished() && opponent.time() < time
                        : opponent.finished() || opponent.runner().progress() > runner.progress())
                    rank++;
            return rank;
        }

        private record Place(Component name, double time, double progress, boolean player) {
        }

        private void showResult() {
            List<Place> places = new ArrayList<>();
            places.add(new Place(player.getDisplayName(), time, runner.progress(), true));
            for (RaceOpponent opponent : opponents)
                places.add(new Place(opponent.name(), opponent.time(), opponent.runner().progress(), false));
            places.sort((a, b) -> a.time() >= 0 && b.time() >= 0 ? Double.compare(a.time(), b.time())
                    : a.time() >= 0 ? -1 : b.time() >= 0 ? 1 : Double.compare(b.progress(), a.progress()));
            double best = places.getFirst().time();
            player.sendSystemMessage(Component.translatable("umapyoi.race.result.header", String.format("%.0f", setup.distanceMeters()))
                    .withStyle(ChatFormatting.GOLD));
            for (int i = 0; i < places.size(); i++) {
                Place place = places.get(i);
                Component when = place.time() < 0 ? Component.translatable("umapyoi.race.result.unplaced")
                        : Component.literal(i == 0 ? formatTime(place.time()) : formatTime(place.time()) + String.format(" (+%.2f)", place.time() - best));
                MutableComponent line = Component.literal((i + 1) + ". ").append(place.name()).append("  ").append(when);
                player.sendSystemMessage(place.player() ? line.withStyle(ChatFormatting.GREEN) : line);
            }
        }

        private boolean countdown() {
            // Stay in the stall: the back of it is open.
            if (Math.hypot(player.getX() - stallX, player.getZ() - stallZ) > 0.35)
                player.connection.teleport(stallX, origin.getY() + 1, stallZ, player.getYRot(), player.getXRot());
            int left = countdownTicks - ticks;
            // Working out everyone's pacing takes a moment: one of them per tick.
            for (RaceOpponent opponent : opponents)
                if (opponent.plan(setup, plainSprint, track) && left > 0)
                    break;
            if (left > 0) {
                if (left % 20 == 0) {
                    title(Component.literal(String.valueOf(left / 20)), Component.empty());
                    player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                return false;
            }
            for (BlockPos door : doors) {
                BlockState state = level.getBlockState(door);
                if (state.getBlock() instanceof GateDoor)
                    level.setBlock(door, state.setValue(GateDoor.OPEN, true), Block.UPDATE_CLIENTS);
            }
            BlockPos middle = doors.get(doors.size() / 2);
            if (level.getBlockState(middle).getBlock() instanceof GateDoor gateDoor)
                gateDoor.playSound(null, level, middle, true);
            title(Component.translatable("umapyoi.race.trial.go"), Component.empty());
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
            openTick = ticks;
            lastX = player.getX();
            lastZ = player.getZ();
            TrackGeometry.Spot spot = here();
            lastSpot = spot.progress();
            offset = spot.offset();
            // Standing just behind the line: a little below zero.
            runner.start(spot.progress() > track.lap() / 2 ? spot.progress() - track.lap() : spot.progress());
            PacketDistributor.sendToPlayer(player, runner.state(skill()));
            return false;
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
            if (openTick >= 0 && time < 0 && runner.jump())
                gasp();
        }

        /**
         * The action bar, with what helps while testing: phase and place on the course, running
         * style and its speed coefficient now, whether the runner is speeding up or slowing down,
         * and the speed being aimed at, then distance left and time.
         */
        private void showHud(RaceStatePayload state, int sinceOpen) {
            RacePhase phase = runner.phase();
            boolean sprinting = player.isSprinting();
            String motionKey = runner.exhausted() ? "umapyoi.race.motion.exhausted"
                    : runner.accelerating(state, sprinting) ? "umapyoi.race.motion.accelerating"
                    : runner.decelerating(state, sprinting) ? "umapyoi.race.motion.decelerating"
                    : sprinting ? "umapyoi.race.motion.full" : "umapyoi.race.motion.cruising";
            double target = (sprinting ? state.sprintSpeed() : state.cruiseSpeed()) * plainSprint;
            double remaining = Math.max(0, distance - runner.progress()) * metersPerBlock;
            MutableComponent place = opponents.isEmpty() ? Component.empty()
                    : Component.translatable("umapyoi.race.rank", rank(), opponents.size() + 1).append(" | ");
            player.displayClientMessage(place.append(Component.translatable(phase.key()))
                    .append(" · ").append(Component.translatable(runner.layout().sectionKey(runner.progress())))
                    .append(" | ").append(Component.translatable(RaceStyle.key(stats.style())))
                    .append(String.format(" ×%.3f", RaceStyle.speed(stats.style(), phase)))
                    .append(" | ").append(Component.translatable(motionKey))
                    .append(" → ").append(Component.translatable("umapyoi.race.speed", String.format("%.2f", target)))
                    .append(" | ").append(Component.translatable("umapyoi.race.trial.hud",
                            String.format("%.0f", remaining), formatTime(sinceOpen / 20.0))), true);
        }

        private TrackGeometry.Spot here() {
            return track.locate(player.getX() - origin.getX(), player.getZ() - origin.getZ());
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

        private void finish(double seconds) {
            String clock = formatTime(seconds);
            String meters = String.format("%.0f", setup.distanceMeters());
            title(opponents.isEmpty() ? Component.literal(clock) : Component.translatable("umapyoi.race.rank", rank(), opponents.size() + 1).append("  " + clock),
                    Component.translatable(opponents.isEmpty() ? "umapyoi.race.trial.finish_subtitle" : "umapyoi.race.result.header", meters));
            player.sendSystemMessage(Component.translatable("umapyoi.race.trial.result", meters, clock));
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            time = seconds;
            finishTick = ticks;
            if (opponents.isEmpty()) {
                cleanUp();
                return;
            }
            PacketDistributor.sendToPlayer(player, RaceStatePayload.INACTIVE);
        }

        void cancel() {
            if (!player.hasDisconnected())
                player.sendSystemMessage(Component.translatable("umapyoi.race.trial.cancelled"));
            cleanUp();
        }

        void cleanUp() {
            removeGates(false);
            opponents.forEach(RaceOpponent::disappear);
            if (!player.hasDisconnected())
                PacketDistributor.sendToPlayer(player, RaceStatePayload.INACTIVE);
            if (!player.hasDisconnected() && player.gameMode.getGameModeForPlayer() != previousMode)
                player.setGameMode(previousMode);
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

        private void title(Component title, Component subtitle) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }
}
