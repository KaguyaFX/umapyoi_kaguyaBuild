package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.utils.Surface;

/**
 * What the race world remembers about its course: where it is, its shape, whether it was finished,
 * and how it was last dressed for a race (surface and handedness). The shape stays known while a
 * rebuild runs, so the old area can still be cleared if the server stops halfway; a repaint that
 * stops halfway is done again before the next race.
 */
public class RacecourseData extends SavedData {
    private static final String NAME = "umapyoi_racecourse";
    /** Bumped when the course's shape is read differently or what is built on it changes; an older course counts as unbuilt and gets rebuilt. */
    private static final int VERSION = 5;

    private boolean built;
    private BlockPos origin = BlockPos.ZERO;
    @Nullable
    private TrackGeometry track;
    private Surface surface = Surface.TURF;
    private boolean leftHanded;
    /** A repaint was started and never finished: the course may be a mix. */
    private boolean halfDressed;

    public static RacecourseData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RacecourseData::new, RacecourseData::load, null), NAME);
    }

    private static RacecourseData load(CompoundTag tag, HolderLookup.Provider registries) {
        RacecourseData data = new RacecourseData();
        data.built = tag.getBoolean("built") && tag.getInt("version") >= VERSION;
        data.origin = NbtUtils.readBlockPos(tag, "origin").orElse(BlockPos.ZERO);
        try {
            data.surface = Surface.valueOf(tag.getString("surface"));
        } catch (IllegalArgumentException ignored) {
            data.surface = Surface.TURF;
        }
        data.leftHanded = tag.getBoolean("left_handed");
        data.halfDressed = tag.getBoolean("half_dressed");
        if (tag.contains("track")) {
            CompoundTag t = tag.getCompound("track");
            data.track = new TrackGeometry(t.getDouble("straight"), t.getDouble("radius"), t.getDouble("width"), t.getDouble("finish"), false);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", VERSION);
        tag.putString("surface", surface.name());
        tag.putBoolean("left_handed", leftHanded);
        tag.putBoolean("half_dressed", halfDressed);
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

    /** The last course built or being replaced, right-handed, or null if there never was one. */
    @Nullable
    public TrackGeometry baseTrack() {
        return track;
    }

    /** The course as it is dressed now (left-handed if a race last ran that way), or null. */
    @Nullable
    public TrackGeometry track() {
        return track == null ? null : track.withMirrored(leftHanded);
    }

    /** Whether the course has to be repainted for a race on this surface and handedness. */
    public boolean needsDressing(Surface surface, boolean left) {
        return halfDressed || this.surface != surface || this.leftHanded != left;
    }

    void startBuilding(BlockPos origin) {
        this.built = false;
        this.origin = origin;
        setDirty();
    }

    /** A freshly built course is right-handed turf. */
    void markBuilt(TrackGeometry track) {
        this.built = true;
        this.track = track.withMirrored(false);
        this.surface = Surface.TURF;
        this.leftHanded = false;
        this.halfDressed = false;
        setDirty();
    }

    void startDressing() {
        this.halfDressed = true;
        setDirty();
    }

    void markDressed(Surface surface, boolean leftHanded) {
        this.surface = surface;
        this.leftHanded = leftHanded;
        this.halfDressed = false;
        setDirty();
    }
}
