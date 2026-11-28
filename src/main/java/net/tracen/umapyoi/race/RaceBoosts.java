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
 * <li>Full speed plus so many blocks a second, and the same on top while what was saved up is being used:
 * of each, the highest running.</li>
 * <li>Saving up and stamina use are multiplied by every boost to them that is running; stamina use can also
 * stop altogether.</li>
 * <li>A runner can be made to lose touch (save nothing up) for a while, however close she is.</li>
 * </ul>
 */
final class RaceBoosts {
    private enum Kind {
        SPEED, SLOW, ACCEL, FLAT, RELEASE, CHARGE, DRAIN, FREE, NO_TOUCH
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

    /** Full speed plus this many blocks a second. */
    void flat(double blocksPerSecond, int ticks) {
        add(Kind.FLAT, blocksPerSecond, ticks);
    }

    /** While using what was saved up, this many blocks a second faster still. */
    void release(double blocksPerSecond, int ticks) {
        add(Kind.RELEASE, blocksPerSecond, ticks);
    }

    /** Saving up for the last leg goes {@code times} as fast. */
    void charge(double times, int ticks) {
        add(Kind.CHARGE, times, ticks);
    }

    /** Stamina is used {@code times} as fast. */
    void drain(double times, int ticks) {
        add(Kind.DRAIN, times, ticks);
    }

    /** No stamina is used at all. */
    void free(int ticks) {
        add(Kind.FREE, 1, ticks);
    }

    /** Lost touch, however close: nothing is saved up. */
    void loseTouch(int ticks) {
        add(Kind.NO_TOUCH, 1, ticks);
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
        return product(Kind.ACCEL);
    }

    /** Blocks a second added to full speed. */
    double flatSpeed() {
        return highest(Kind.FLAT);
    }

    /** Blocks a second added while using what was saved up. */
    double releaseSpeed() {
        return highest(Kind.RELEASE);
    }

    double chargeFactor() {
        return product(Kind.CHARGE);
    }

    double drainFactor() {
        return product(Kind.DRAIN);
    }

    boolean free() {
        return highest(Kind.FREE) > 0;
    }

    boolean touchLost() {
        return highest(Kind.NO_TOUCH) > 0;
    }

    private double product(Kind kind) {
        double factor = 1;
        for (Boost boost : running)
            if (boost.kind() == kind)
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
