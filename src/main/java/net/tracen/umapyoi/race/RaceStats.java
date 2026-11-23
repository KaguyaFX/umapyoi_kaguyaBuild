package net.tracen.umapyoi.race;

import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.curios.UmaSoulCuriosWrapper;
import net.tracen.umapyoi.utils.Distance;
import net.tracen.umapyoi.utils.Position;
import net.tracen.umapyoi.utils.Surface;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * What a soul is worth in a race, kept apart from its survival attributes. Every stat goes through
 * the survival curve (the S-shaped curve with SS+ in the middle), with the soul's growth rate and a
 * race mood multiplier; graduated and active souls both count at full strength for now.
 * <ul>
 * <li>Speed: a race sprint is the plain player's sprint plus the Umamusume bonus: the body (the same
 * +5% as in survival) and the speed stat, capped at {@code race_speed_max_bonus} instead of
 * survival's 0.84, times the aptitude for the race's distance.</li>
 * <li>Stamina: blocks of sprinting, from the stamina stat, times the running style's coefficient.</li>
 * <li>Power: acceleration, in blocks per second per second, times the aptitude for the surface.</li>
 * <li>Guts: how much of the speed bonus is left when stamina runs out.</li>
 * <li>Wisdom: how fast skill action points come back in the race world.</li>
 * </ul>
 *
 * @param bonusRatio     the Umamusume bonus as a fraction of the plain sprint (0.4 = 40% faster)
 * @param stamina        full stamina, in blocks of sprinting
 * @param accel          blocks per second per second
 * @param apRegen        fraction of the usual action point recovery
 * @param emptyFloor     share of the bonus kept with no stamina left
 * @param distanceFactor aptitude for the distance
 * @param surfaceFactor  aptitude for the surface
 * @param mood           the race mood multiplier
 */
public record RaceStats(Position style, double bonusRatio, double stamina, double accel, double apRegen, double emptyFloor,
        double distanceFactor, double surfaceFactor, double mood) {
    private static final int SPEED = 0, STAMINA = 1, POWER = 2, GUTS = 3, WISDOM = 4;
    public static RaceStats of(ItemStack soul, Position style, Surface surface, Distance distance) {
        double lowBonus = UmapyoiConfig.RACE_LOW_BONUS_END.get();
        double apMin = UmapyoiConfig.RACE_ACTION_POINT_REGEN.get();
        if (!UmaSoulUtils.hasSoulData(soul))
            return new RaceStats(style, 0, UmapyoiConfig.RACE_STAMINA_BASE.get() * RaceStyle.stamina(style),
                    UmapyoiConfig.RACE_ACCEL_BASE.get(), apMin, lowBonus, 1, 1, 1);
        double distanceFactor = distance == null ? 1 : distance.GetMultiplier(soul);
        double surfaceFactor = surface.GetMultiplier(soul);
        double mood = mood(soul);
        double bonus = ((1 + UmapyoiConfig.UMA_BASE_SPEED.get())
                * (1 + UmapyoiConfig.RACE_SPEED_MAX_BONUS.get() * curve(soul, SPEED)) - 1) * distanceFactor;
        double stamina = (UmapyoiConfig.RACE_STAMINA_BASE.get() + UmapyoiConfig.RACE_STAMINA_SCALE.get() * curve(soul, STAMINA))
                * RaceStyle.stamina(style);
        double accel = (UmapyoiConfig.RACE_ACCEL_BASE.get() + UmapyoiConfig.RACE_ACCEL_SCALE.get() * curve(soul, POWER)) * surfaceFactor;
        double floorMax = UmapyoiConfig.RACE_GUTS_FLOOR_MAX.get();
        double emptyFloor = lowBonus + (floorMax - lowBonus) * Math.min(1, curve(soul, GUTS) / curveMax());
        return new RaceStats(style, bonus, stamina, accel, apRegen(soul), emptyFloor, distanceFactor, surfaceFactor, mood);
    }

    /** Wisdom's action point rate for a soul, without needing its style. */
    public static double apRegen(ItemStack soul) {
        double apMin = UmapyoiConfig.RACE_ACTION_POINT_REGEN.get();
        if (!UmaSoulUtils.hasSoulData(soul))
            return apMin;
        return Math.min(UmapyoiConfig.RACE_ACTION_POINT_REGEN_MAX.get(), apMin + (1 - apMin) * curve(soul, WISDOM));
    }

    /** The race mood multiplier: a much narrower range than survival's. */
    private static double mood(ItemStack soul) {
        java.util.List<?> multipliers = UmapyoiConfig.RACE_MOOD_MULTIPLIERS.get();
        int index = UmaSoulUtils.getMotivation(soul).ordinal();
        return index < multipliers.size() && multipliers.get(index) instanceof Number number ? number.doubleValue() : 1;
    }

    /** The best any stat reaches on the survival curve (level 39). */
    private static double curveMax() {
        return UmaSoulCuriosWrapper.propertyPercentageByValue(39);
    }

    /** A stat through the survival curve, with mood and growth rate. */
    private static double curve(ItemStack soul, int stat) {
        int level = UmaSoulUtils.getProperty(soul).toArray()[stat];
        int rate = UmaSoulUtils.getPropertyRate(soul).toArray()[stat];
        return mood(soul) * (1 + rate / 100.0) * UmaSoulCuriosWrapper.propertyPercentageByValue(level);
    }

    /**
     * How much of the bonus is left at a stamina level (0 to 1): all of it down to the low line,
     * then a sudden drop, falling further to {@code floor} at empty.
     */
    public static double bonusLeft(double staminaFraction, double floor) {
        double low = UmapyoiConfig.RACE_LOW_STAMINA.get();
        if (staminaFraction >= low || low <= 0)
            return 1;
        double start = UmapyoiConfig.RACE_LOW_BONUS_START.get();
        double t = Math.max(0, staminaFraction) / low;
        double bottom = Math.min(start, floor);
        return bottom + (start - bottom) * t;
    }
}
