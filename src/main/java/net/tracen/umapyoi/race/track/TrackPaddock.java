package net.tracen.umapyoi.race.track;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * The paddock behind the grandstand, where the runners spend the time before the race and people look
 * at them: a walking ring (a small course of its own, so runners walk it the way they run the big one)
 * round a lawn with a cherry tree and benches, a rail with a gap towards the stand, a flat apron and
 * three steps of standing room all round, and a way from the stand's middle tunnel, which is open at
 * the back.
 * <p>
 * Places are in the course's own coordinates (origin = the infield's middle), like everything else;
 * the spots where runners sit or stand are relative to the ring's middle.
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

    /** Where a runner sits or stands, from the ring's middle, and which way she faces (yaw). */
    public record Spot(double x, double z, float yaw, boolean seat) {
    }

    /** Benches, back to back in pairs, each this long: where their west ends are, from the ring's middle. */
    private static final int BENCH = 4;
    private static final int[] BENCH_WEST = { -12, 8 };
    private static final List<Spot> SEATS = new ArrayList<>();
    /** People standing together: two or three facing each other, or one by the rail looking out. */
    private static final List<List<Spot>> GROUPS = new ArrayList<>();

    static {
        for (int west : BENCH_WEST)
            for (int j = 0; j < BENCH; j++) {
                SEATS.add(new Spot(west + j + 0.5, -0.5, 180, true));
                SEATS.add(new Spot(west + j + 0.5, 0.5, 0, true));
            }
        group(-18.5, -2.0, -17.0, -0.6);
        group(16.6, 1.0, 18.2, 2.4, 18.6, 0.2);
        group(5.0, -4.8, 6.4, -5.6);
        group(-6.2, 4.4, -4.8, 5.4);
        GROUPS.add(List.of(new Spot(-10.0, -5.8, 180, false)));
        GROUPS.add(List.of(new Spot(11.0, 5.8, 0, false)));
        GROUPS.add(List.of(new Spot(0.5, 5.6, 0, false)));
    }

    /** A group facing its middle. */
    private static void group(double... xz) {
        double mx = 0, mz = 0;
        int n = xz.length / 2;
        for (int i = 0; i < n; i++) {
            mx += xz[2 * i] / n;
            mz += xz[2 * i + 1] / n;
        }
        List<Spot> spots = new ArrayList<>();
        for (int i = 0; i < n; i++)
            spots.add(new Spot(xz[2 * i], xz[2 * i + 1], yawTowards(mx - xz[2 * i], mz - xz[2 * i + 1]), false));
        GROUPS.add(List.copyOf(spots));
    }

    private static float yawTowards(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private TrackPaddock() {
    }

    /** The walking ring, centred on {@link #centre}. */
    public static TrackGeometry ring() {
        return RING;
    }

    public static List<Spot> seats() {
        return SEATS;
    }

    public static List<List<Spot>> groups() {
        return GROUPS;
    }

    /** The ring's middle, in the course's coordinates (x, z), on a block corner. */
    public static TrackGeometry.Point centre(TrackGeometry track) {
        CourseLayout layout = new CourseLayout(track, BlockPos.ZERO);
        double x = layout.standMinX() + layout.standLength() / 2;
        double z = Math.floor(standBack(track) + WAY + RING.radius() + EDGE);
        return new TrackGeometry.Point(x, z, 0);
    }

    /** The ring's middle as a block position, the origin runners in the paddock are placed from. */
    public static BlockPos ringOrigin(TrackGeometry track, BlockPos origin) {
        TrackGeometry.Point c = centre(track);
        return origin.offset((int) c.x(), 0, (int) c.z());
    }

    /** The middle of the way from the stand's middle tunnel. */
    private static double wayX(TrackGeometry track) {
        CourseLayout layout = new CourseLayout(track, BlockPos.ZERO);
        return layout.standMinX() + layout.standLength() / 2 + 0.5;
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
        boolean gap = Math.abs(x - wayX(track)) <= HALF_GAP && dz < 0;
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
        if (gap && z >= standBack(track))
            return Part.WAY;
        return Part.NONE;
    }

    /** Whether a place is the walking ring or its lawn: for runners only. */
    public static boolean inRing(TrackGeometry track, double x, double z) {
        Part part = part(track, x, z);
        return part == Part.LAWN || part == Part.PATH;
    }

    /** The way a bench's seat at this column faces (its stairs' facing), or null if there's none. */
    @Nullable
    static Direction bench(TrackGeometry track, double x, double z) {
        TrackGeometry.Point c = centre(track);
        double dx = x - c.x(), dz = z - c.z();
        for (Spot seat : SEATS)
            if (Math.abs(seat.x() - dx) < 0.5 && Math.abs(seat.z() - dz) < 0.5)
                // A seat faced north has its back, the stairs' high side, to the south.
                return seat.yaw() == 180 ? Direction.SOUTH : Direction.NORTH;
        return null;
    }

    /** Whether flowers may grow here: not on the benches, the tree, or where people stand. */
    static boolean clear(TrackGeometry track, double x, double z) {
        TrackGeometry.Point c = centre(track);
        double dx = x - c.x(), dz = z - c.z();
        if (Math.abs(dx) < 2 && Math.abs(dz) < 2)
            return false;
        for (Spot seat : SEATS)
            if (Math.abs(seat.x() - dx) < 1.5 && Math.abs(seat.z() - dz) < 2)
                return false;
        for (List<Spot> group : GROUPS)
            for (Spot spot : group)
                if (Math.hypot(spot.x() - dx, spot.z() - dz) < 1.5)
                    return false;
        return true;
    }

    /** Where the cherry tree in the middle of the lawn grows, in the course's coordinates. */
    static TrackGeometry.Point tree(TrackGeometry track) {
        return centre(track);
    }

    /** A place on the walking ring, {@code share} of the way round, in the course's coordinates. */
    public static TrackGeometry.Point onRing(TrackGeometry track, double share) {
        TrackGeometry.Point c = centre(track);
        TrackGeometry.Point at = RING.pointAt(RING.lap() * Mth.frac(share), RING.middle());
        return new TrackGeometry.Point(c.x() + at.x(), c.z() + at.z(), at.yaw());
    }
}
