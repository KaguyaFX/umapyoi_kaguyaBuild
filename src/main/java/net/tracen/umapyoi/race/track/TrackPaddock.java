package net.tracen.umapyoi.race.track;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/**
 * The paddock behind the grandstand, where the runners walk round before the race and people look
 * at them: a walking ring (a small course of its own, so runners walk it the way they run the big one)
 * round a lawn, a rail with a gap towards the stand, a flat apron and three steps of standing room all
 * round, and a way from the stand's middle tunnel, which is open at the back.
 * <p>
 * Places are in the course's own coordinates (origin = the infield's middle), like everything else.
 */
public final class TrackPaddock {
    /** The walking ring, as a course: its straights, the radius of its reference line, its width. */
    private static final TrackGeometry RING = new TrackGeometry(30, 8, 3, 0, false);
    /** Offsets from the ring's reference line: the rail, the apron, and the three steps up to the outer edge. */
    private static final double RAIL = 2, APRON = 3, STEPS = 5, EDGE = 8;
    /** Blocks between the stand's back and the paddock's outer edge. */
    private static final double WAY = 6;
    /** Half the width of the way and of the gap in the rail. */
    private static final double HALF_GAP = 1.5;

    public enum Part {
        NONE, LAWN, PATH, RAIL, APRON, STEP_1, STEP_2, STEP_3, WAY
    }

    private TrackPaddock() {
    }

    /** The walking ring, centred on {@link #centre}. */
    public static TrackGeometry ring() {
        return RING;
    }

    /** The ring's middle, in the course's coordinates (x, z). */
    public static TrackGeometry.Point centre(TrackGeometry track) {
        CourseLayout layout = new CourseLayout(track, BlockPos.ZERO);
        double x = layout.standMinX() + layout.standLength() / 2 + 0.5;
        double z = standBack(track) + WAY + RING.radius() + EDGE;
        return new TrackGeometry.Point(x, z, 0);
    }

    /** The far side of the stand. */
    private static double standBack(TrackGeometry track) {
        CourseLayout layout = new CourseLayout(track, BlockPos.ZERO);
        return track.radius() + layout.standStart() + Grandstand.DEPTH;
    }

    /** How far south the paddock reaches. */
    static double farEdge(TrackGeometry track) {
        return centre(track).z() + RING.radius() + EDGE;
    }

    /** What's at a column. */
    public static Part part(TrackGeometry track, double x, double z) {
        TrackGeometry.Point c = centre(track);
        double dx = x - c.x(), dz = z - c.z();
        double o = RING.locate(dx, dz).offset();
        boolean gap = Math.abs(dx) <= HALF_GAP && dz < 0;
        if (o < RING.inner())
            return Part.LAWN;
        if (o < RAIL)
            return Part.PATH;
        if (o < APRON)
            return gap ? Part.APRON : Part.RAIL;
        if (o < STEPS)
            return Part.APRON;
        if (o < EDGE)
            return gap ? Part.WAY : o < STEPS + 1 ? Part.STEP_1 : o < STEPS + 2 ? Part.STEP_2 : Part.STEP_3;
        if (gap && z >= standBack(track) && dz < 0)
            return Part.WAY;
        return Part.NONE;
    }

    /** Whether a place is the walking ring or its lawn: for runners only. */
    public static boolean inRing(TrackGeometry track, double x, double z) {
        Part part = part(track, x, z);
        return part == Part.LAWN || part == Part.PATH;
    }

    /** A place on the walking ring, {@code share} of the way round, in the course's coordinates. */
    public static TrackGeometry.Point onRing(TrackGeometry track, double share) {
        TrackGeometry.Point c = centre(track);
        TrackGeometry.Point at = RING.pointAt(RING.lap() * Mth.frac(share), RING.middle());
        return new TrackGeometry.Point(c.x() + at.x(), c.z() + at.z(), at.yaw());
    }
}
