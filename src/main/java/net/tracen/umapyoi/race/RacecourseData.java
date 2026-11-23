package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * What the race world remembers about its course: where it is, its shape, and whether it was
 * finished. The shape stays known while a rebuild runs, so the old area can still be cleared if
 * the server stops halfway.
 */
public class RacecourseData extends SavedData {
    private static final String NAME = "umapyoi_racecourse";

    private boolean built;
    private BlockPos origin = BlockPos.ZERO;
    @Nullable
    private TrackGeometry track;

    public static RacecourseData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RacecourseData::new, RacecourseData::load, null), NAME);
    }

    private static RacecourseData load(CompoundTag tag, HolderLookup.Provider registries) {
        RacecourseData data = new RacecourseData();
        data.built = tag.getBoolean("built");
        data.origin = NbtUtils.readBlockPos(tag, "origin").orElse(BlockPos.ZERO);
        if (tag.contains("track")) {
            CompoundTag t = tag.getCompound("track");
            data.track = new TrackGeometry(t.getDouble("straight"), t.getDouble("radius"), t.getDouble("width"), t.getDouble("finish"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("built", built);
        tag.put("origin", NbtUtils.writeBlockPos(origin));
        if (track != null) {
            CompoundTag t = new CompoundTag();
            t.putDouble("straight", track.straight());
            t.putDouble("radius", track.radius());
            t.putDouble("width", track.width());
            t.putDouble("finish", track.finish());
            tag.put("track", t);
        }
        return tag;
    }

    public boolean built() {
        return built && track != null;
    }

    /** Where the infield's middle is, at ground height; only meaningful once a course was started. */
    public BlockPos origin() {
        return origin;
    }

    /** The last course built or being replaced, or null if there never was one. */
    @Nullable
    public TrackGeometry track() {
        return track;
    }

    void startBuilding(BlockPos origin) {
        this.built = false;
        this.origin = origin;
        setDirty();
    }

    void markBuilt(TrackGeometry track) {
        this.built = true;
        this.track = track;
        setDirty();
    }
}
