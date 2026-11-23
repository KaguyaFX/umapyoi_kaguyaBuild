package net.tracen.umapyoi.race;

import java.util.List;
import java.util.Locale;

import net.tracen.umapyoi.UmapyoiConfig;

/**
 * The state of the ground, which the weather decides: firm (good), yielding (slightly heavy) or
 * heavy. Worse ground slows acceleration and drains stamina faster; the numbers are config lists.
 */
public enum RaceGround {
    FIRM, YIELDING, HEAVY;

    /** What acceleration is multiplied by on this ground. */
    public double accel() {
        return value(UmapyoiConfig.RACE_GROUND_ACCEL.get());
    }

    /** What the stamina drain is multiplied by on this ground. */
    public double drain() {
        return value(UmapyoiConfig.RACE_GROUND_DRAIN.get());
    }

    private double value(List<?> list) {
        return ordinal() < list.size() && list.get(ordinal()) instanceof Number number ? number.doubleValue() : 1;
    }

    public String key() {
        return "umapyoi.race.ground." + name().toLowerCase(Locale.ROOT);
    }
}
