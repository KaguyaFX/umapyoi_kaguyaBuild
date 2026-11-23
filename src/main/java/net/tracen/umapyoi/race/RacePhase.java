package net.tracen.umapyoi.race;

import java.util.Locale;

import net.tracen.umapyoi.UmapyoiConfig;

/**
 * The parts of a race by distance run, as in the original game: the opening (first sixth), the
 * middle (second to fourth sixth), the late part or final leg (fifth sixth) and the last spurt
 * (last sixth). The lines are config fractions. The final straight is separate: it depends on the
 * course, not on the distance (see {@link RaceLayout}).
 */
public enum RacePhase {
    OPENING, MIDDLE, FINAL, LAST_SPURT;

    public static RacePhase of(double progress, double distance) {
        double fraction = distance <= 0 ? 0 : progress / distance;
        if (fraction >= UmapyoiConfig.RACE_PHASE_SPURT_FROM.get())
            return LAST_SPURT;
        if (fraction >= UmapyoiConfig.RACE_PHASE_FINAL_FROM.get())
            return FINAL;
        if (fraction >= UmapyoiConfig.RACE_PHASE_MIDDLE_FROM.get())
            return MIDDLE;
        return OPENING;
    }

    public String key() {
        return "umapyoi.race.phase." + name().toLowerCase(Locale.ROOT);
    }
}
