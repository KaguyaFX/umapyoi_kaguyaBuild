package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
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
import net.tracen.umapyoi.utils.Distance;
import net.tracen.umapyoi.utils.Surface;
import net.tracen.umapyoi.utils.ThreeBlockPart;

/**
 * A solo time trial on the race world's course. The fixed start is the finish line in front of
 * the stand, so a race as long as the lap starts and ends there. The runner waits in a starting
 * stall while a countdown runs, the gates open, and they are taken off the course a few seconds
 * later.
 * <p>
 * Distance is counted, not inferred from where the runner stands: every tick adds how far they
 * moved along the course, and a jump of more than {@link #MAX_STEP} blocks (a teleport, a cut
 * through the infield) counts for nothing. Running backwards takes distance off. So skipping
 * part of the course never helps, which is what checkpoints would be for.
 * <p>
 * Stamina: sprinting costs one point per block, each jump costs a few more, walking (cruising)
 * costs nothing, and standing still for a moment slowly brings some back. Below the low line the
 * Umamusume speed bonus drops ({@link RaceStats#bonusLeft}); the plain player's speed never does.
 * The client gets the result every tick and moves at that speed ({@link RaceSpeed}).
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class TimeTrial {
    /** Blocks per tick: anything faster isn't running. */
    private static final double MAX_STEP = 4;
    private static final int STALLS = 18;
    /** Gives up after this long without finishing. */
    private static final int TIME_LIMIT_TICKS = 20 * 60 * 10;

    public enum Start {
        STARTED, BUSY, NOT_THERE, NO_COURSE
    }

    @Nullable
    private static Run current;

    private TimeTrial() {
    }

    /** @param surface what the race is run on; the course is turf for now, so it only matters for the aptitude */
    public static Start start(ServerPlayer player, double distanceMeters, Surface surface) {
        if (current != null || TrackBuilder.busy())
            return Start.BUSY;
        if (!RaceWorld.isRaceWorld(player.level()))
            return Start.NOT_THERE;
        ServerLevel level = (ServerLevel) player.level();
        RacecourseData data = RacecourseData.get(level);
        TrackGeometry track = data.track();
        if (!data.built() || track == null)
            return Start.NO_COURSE;
        current = new Run(player, level, track, data.origin(), distanceMeters, surface);
        return Start.STARTED;
    }

    public static boolean running() {
        return current != null;
    }

    /** @return false if this player has no trial running */
    public static boolean cancel(ServerPlayer player) {
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
        private final ServerPlayer player;
        private final ServerLevel level;
        private final TrackGeometry track;
        private final BlockPos origin;
        private final double distance;
        private final double metersPerBlock;
        private final double distanceMeters;
        private final GameType previousMode;
        private final List<BlockPos> gateBlocks = new ArrayList<>();
        private final List<BlockPos> doors = new ArrayList<>();
        private final double stallX;
        private final double stallZ;
        private final float yaw;
        private final int countdownTicks;
        private final RaceStats stats;
        /** Names the parts of this race (phases, corners, final straight). */
        private final RaceLayout layout;
        /** The plain player's sprint, in blocks per second: converts accelerations to RaceMotion's unit. */
        private final double plainSprint;
        /** The server's copy of the runner's speed; null until the gates open. */
        @Nullable
        private RaceMotion motion;
        private double stamina;
        private double lastX;
        private double lastZ;
        private int stillTicks;
        private int ticks;
        private int openTick = -1;
        private double progress;
        private double lastSpot;

        Run(ServerPlayer player, ServerLevel level, TrackGeometry track, BlockPos origin, double distanceMeters, Surface surface) {
            this.player = player;
            this.level = level;
            this.track = track;
            this.origin = origin;
            this.metersPerBlock = UmapyoiConfig.RACE_METERS_PER_BLOCK.get();
            this.distanceMeters = distanceMeters;
            this.distance = distanceMeters / metersPerBlock;
            this.previousMode = player.gameMode.getGameModeForPlayer();
            this.countdownTicks = UmapyoiConfig.RACE_COUNTDOWN_SECONDS.get() * 20;
            ItemStack soul = UmapyoiAPI.getUmaSoul(player);
            this.stats = RaceStats.of(soul, RaceStyle.of(soul, level), surface, Distance.match((int) Math.round(distanceMeters)));
            var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            this.plainSprint = (attribute == null ? 0.1 : attribute.getBaseValue()) * 1.3 * RaceSpeed.BLOCKS_PER_SECOND;
            this.layout = new RaceLayout(track, distance);
            this.stamina = stats.stamina();

            // A row of stalls standing on the finish line, doors just past it, innermost stall first.
            double lap = track.lap();
            int stalls = Math.min(STALLS, (int) track.width());
            TrackGeometry.Point first = null;
            for (int lane = 0; lane < stalls; lane++) {
                double offset = track.inner() + 0.5 + lane;
                TrackGeometry.Point stall = track.pointAt(lap - 0.5, offset);
                TrackGeometry.Point door = track.pointAt(0.5, offset);
                if (first == null)
                    first = stall;
                placeStall(cell(stall), cell(door), Direction.fromYRot(stall.yaw()));
            }
            BlockPos firstCell = cell(first);
            this.stallX = firstCell.getX() + 0.5;
            this.stallZ = firstCell.getZ() + 0.5;
            this.yaw = first.yaw();

            if (previousMode != GameType.ADVENTURE)
                player.setGameMode(GameType.ADVENTURE);
            player.teleportTo(level, stallX, origin.getY() + 1, stallZ, yaw, 0);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
            sendStats(surface);
        }

        /** What the race makes of this soul: shown once at the start, for testing. */
        private void sendStats(Surface surface) {
            player.sendSystemMessage(Component.translatable("umapyoi.race.trial.stats",
                    String.format("%.1f", stats.bonusRatio() * 100),
                    String.format("%.2f", stats.accel()),
                    String.format("%.0f", stats.stamina()),
                    String.format("%.0f", stats.emptyFloor() * 100),
                    String.format("%.2f", stats.mood()),
                    Component.translatable("umapyoi.race.surface." + surface.name().toLowerCase(java.util.Locale.ROOT)),
                    String.format("%.2f", stats.surfaceFactor()),
                    String.format("%.2f", stats.distanceFactor())));
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

            double before = progress;
            TrackGeometry.Spot spot = here();
            double step = spot.progress() - lastSpot;
            double lap = track.lap();
            if (step > lap / 2)
                step -= lap;
            else if (step < -lap / 2)
                step += lap;
            lastSpot = spot.progress();
            if (Math.abs(step) <= MAX_STEP)
                progress += step;
            RaceStatePayload state = state();
            motion.step(state, player.isSprinting(), -1, 0);
            useStamina(state);

            if (progress >= distance && step > 0) {
                // Crossed during this tick: work out when, between the last tick and this one.
                double fraction = (distance - before) / step;
                finish((sinceOpen - 1 + fraction) / 20.0);
                return true;
            }
            showHud(state, sinceOpen);
            return false;
        }

        private boolean countdown() {
            // Stay in the stall: the back of it is open.
            if (Math.hypot(player.getX() - stallX, player.getZ() - stallZ) > 0.35)
                player.connection.teleport(stallX, origin.getY() + 1, stallZ, player.getYRot(), player.getXRot());
            int left = countdownTicks - ticks;
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
            motion = new RaceMotion(UmapyoiConfig.RACE_START_SPEED.get());
            sendState(state());
            TrackGeometry.Spot spot = here();
            lastSpot = spot.progress();
            // Standing just behind the line: a little below zero.
            progress = spot.progress() > track.lap() / 2 ? spot.progress() - track.lap() : spot.progress();
            return false;
        }

        /**
         * Speed above cruising costs stamina: one point per block at full speed, less while still
         * accelerating, and a little while slowing back down. Running wide costs more (more blocks).
         * Resting brings a little back.
         */
        private void useStamina(RaceStatePayload state) {
            double moved = Math.hypot(player.getX() - lastX, player.getZ() - lastZ);
            lastX = player.getX();
            lastZ = player.getZ();
            double span = state.sprintSpeed() - state.cruiseSpeed();
            double effort = span <= 0 ? 0 : Math.max(0, Math.min(1, (motion.speed() - state.cruiseSpeed()) / span));
            if (moved <= MAX_STEP)
                stamina -= moved * effort;
            stillTicks = moved < 0.01 ? stillTicks + 1 : 0;
            if (stillTicks >= UmapyoiConfig.RACE_REST_SECONDS.get() * 20)
                stamina += stats.stamina() * UmapyoiConfig.RACE_STAMINA_REGEN_PERCENT.get() / 100 / 20;
            stamina = Math.max(0, Math.min(stats.stamina(), stamina));
            sendState(state);
        }

        void jump() {
            if (openTick < 0)
                return;
            stamina = Math.max(0, stamina - UmapyoiConfig.RACE_JUMP_STAMINA.get());
        }

        /**
         * Full speed = the running style's coefficient for this phase x (1 + the Umamusume bonus left
         * after stamina, times the skill boost); cruise is a fraction of it.
         */
        RaceStatePayload state() {
            RacePhase phase = layout.phase(progress);
            double left = RaceStats.bonusLeft(stamina / stats.stamina(), stats.emptyFloor());
            // Speed skills give the Speed effect; in a race it multiplies only the Umamusume bonus.
            double skill = 1;
            MobEffectInstance speedEffect = player.getEffect(MobEffects.MOVEMENT_SPEED);
            if (speedEffect != null)
                skill += UmapyoiConfig.RACE_SKILL_SPEED_PER_LEVEL.get() * (speedEffect.getAmplifier() + 1);
            double sprint = RaceStyle.speed(stats.style(), phase) * (1 + stats.bonusRatio() * left * skill);
            double cruise = sprint * UmapyoiConfig.RACE_CRUISE_RATIO.get();
            double perTick = 1 / (plainSprint * 20);
            return new RaceStatePayload(true, (float) stamina, (float) stats.stamina(), (float) sprint, (float) cruise,
                    (float) (stats.accel() * RaceStyle.accel(stats.style(), phase) * perTick),
                    (float) (UmapyoiConfig.RACE_DECEL.get() * perTick),
                    (float) (UmapyoiConfig.RACE_START_DASH.get() * perTick),
                    UmapyoiConfig.RACE_SPRINT_KICK.get().floatValue(),
                    UmapyoiConfig.RACE_START_SPEED.get().floatValue());
        }

        private void sendState(RaceStatePayload state) {
            PacketDistributor.sendToPlayer(player, state);
        }

        /**
         * The action bar, with what helps while testing: phase and place on the course, running
         * style and its speed coefficient now, whether the runner is speeding up or slowing down,
         * and the speed being aimed at, then distance left and time.
         */
        private void showHud(RaceStatePayload state, int sinceOpen) {
            RacePhase phase = layout.phase(progress);
            boolean sprinting = player.isSprinting();
            String motionKey = motion.accelerating(state, sprinting) ? "umapyoi.race.motion.accelerating"
                    : motion.decelerating(state, sprinting) ? "umapyoi.race.motion.decelerating"
                    : sprinting ? "umapyoi.race.motion.full" : "umapyoi.race.motion.cruising";
            double target = (sprinting ? state.sprintSpeed() : state.cruiseSpeed()) * plainSprint;
            double remaining = Math.max(0, distance - progress) * metersPerBlock;
            player.displayClientMessage(Component.translatable(phase.key())
                    .append(" · ").append(Component.translatable(layout.sectionKey(progress)))
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
            String time = formatTime(seconds);
            String meters = String.format("%.0f", distanceMeters);
            title(Component.literal(time), Component.translatable("umapyoi.race.trial.finish_subtitle", meters));
            player.sendSystemMessage(Component.translatable("umapyoi.race.trial.result", meters, time));
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            cleanUp();
        }

        void cancel() {
            if (!player.hasDisconnected())
                player.sendSystemMessage(Component.translatable("umapyoi.race.trial.cancelled"));
            cleanUp();
        }

        void cleanUp() {
            removeGates(false);
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
