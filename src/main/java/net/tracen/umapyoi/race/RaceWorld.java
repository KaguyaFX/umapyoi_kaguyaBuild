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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.race.track.TrackBuilder;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * The race world: a separate dimension (flat, no mobs, always noon) where races are run, so race
 * rules never mix with survival. It holds one course, built the first time someone goes there.
 * Going in remembers where the player came from; leaving sends them back.
 */
public final class RaceWorld {
    public static final ResourceKey<Level> RACECOURSE = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "racecourse"));
    private static final String RETURN_TAG = "umapyoi_race_return";
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
        TrackGeometry previous = data.track();
        BlockPos origin = previous != null ? data.origin() : groundAtCentre(race);
        data.startBuilding(origin);
        double poleSpacing = POLE_METERS / UmapyoiConfig.RACE_METERS_PER_BLOCK.get();
        TrackBuilder.start(race, origin, track, previous, poleSpacing, onProgress, result -> {
            data.markBuilt(track);
            onDone.accept(result);
        });
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

    /** Sends a player back to where they came from (or to the world spawn). False if they aren't in the race world. */
    public static boolean leave(ServerPlayer player) {
        if (!isRaceWorld(player.level()))
            return false;
        CompoundTag back = player.getPersistentData().getCompound(RETURN_TAG);
        player.getPersistentData().remove(RETURN_TAG);
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
