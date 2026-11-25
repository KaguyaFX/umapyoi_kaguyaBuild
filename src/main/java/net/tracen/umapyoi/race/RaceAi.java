package net.tracen.umapyoi.race;

import java.util.function.Supplier;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.utils.Position;

/**
 * When a computer runner sprints. Front runners sprint out of the gates for a while; everyone cruises
 * through the middle; then comes a last sprint, timed so that the stamina lasts exactly to the
 * finish. Where it has to start is found by running the race in advance, alone; a personal error
 * then shifts it, so some go too early and some too late.
 */
final class RaceAi {
    /** They stop sprinting with this much stamina left rather than run dry... */
    private static final double HOLD = 0.02;
    /** ...unless the finish is this close. */
    private static final double HOLD_UNTIL = 12;
    private static final int SEARCH_STEPS = 9;
    /** However far they like to sprint right away, it's not more than this much of their stamina. */
    private static final double EARLY_STAMINA = 0.4;

    private final double distance;
    /** Sprinting from the start until here. */
    private double earlyUntil;
    /** How far the last sprint's start is from the best place, in blocks (below zero = early). */
    private final double error;
    /** Sprinting from here to the finish. */
    private double sprintFrom;
    private boolean planned;

    RaceAi(Position style, double distance, RandomSource random) {
        this.distance = distance;
        this.earlyUntil = distance * early(style);
        double spread = UmapyoiConfig.RACE_AI_ERROR.get();
        this.error = distance * Mth.clamp(random.nextGaussian() * spread, -2.5 * spread, 2.5 * spread);
        this.sprintFrom = distance * 0.6;
    }

    /** The part of the race sprinted right away. */
    private static double early(Position style) {
        return switch (style) {
            case RUNAWAY -> 0.30;
            case FRONT_RUNNER -> 0.12;
            case PACE_CHASER -> 0.06;
            case LATE_SURGER -> 0.03;
            case END_CLOSER -> 0;
        };
    }

    boolean planned() {
        return planned;
    }

    /**
     * Finds where the last sprint has to start.
     *
     * @param fresh  a new runner with the same stats each time, not started
     * @param offset how far off the reference line they mean to run
     */
    void plan(Supplier<RaceRunner> fresh, TrackGeometry track, double offset) {
        planned = true;
        earlyUntil = Math.min(earlyUntil, fresh.get().stats().stamina() * EARLY_STAMINA);
        double low = earlyUntil;
        double high = distance;
        if (lasts(fresh.get(), track, offset, low)) {
            high = low; // strong enough to sprint all the way
        } else {
            for (int i = 0; i < SEARCH_STEPS; i++) {
                double middle = (low + high) / 2;
                if (lasts(fresh.get(), track, offset, middle))
                    high = middle;
                else
                    low = middle;
            }
        }
        sprintFrom = high + error;
    }

    /** Whether stamina lasts to the finish when the last sprint starts at {@code from}. */
    private boolean lasts(RaceRunner runner, TrackGeometry track, double offset, double from) {
        runner.start(0);
        for (int tick = 0; tick < 20 * 600 && runner.progress() < distance; tick++) {
            step(runner, track, offset, runner.progress() < earlyUntil || runner.progress() >= from, RaceTraffic.FREE);
            if (runner.exhausted())
                return false;
        }
        return true;
    }

    /**
     * One tick of a computer runner.
     *
     * @param limit the most the runner in front allows, in blocks per second
     */
    static RaceStatePayload step(RaceRunner runner, TrackGeometry track, double offset, boolean sprint, double limit) {
        RaceStatePayload state = runner.state(1, limit);
        runner.move(state, sprint);
        double moved = runner.speed() / 20;
        runner.useStamina(state, moved);
        runner.advance(moved * track.along(runner.progress(), offset));
        return state;
    }

    boolean sprint(RaceRunner runner) {
        double progress = runner.progress();
        if (progress >= distance)
            return false; // past the post
        if (progress >= earlyUntil && progress < sprintFrom)
            return false;
        return runner.staminaFraction() > HOLD || distance - progress < HOLD_UNTIL;
    }
}
