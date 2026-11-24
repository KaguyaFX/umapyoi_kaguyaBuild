package net.tracen.umapyoi.race.track;

/**
 * A racecourse shaped like a running track: two straights joined by two half circles. Everything
 * else (progress, placing, finishing, where to put a pole) is worked out from one reference line.
 * <p>
 * The reference line is where race distance is measured, as on real courses: one block outside
 * the inner edge of the running surface. A runner hugging the inner rail runs about the race's
 * distance; one running wide runs more. Offsets are measured from this line, positive outward;
 * the surface spans {@link #inner()} to {@link #outer()}.
 * <p>
 * Units are blocks. The origin is the middle of the infield and the straights run along X.
 * Runners go clockwise seen from above (north up): east along the north straight, west along the
 * south one. The south straight is the home straight. Progress 0 is the finish line, {@code finish}
 * blocks before the home straight runs into the curve.
 *
 * @param straight length of each straight
 * @param radius   radius of the reference line in the curves
 * @param width    width of the running surface
 * @param finish   how far before the end of the home straight the finish line is (0 to straight)
 * @param mirrored true for a left-handed course: the same course turned left to right, run
 *                 anticlockwise (the home straight is still in front of the stand)
 */
public record TrackGeometry(double straight, double radius, double width, double finish, boolean mirrored) {
    /** The reference line is this far outside the surface's inner edge. */
    public static final double MEASURE_FROM_INNER = 1;
    /** The curves never get tighter than this, whatever lap length is asked for. */
    private static final double MIN_RADIUS = 15;

    /**
     * The parts of the course in running order from the finish line: the first curve is turns 1 and
     * 2, then the back straight, the second curve is turns 3 and 4, then the home straight in front
     * of the stand. Each curve is split in half, as real courses number their turns.
     */
    public enum Section {
        TURN_1, TURN_2, BACK_STRAIGHT, TURN_3, TURN_4, HOME_STRAIGHT;

        public boolean straight() {
            return this == BACK_STRAIGHT || this == HOME_STRAIGHT;
        }

