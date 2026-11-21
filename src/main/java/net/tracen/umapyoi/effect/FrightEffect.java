package net.tracen.umapyoi.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 恐慌 (fright): the fight-or-flight state several Uma traits share. Not the same as 焦躁
 * (PanickingEffect). The bonus cut and the motivation drops live in UmaTraitEvents, because the cut
 * depends on the soul and has to follow it every second.
 */
public class FrightEffect extends MobEffect {
    public FrightEffect() {
        super(MobEffectCategory.HARMFUL, 0x8C5AD8);
    }
}
