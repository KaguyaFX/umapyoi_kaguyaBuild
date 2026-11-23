package net.tracen.umapyoi.race.track;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/**
 * Where everything around the running surface goes, in the same local block coordinates as
 * {@link TrackGeometry} (origin = the infield's middle). Offsets are from the course's reference
 * line. The grandstand faces the home straight from the south; the pond sits in the middle of
 * the infield.
 */
record CourseLayout(TrackGeometry track, BlockPos origin) {
    /** The stand's front wall is this far outside the surface, beyond the rail, hedge and apron. */
    private static final double STAND_FROM_EDGE = 9;
    /** The stand stops this many blocks short of each end of the straight. */
    private static final int STAND_END_GAP = 2;

    /** The stand's front wall, as an offset from the reference line. */
    double standStart() {
        return track.outer() + STAND_FROM_EDGE;
    }

    int standMinX() {
        return Mth.ceil(-track.straight() / 2) + STAND_END_GAP;
    }

    /** Exclusive. */
    int standMaxX() {
        return Mth.floor(track.straight() / 2) - STAND_END_GAP;
    }

    int standLength() {
        return Math.max(0, standMaxX() - standMinX());
    }

    /** South of the home straight, within the stand's length: the apron and the stand itself. */
    boolean besideStand(int i, double z) {
        return z > 0 && i >= standMinX() && i < standMaxX();
    }

    /** Below 1 inside the pond (an ellipse in the middle of the infield); infinite when there's no room for one. */
    double pondNorm(double x, double z) {
        double a = track.straight() / 2 * 0.55;
        double b = (track.radius() + track.inner()) * 0.42;
        if (a < 4 || b < 3)
            return Double.POSITIVE_INFINITY;
        return (x / a) * (x / a) + (z / b) * (z / b);
    }

    BlockPos world(int i, int h, int k) {
        return new BlockPos(origin.getX() + i, origin.getY() + h, origin.getZ() + k);
    }
}
