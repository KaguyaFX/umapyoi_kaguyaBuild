package net.tracen.umapyoi.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.blessing.BlessingEffect;
import net.tracen.umapyoi.blessing.GaleBlessingEffect;
import net.tracen.umapyoi.blessing.RenewalBlessingEffect;

public class MobEffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT,
            Umapyoi.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> PANICKING = EFFECTS.register("panicking", PanickingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> MOOD_BONUS = EFFECTS.register("mood_bonus", MoodBonus::new);
    public static final DeferredHolder<MobEffect, MobEffect> SLOW_METABOLISM = EFFECTS.register("slow_metabolism", SlowMetabolismEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> NIGHT_OWL = EFFECTS.register("night_owl", NightOwlEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> FRIGHT = EFFECTS.register("fright", FrightEffect::new);

    // Three Goddesses blessings (see net.tracen.umapyoi.blessing)
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_RENEWAL = EFFECTS.register("blessing_renewal", RenewalBlessingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_HAPPINESS = EFFECTS.register("blessing_happiness", BlessingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_SORROW = EFFECTS.register("blessing_sorrow", BlessingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_SURVIVAL = EFFECTS.register("blessing_survival", BlessingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_GALE = EFFECTS.register("blessing_gale", GaleBlessingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_SLUMBER = EFFECTS.register("blessing_slumber", BlessingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_CALM = EFFECTS.register("blessing_calm", BlessingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_FOCUS = EFFECTS.register("blessing_focus", BlessingEffect::new);
}
