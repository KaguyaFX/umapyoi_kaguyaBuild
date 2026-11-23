package net.tracen.umapyoi.race;

import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.curios.UmaSoulCuriosWrapper;
import net.tracen.umapyoi.utils.Position;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * What a soul is worth in a race, kept apart from its survival attributes. Every stat goes through
 * the survival curve (the S-shaped curve with SS+ in the middle), with mood and the soul's growth
 * rate; graduated and active souls both count at full strength for now.
 * <ul>
 * <li>Speed: a race sprint is the plain player's sprint plus the Umamusume bonus: the body (the same
 * +5% as in survival) and the speed stat, capped at {@code race_speed_max_bonus} instead of
 * survival's 0.84.</li>
 * <li>Stamina: blocks of sprinting, from the stamina stat, times the running style's coefficient.</li>
 * <li>Power: acceleration, in blocks per second per second.</li>
 * <li>Wisdom: how fast skill action points come back in the race world.</li>
 * </ul>
 *
 * @param bonusRatio the Umamusume bonus as a fraction of the plain sprint (0.4 = 40% faster)
 * @param stamina    full stamina, in blocks of sprinting
 * @param accel      blocks per second per second
 * @param apRegen    fraction of the usual action point recovery
 */
public record RaceStats(Position style, double bonusRatio, double stamina, double accel, double apRegen) {

    public static RaceStats of(ItemStack soul, Position style) {
        double accelBase = UmapyoiConfig.RACE_ACCEL_BASE.get();
        double apMin = UmapyoiConfig.RACE_ACTION_POINT_REGEN.get();
        if (!UmaSoulUtils.hasSoulData(soul))
            return new RaceStats(style, 0, UmapyoiConfig.RACE_STAMINA_BASE.get() * RaceStyle.stamina(style), accelBase, apMin);
        double bonus = (1 + UmapyoiConfig.UMA_BASE_SPEED.get())
                * (1 + UmapyoiConfig.RACE_SPEED_MAX_BONUS.get() * curve(soul, 0)) - 1;
        double stamina = (UmapyoiConfig.RACE_STAMINA_BASE.get() + UmapyoiConfig.RACE_STAMINA_SCALE.get() * curve(soul, 1))
                * RaceStyle.stamina(style);
        double accel = accelBase + UmapyoiConfig.RACE_ACCEL_SCALE.get() * curve(soul, 2);
        double apRegen = Math.min(UmapyoiConfig.RACE_ACTION_POINT_REGEN_MAX.get(), apMin + (1 - apMin) * curve(soul, 4));
        return new RaceStats(style, bonus, stamina, accel, apRegen);
    }

    /** Wisdom's action point rate for a soul, without needing its style. */
    public static double apRegen(ItemStack soul) {
        double apMin = UmapyoiConfig.RACE_ACTION_POINT_REGEN.get();
        if (!UmaSoulUtils.hasSoulData(soul))
            return apMin;
        return Math.min(UmapyoiConfig.RACE_ACTION_POINT_REGEN_MAX.get(), apMin + (1 - apMin) * curve(soul, 4));
    }

    /** A stat through the survival curve, with mood and growth rate: 0 speed, 1 stamina, 2 power, 3 guts, 4 wisdom. */
    private static double curve(ItemStack soul, int stat) {
        double mood = UmaSoulUtils.getMotivation(soul).getMultiplier();
        int level = UmaSoulUtils.getProperty(soul).toArray()[stat];
        int rate = UmaSoulUtils.getPropertyRate(soul).toArray()[stat];
        return mood * (1 + rate / 100.0) * UmaSoulCuriosWrapper.propertyPercentageByValue(level);
    }

    /**
     * How much of the bonus is left at a stamina level (0 to 1): all of it down to the low line,
     * then a sudden drop, falling further to the minimum at empty.
     */
    public static double bonusLeft(double staminaFraction) {
        double low = UmapyoiConfig.RACE_LOW_STAMINA.get();
        if (staminaFraction >= low || low <= 0)
            return 1;
        double start = UmapyoiConfig.RACE_LOW_BONUS_START.get();
        double end = UmapyoiConfig.RACE_LOW_BONUS_END.get();
        double t = Math.max(0, staminaFraction) / low;
        return end + (start - end) * t;
    }
}
