package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.List;

import net.tracen.umapyoi.UmapyoiConfig;

/**
 * Skill effects running on one runner, player or computer, for a few seconds each. They change the
 * race's own numbers, never the soul's stats:
 * <ul>
 * <li>Speed and slowness come in levels. Each level of speed multiplies the Umamusume bonus by 1 +
 * {@code race_skill_speed_per_level}, each level of slowness by 1 minus it. They don't add up: of each
 * only the highest level running counts, as with Minecraft's effects.</li>
 * <li>Acceleration is multiplied by every boost to it that is running.</li>
 * </ul>
 */
final class RaceBoosts {
    private enum Kind {
        SPEED, SLOW, ACCEL
    }

    private record Boost(Kind kind, double amount, int ticks) {
    }

    private final List<Boost> running = new ArrayList<>();

    void speed(int level, int ticks) {
        add(Kind.SPEED, level, ticks);
    }

    void slow(int level, int ticks) {
        add(Kind.SLOW, level, ticks);
    }

    /** Acceleration times {@code by}. */
    void accel(double by, int ticks) {
        add(Kind.ACCEL, by, ticks);
    }

    private void add(Kind kind, double amount, int ticks) {
        if (ticks > 0)
            running.add(new Boost(kind, amount, ticks));
    }

    /** One tick passes. */
    void tick() {
        for (int i = running.size() - 1; i >= 0; i--) {
            Boost boost = running.get(i);
            if (boost.ticks() <= 1)
                running.remove(i);
            else
                running.set(i, new Boost(boost.kind(), boost.amount(), boost.ticks() - 1));
        }
    }

    int speedLevel() {
        return (int) highest(Kind.SPEED);
    }

    int slowLevel() {
        return (int) highest(Kind.SLOW);
    }

    /** What the Umamusume bonus is multiplied by. */
    double bonusFactor() {
        double step = UmapyoiConfig.RACE_SKILL_SPEED_PER_LEVEL.get();
        return (1 + step * speedLevel()) * Math.max(0, 1 - step * slowLevel());
    }

    double accelFactor() {
        double factor = 1;
        for (Boost boost : running)
            if (boost.kind() == Kind.ACCEL)
                factor *= boost.amount();
        return factor;
    }

    /** Ticks left of the longest speed boost at the highest level; 0 if none. */
    int speedTicks() {
        int level = speedLevel();
        int ticks = 0;
        for (Boost boost : running)
            if (boost.kind() == Kind.SPEED && boost.amount() == level)
                ticks = Math.max(ticks, boost.ticks());
        return ticks;
    }

    private double highest(Kind kind) {
        double best = 0;
        for (Boost boost : running)
            if (boost.kind() == kind)
                best = Math.max(best, boost.amount());
        return best;
    }
}
