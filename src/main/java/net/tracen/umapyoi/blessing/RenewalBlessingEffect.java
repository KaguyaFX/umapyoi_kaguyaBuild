package net.tracen.umapyoi.blessing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.tracen.umapyoi.Umapyoi;

/** 延續: 4 / 6 / 8 points of absorption, topped up every 30 / 25 / 20 seconds. */
public class RenewalBlessingEffect extends BlessingEffect {

    public RenewalBlessingEffect() {
        addAttributeModifier(Attributes.MAX_ABSORPTION,
                ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "effect.blessing_renewal"),
                AttributeModifier.Operation.ADD_VALUE, amplifier -> absorption(amplifier));
    }

    public static float absorption(int amplifier) {
        return 4 + 2 * amplifier;
    }

    public static int refillTicks(int amplifier) {
        return (30 - 5 * amplifier) * 20;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.tickCount % refillTicks(amplifier) == 0)
            refill(entity, amplifier);
        return true;
    }

    public static void refill(LivingEntity entity, int amplifier) {
        entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), absorption(amplifier)));
    }
}
