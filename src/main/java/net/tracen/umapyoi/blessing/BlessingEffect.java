package net.tracen.umapyoi.blessing;

import java.util.Set;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;

/**
 * A Three Goddesses blessing. Neither milk nor a totem removes it; it only goes away
 * when it runs out or when the player takes off their Uma soul (BlessingEvents).
 */
public class BlessingEffect extends MobEffect {
    public static final int COLOR = 0xF2C14E;

    public BlessingEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {
        // no cures on purpose
    }
}
