package net.tracen.umapyoi.blessing;

import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.effect.MobEffectRegistry;

/** The 8 Three Goddesses blessings, in display order. Durations in seconds per level (I, II, III). */
public enum BlessingType {
    RENEWAL("renewal", () -> MobEffectRegistry.BLESSING_RENEWAL, new int[][] { { 300, 600 }, { 300, 600 }, { 300, 600 } }),
    HAPPINESS("happiness", () -> MobEffectRegistry.BLESSING_HAPPINESS, new int[][] { { 60, 60 }, { 45, 45 }, { 30, 30 } }),
    SORROW("sorrow", () -> MobEffectRegistry.BLESSING_SORROW, new int[][] { { 1800, 1800 }, { 1800, 1800 }, { 1800, 1800 } }),
    SURVIVAL("survival", () -> MobEffectRegistry.BLESSING_SURVIVAL, new int[][] { { 1200, 1500 }, { 1200, 1500 }, { 1200, 1500 } }),
    GALE("gale", () -> MobEffectRegistry.BLESSING_GALE, new int[][] { { 1200, 1200 }, { 1200, 1200 }, { 1200, 1200 } }),
    SLUMBER("slumber", () -> MobEffectRegistry.BLESSING_SLUMBER, new int[][] { { 1200, 1200 }, { 1200, 1200 }, { 1200, 1200 } }),
    CALM("calm", () -> MobEffectRegistry.BLESSING_CALM, new int[][] { { 1200, 1200 }, { 1200, 1200 }, { 1200, 1200 } }),
    FOCUS("focus", () -> MobEffectRegistry.BLESSING_FOCUS, new int[][] { { 1200, 1200 }, { 1200, 1200 }, { 1200, 1200 } });

    /** Price in points to refresh a blessing of level I / II / III. */
    public static final int[] EXTEND_COST = { 8, 16, 32 };

    private final String id;
    private final Supplier<Holder<MobEffect>> effect;
    private final int[][] seconds;

    BlessingType(String id, Supplier<Holder<MobEffect>> effect, int[][] seconds) {
        this.id = id;
        this.effect = effect;
        this.seconds = seconds;
    }

    public String id() {
        return id;
    }

    public Holder<MobEffect> effect() {
        return effect.get();
    }

    /** A new duration in ticks; ranges such as 5-10 minutes are rolled each time. */
    public int rollDuration(int level, RandomSource random) {
        int[] range = seconds[Math.min(level, 2)];
        return (range[0] + (range[1] > range[0] ? random.nextInt(range[1] - range[0] + 1) : 0)) * 20;
    }

    /** "20 minutes", "5-10 minutes" or "45 seconds", for tooltips. */
    public Component durationText(int level) {
        int[] range = seconds[Math.min(level, 2)];
        if (range[0] % 60 == 0 && range[1] % 60 == 0) {
            if (range[0] == range[1])
                return Component.translatable("gui.umapyoi.blessing.minutes", range[0] / 60);
            return Component.translatable("gui.umapyoi.blessing.minutes_range", range[0] / 60, range[1] / 60);
        }
        return Component.translatable("gui.umapyoi.blessing.seconds", range[0]);
    }

    public ResourceLocation icon() {
        return ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "textures/mob_effect/blessing_" + id + ".png");
    }

    public Component displayName(int level) {
        return Component.translatable("effect.umapyoi.blessing_" + id).append(" ")
                .append(Component.translatable("enchantment.level." + (level + 1)));
    }

    public Component shortText() {
        return Component.translatable("umapyoi.blessing." + id + ".short");
    }

    public Component flavorText() {
        return Component.translatable("umapyoi.blessing." + id + ".flavor");
    }

    public Component effectText(int level) {
        return Component.translatable("umapyoi.blessing." + id + ".effect." + (level + 1));
    }

    public static BlessingType byOrdinal(int ordinal) {
        BlessingType[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