        public String key() {
            return "umapyoi.race.section." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Where a spot is: how far along the reference line (0 to lap) and how far off it (positive is outside). */
    public record Spot(double progress, double offset) {
    }

    /** A point on the course and the running direction there, as a Minecraft yaw. */
    public record Point(double x, double z, float yaw) {
    }

    /** The same course, right- or left-handed. */
    public TrackGeometry withMirrored(boolean left) {
        return mirrored == left ? this : new TrackGeometry(straight, radius, width, finish, left);
    }

    /** A right-handed course whose reference line is {@code lapMeters} round; the curves take what the straights leave. */
    public static TrackGeometry fromMeters(double lapMeters, double straightMeters, double widthBlocks,
            double finishMeters, double metersPerBlock) {
        double straight = straightMeters / metersPerBlock;
        double radius = Math.max((lapMeters / metersPerBlock - 2 * straight) / (2 * Math.PI), MIN_RADIUS);
        double finish = Math.min(Math.max(finishMeters / metersPerBlock, 0), straight);
        return new TrackGeometry(straight, radius, widthBlocks, finish, false);
    }

    public double lap() {
        return 2 * straight + 2 * Math.PI * radius;
    }

    private double curve() {
        return Math.PI * radius;
    }

    /** The surface's inner edge, as an offset from the reference line. */
    public double inner() {
        return -MEASURE_FROM_INNER;
    }

    /** The surface's outer edge, as an offset from the reference line. */
    public double outer() {
        return width - MEASURE_FROM_INNER;
    }

    /** The middle of the surface, as an offset from the reference line. */
    public double middle() {
        return (inner() + outer()) / 2;
    }

    public boolean onSurface(double offset) {
        return offset >= inner() && offset < outer();
    }

    /** How far outside the surface an offset is (0 on it). */
    public double beyondSurface(double offset) {
        return offset < inner() ? inner() - offset : offset >= outer() ? offset - outer() : 0;
    }

    /** The section a progress (0 to lap) falls in. */
    public Section section(double progress) {
        double p = wrap(progress - finish);
        double c = curve();
        if (p < c / 2)
            return Section.TURN_1;
        if (p < c)
            return Section.TURN_2;
        if (p < c + straight)
            return Section.BACK_STRAIGHT;
        if (p < c + straight + c / 2)
            return Section.TURN_3;
        if (p < 2 * c + straight)
            return Section.TURN_4;
        return Section.HOME_STRAIGHT;
    }

    /** How far into its section a progress is. */
    public double intoSection(double progress) {
        return wrap(progress - finish) - rawStart(section(progress));
    }

    /** Progress (0 to lap) where a section begins. */
    public double sectionStart(Section section) {
        return wrap(rawStart(section) + finish);
    }

    /** Length of a section along the reference line. */
    public double sectionLength(Section section) {
        return section.straight() ? straight : curve() / 2;
    }

    /** Where a section starts, counted from the end of the home straight (where "raw" progress is 0). */
    private double rawStart(Section section) {
        double c = curve();
        return switch (section) {
            case TURN_1 -> 0;
            case TURN_2 -> c / 2;
            case BACK_STRAIGHT -> c;
            case TURN_3 -> c + straight;
            case TURN_4 -> c + straight + c / 2;
            case HOME_STRAIGHT -> 2 * c + straight;
        };
    }

    /** Progress where the home straight begins (the end of the last curve). */
    public double homeStraightStart() {
        return wrap(lap() - straight + finish);
    }

    /** Half the extent along X and Z of everything up to {@code beyond} blocks outside the surface. */
    public double halfLengthX(double beyond) {
        return straight / 2 + radius + outer() + beyond;
    }

    public double halfLengthZ(double beyond) {
        return radius + outer() + beyond;
    }

    /** How much progress a block run at this offset makes: 1 on a straight, less on the outside of a curve. */
    public double along(double progress, double offset) {
        return section(wrap(progress)).straight() ? 1 : radius / Math.max(1, radius + offset);
    }

    public Spot locate(double x, double z) {
        if (mirrored)
            x = -x;
        double half = straight / 2;
        double raw;
        double offset;
        if (x < -half) {
            // West curve, right after the home straight. Angle runs from pi/2 (south) to 3pi/2 (north).
            double dx = x + half;
            double angle = Math.atan2(z, dx);
            if (angle < 0)
                angle += 2 * Math.PI;
            offset = Math.hypot(dx, z) - radius;
            raw = (angle - Math.PI / 2) * radius;
        } else if (x > half) {
            // East curve. Angle runs from -pi/2 (north) to pi/2 (south).
            double dx = x - half;
            offset = Math.hypot(dx, z) - radius;
            raw = curve() + straight + (Math.atan2(z, dx) + Math.PI / 2) * radius;
        } else if (z < 0) {
            offset = -z - radius;
            raw = curve() + (x + half);
        } else {
            offset = z - radius;
            raw = 2 * curve() + straight + (half - x);
        }
        // "raw" starts where the home straight meets the west curve; the finish is a little before that.
        return new Spot(wrap(raw + finish), offset);
    }

    public Point pointAt(double progress, double offset) {
        Point point = rightHandedPoint(progress, offset);
        return mirrored ? new Point(-point.x(), point.z(), -point.yaw()) : point;
    }

    private Point rightHandedPoint(double progress, double offset) {
        double p = wrap(progress - finish);
        double half = straight / 2;
        double r = radius + offset;
        double c = curve();
        if (p < c) {
            double angle = Math.PI / 2 + p / radius;
            return onCurve(-half, angle, r);
        }
        if (p < c + straight)
            return new Point(-half + (p - c), -r, yaw(1, 0));
        if (p < 2 * c + straight) {
            double angle = -Math.PI / 2 + (p - c - straight) / radius;
            return onCurve(half, angle, r);
        }
        return new Point(half - (p - 2 * c - straight), r, yaw(-1, 0));
    }

    private static Point onCurve(double centerX, double angle, double r) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // Clockwise on screen means the angle grows; the running direction is (-sin, cos).
        return new Point(centerX + r * cos, r * sin, yaw(-sin, cos));
    }

    private static float yaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public double wrap(double progress) {
        double lap = lap();
        double p = progress % lap;
        return p < 0 ? p + lap : p;
    }
}
