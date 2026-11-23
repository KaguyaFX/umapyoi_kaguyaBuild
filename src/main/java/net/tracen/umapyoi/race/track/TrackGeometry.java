package net.tracen.umapyoi.race.track;

/**
 * A racecourse shaped like a running track: two straights joined by two half circles. Everything
 * else (progress, placing, checkpoints, where to put a pole) is worked out from this centre line.
 * <p>
 * Units are blocks. The origin is the middle of the infield and the straights run along X.
 * Runners go clockwise seen from above (north up): east along the north straight, west along the
 * south one. The south straight is the home straight. Progress 0 is the finish line, {@code finish}
 * blocks before the home straight runs into the curve.
 *
 * @param straight length of each straight
 * @param radius   radius of the centre line in the curves
 * @param width    width of the running surface
 * @param finish   how far before the end of the home straight the finish line is (0 to straight)
 */
public record TrackGeometry(double straight, double radius, double width, double finish) {

    /** Where a spot is: how far along the centre line (0 to lap) and how far off it (positive is outside). */
    public record Spot(double progress, double offset) {
    }

    /** A point on the course and the running direction there, as a Minecraft yaw. */
    public record Point(double x, double z, float yaw) {
    }

    public static TrackGeometry fromMeters(double straightMeters, double radiusMeters, double widthBlocks,
            double finishMeters, double metersPerBlock) {
        double straight = straightMeters / metersPerBlock;
        double finish = Math.min(Math.max(finishMeters / metersPerBlock, 0), straight);
        return new TrackGeometry(straight, radiusMeters / metersPerBlock, widthBlocks, finish);
    }

    public double lap() {
        return 2 * straight + 2 * Math.PI * radius;
    }

    private double curve() {
        return Math.PI * radius;
    }

    /** Progress where the home straight begins (the end of the last curve). */
    public double homeStraightStart() {
        return wrap(lap() - straight + finish);
    }

    /** Half the extent along X and Z of everything up to {@code beyond} blocks outside the running surface. */
    public double halfLengthX(double beyond) {
        return straight / 2 + radius + width / 2 + beyond;
    }

    public double halfLengthZ(double beyond) {
        return radius + width / 2 + beyond;
    }

    public Spot locate(double x, double z) {
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
