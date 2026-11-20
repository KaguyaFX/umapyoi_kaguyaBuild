package net.tracen.umapyoi.blessing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.registry.UmapyoiAttributesRegistry;

/**
 * 疾風: sprint speed +5 / 10 / 15 %, through the mod's own sprint-speed attribute
 * (only applied while sprinting). Less hunger while sprinting: PlayerFoodExhaustionMixin.
 */
public class GaleBlessingEffect extends BlessingEffect {

    public GaleBlessingEffect() {
        addAttributeModifier(UmapyoiAttributesRegistry.SPRINT_SPEED,
                ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "effect.blessing_gale"),
                AttributeModifier.Operation.ADD_VALUE, amplifier -> {
                    double percent = 0.05 * (amplifier + 1);
                    // percent mode multiplies movement speed; otherwise it is added to the 0.1 base speed
                    return UmapyoiConfig.UMASOUL_SPEED_PRECENT_ENABLE.get() ? percent : percent * 0.1;
                });
    }

    /** Share of sprinting exhaustion removed: 20 / 30 / 40 %. */
    public static float exhaustionCut(int amplifier) {
        return 0.2F + 0.1F * amplifier;
    }
}
