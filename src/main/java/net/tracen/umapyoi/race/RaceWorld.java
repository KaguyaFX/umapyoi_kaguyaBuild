package net.tracen.umapyoi.race;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.race.track.TrackBuilder;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * The race world: a separate dimension (flat, no mobs, always noon) where races are run, so race
 * rules never mix with survival. It holds one course, built the first time someone goes there.
 * Going in remembers where the player came from; leaving sends them back. What they carry stays at the
 * door ({@link RaceStash}).
 */
public final class RaceWorld {
    public static final ResourceKey<Level> RACECOURSE = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "racecourse"));
    /** The race world's only biome: plain, nothing spawns, and it can rain. */
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "racecourse"));
    private static final String RETURN_TAG = "umapyoi_race_return";
    private static final String MODE = "mode";
    /** Distance poles stand every this many race meters before the finish. */
    private static final double POLE_METERS = 200;

    public enum Entry {
        SENT, PREPARING, BUSY, ALREADY_THERE, MISSING
    }

    private RaceWorld() {
    }

    public static boolean isRaceWorld(Level level) {
        return level.dimension() == RACECOURSE;
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(RACECOURSE);
    }

    /** The course the config describes. */
    public static TrackGeometry configuredTrack() {
        return track(UmapyoiConfig.TRACK_LAP_METERS.get(), UmapyoiConfig.TRACK_STRAIGHT_METERS.get(),
                UmapyoiConfig.TRACK_WIDTH.get());
    }

    public static TrackGeometry track(double lapMeters, double straightMeters, int width) {
        return TrackGeometry.fromMeters(lapMeters, straightMeters, width, UmapyoiConfig.TRACK_FINISH_METERS.get(),
                UmapyoiConfig.RACE_METERS_PER_BLOCK.get());
    }

    /** Sends a player to the course, building it first if it doesn't exist yet. */
    public static Entry enter(ServerPlayer player) {
        ServerLevel race = level(player.server);
        if (race == null)
            return Entry.MISSING;
        RacecourseData data = RacecourseData.get(race);
        if (data.built()) {
            if (isRaceWorld(player.level()))
                return Entry.ALREADY_THERE;
            sendToCourse(player, race, data);
            return Entry.SENT;
        }
        if (TrackBuilder.busy())
            return Entry.BUSY;
        build(race, configuredTrack(),
                percent -> player.displayClientMessage(
                        Component.translatable("umapyoi.command.racetrack.progress", percent), true),
                result -> {
                    if (!player.hasDisconnected())
                        sendToCourse(player, race, data);
                });
        return Entry.PREPARING;
    }

    /**
     * Builds the course, replacing the one already there (whose area is cleared too). The caller
     * checks {@link TrackBuilder#busy()} first.
     */
    public static void build(ServerLevel race, TrackGeometry track, IntConsumer onProgress, Consumer<TrackBuilder.Result> onDone) {
        RacecourseData data = RacecourseData.get(race);
        TrackGeometry previous = data.baseTrack();
        BlockPos origin = previous != null ? data.origin() : groundAtCentre(race);
        RaceWorldEvents.used();
        data.startBuilding(origin);
        TrackBuilder.start(race, origin, track, previous, poleSpacing(), onProgress, result -> {
            data.markBuilt(track);
            onDone.accept(result);
        });
    }

    /**
     * Repaints the course for a race, if it isn't already laid out that way. The caller checks
     * {@link TrackBuilder#busy()} first and that there is a course.
     *
     * @return false if nothing needed doing (onDone isn't called then)
     */
    public static boolean dress(ServerLevel race, RaceSetup setup, IntConsumer onProgress, Runnable onDone) {
        RacecourseData data = RacecourseData.get(race);
        TrackGeometry track = data.baseTrack();
        if (track == null || !data.needsDressing(setup.surface(), setup.leftHanded()))
            return false;
        RaceWorldEvents.used();
        data.startDressing();
        TrackBuilder.dress(race, data.origin(), track.withMirrored(setup.leftHanded()), setup.surface(), poleSpacing(),
                onProgress, () -> {
                    data.markDressed(setup.surface(), setup.leftHanded());
                    onDone.run();
                });
        return true;
    }

    private static double poleSpacing() {
        return POLE_METERS / UmapyoiConfig.RACE_METERS_PER_BLOCK.get();
    }

    /** Puts a player at the start of the home straight, looking down it. */
    public static void sendToCourse(ServerPlayer player, ServerLevel race, RacecourseData data) {
        TrackGeometry track = data.track();
        if (track == null)
            return;
        if (!isRaceWorld(player.level()))
            saveReturnPoint(player);
        TrackGeometry.Point view = track.pointAt(track.homeStraightStart() + 2, track.middle());
        BlockPos origin = data.origin();
        player.teleportTo(race, origin.getX() + view.x(), origin.getY() + 1, origin.getZ() + view.z(), view.yaw(), 0);
    }

    /** Sends a player somewhere in the race world, in adventure mode until they leave. */
    static void visit(ServerPlayer player, ServerLevel race, Vec3 at, float yaw) {
        if (!isRaceWorld(player.level()))
            saveReturnPoint(player);
        CompoundTag back = player.getPersistentData().getCompound(RETURN_TAG);
        if (!back.contains(MODE)) {
            back.putInt(MODE, player.gameMode.getGameModeForPlayer().getId());
            player.getPersistentData().put(RETURN_TAG, back);
        }
        player.stopRiding();
        player.teleportTo(race, at.x, at.y, at.z, yaw, 0);
        if (isRaceWorld(player.level()) && player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE)
            player.setGameMode(GameType.ADVENTURE);
    }

    /** Sends a player back to where they came from (or to the world spawn). False if they aren't in the race world. */
    public static boolean leave(ServerPlayer player) {
        if (!isRaceWorld(player.level()))
            return false;
        CompoundTag back = player.getPersistentData().getCompound(RETURN_TAG);
        player.getPersistentData().remove(RETURN_TAG);
        if (back.contains(MODE))
            player.setGameMode(GameType.byId(back.getInt(MODE)));
        ServerLevel target = null;
        ResourceLocation dimension = back.contains("dimension") ? ResourceLocation.tryParse(back.getString("dimension")) : null;
        if (dimension != null)
            target = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (target != null && !isRaceWorld(target)) {
            player.teleportTo(target, back.getDouble("x"), back.getDouble("y"), back.getDouble("z"),
                    back.getFloat("yaw"), back.getFloat("pitch"));
            return true;
        }
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        overworld.getChunk(spawn);
        BlockPos top = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
        player.teleportTo(overworld, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, player.getYRot(), 0);
        return true;
    }

    private static void saveReturnPoint(ServerPlayer player) {
        CompoundTag back = new CompoundTag();
        back.putString("dimension", player.level().dimension().location().toString());
        back.putDouble("x", player.getX());
        back.putDouble("y", player.getY());
        back.putDouble("z", player.getZ());
        back.putFloat("yaw", player.getYRot());
        back.putFloat("pitch", player.getXRot());
        player.getPersistentData().put(RETURN_TAG, back);
    }

    private static BlockPos groundAtCentre(ServerLevel race) {
        race.getChunk(0, 0);
        return new BlockPos(0, race.getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0) - 1, 0);
    }
}
