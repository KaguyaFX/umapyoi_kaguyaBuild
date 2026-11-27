package net.tracen.umapyoi.race.track;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Low wooded hills round the two turns, beyond the trees: the near end of the scenery whose far end
 * is the panorama. They rise past the second row of trees, crest and fall back to the plain, and keep
 * away from the straights, so the back straight stays open towards Fuji and the stand's side to the city.
 * Seeded from the course's place, so the same course always gets the same hills.
 */
final class TrackHills {
    /** Where they start, and where they have fallen back to the plain, beyond the surface's outer edge. */
    static final double FROM = 18, TO = 88;
    /** How far along the straights they keep away from, beyond the ends. */
    private static final double SIDE_FROM = 5, SIDE_FULL = 45;
    private static final double RISE = 35, FALL_FROM = 45;

    private final TrackGeometry track;
    private final SimplexNoise noise;

    TrackHills(CourseLayout layout) {
        this.track = layout.track();
        this.noise = new SimplexNoise(RandomSource.create(layout.origin().asLong() ^ 0x48494C4CL));
    }

    /** Blocks above the ground at this column, 0 where there's no hill. */
    int height(int i, int k) {
        double x = i + 0.5, z = k + 0.5;
        double d = track.locate(x, z).offset() - track.outer() - FROM;
        if (d <= 0 || d >= TO - FROM)
            return 0;
        double side = smooth((Math.abs(x) - track.straight() / 2 - SIDE_FROM) / (SIDE_FULL - SIDE_FROM));
        if (side <= 0)
            return 0;
        double shape = smooth(d / RISE) * (1 - smooth((d - FALL_FROM) / (TO - FROM - FALL_FROM)));
        double rolling = 6 + 4.5 * noise.getValue(x / 70, z / 70) + 1.5 * noise.getValue(x / 23 + 100, z / 23);
        return Math.max(0, (int) Math.round(side * shape * rolling));
    }

    private static double smooth(double t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
