package net.tracen.umapyoi.race;

import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.curios.UmaSoulCuriosWrapper;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * What a soul is worth in a race, kept apart from its survival attributes.
 * <p>
 * Speed: a race sprint is the plain player's sprint plus the Umamusume bonus. The bonus is the
 * Umamusume body (the same +5% as in survival) and the speed stat, on the survival curve but capped
 * at {@code race_speed_max_bonus} instead of survival's 0.84. Stamina: blocks of sprinting, from
 * the stamina stat on the same curve. Mood and the soul's growth rate count; graduated and active
 * souls both count at full strength for now.
 *
 * @param bonusRatio the Umamusume bonus as a fraction of the plain sprint (0.4 = 40% faster)
 * @param stamina    full stamina, in blocks of sprinting
 */
public record RaceStats(double bonusRatio, double stamina) {

    public static RaceStats of(ItemStack soul) {
        if (!UmaSoulUtils.hasSoulData(soul))
            return new RaceStats(0, UmapyoiConfig.RACE_STAMINA_BASE.get());
        double mood = UmaSoulUtils.getMotivation(soul).getMultiplier();
        var levels = UmaSoulUtils.getProperty(soul);
        var rates = UmaSoulUtils.getPropertyRate(soul);
        double speed = mood * (1 + rates.speed() / 100.0) * UmaSoulCuriosWrapper.propertyPercentageByValue(levels.speed());
        double staminaStat = mood * (1 + rates.stamina() / 100.0) * UmaSoulCuriosWrapper.propertyPercentageByValue(levels.stamina());
        double body = UmapyoiConfig.UMA_BASE_SPEED.get();
        double bonus = (1 + body) * (1 + UmapyoiConfig.RACE_SPEED_MAX_BONUS.get() * speed) - 1;
        double stamina = UmapyoiConfig.RACE_STAMINA_BASE.get() + UmapyoiConfig.RACE_STAMINA_SCALE.get() * staminaStat;
        return new RaceStats(bonus, stamina);
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
