package net.tracen.umapyoi.race;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.registry.umadata.UmaData;
import net.tracen.umapyoi.utils.Position;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * Running styles in a race, with the original game's coefficients: front runners are quick early
 * and fade late, closers start slow and finish strong. Speed coefficients multiply the whole speed
 * (sprint and cruise); acceleration and stamina coefficients are small. The style belongs to the
 * Umamusume (her registry data), not to the individual soul. {@code race_style_strength} scales how
 * far each coefficient is from 1.
 */
public final class RaceStyle {
    /** Opening, middle, late (also the last spurt). */
    private static final double[][] SPEED = {
            { 1.0, 0.98, 0.962 }, // front runner
            { 0.978, 0.991, 0.975 }, // pace chaser
            { 0.938, 0.998, 0.994 }, // late surger
            { 0.931, 1.0, 1.0 }, // end closer
            { 1.063, 0.962, 0.95 } }; // runaway
    private static final double[][] ACCEL = {
            { 1.0, 1.0, 0.996 },
            { 0.985, 1.0, 0.996 },
            { 0.975, 1.0, 1.0 },
            { 0.945, 1.0, 0.997 },
            { 1.17, 0.94, 0.956 } };
    private static final double[] STAMINA = { 0.95, 0.89, 1.0, 0.995, 0.86 };

    private RaceStyle() {
    }

    public static Position of(ItemStack soul, Level level) {
        if (!UmaSoulUtils.hasSoulData(soul))
            return Position.FRONT_RUNNER;
        ResourceLocation name = UmaSoulUtils.getName(soul);
        return UmapyoiAPI.getUmaDataRegistry(level).getOptional(name).map(UmaData::position).orElse(Position.FRONT_RUNNER);
    }

    public static double speed(Position style, RacePhase phase) {
        return scaled(SPEED[style.ordinal()][column(phase)]);
    }

    public static double accel(Position style, RacePhase phase) {
        return scaled(ACCEL[style.ordinal()][column(phase)]);
    }

    public static double stamina(Position style) {
        return scaled(STAMINA[style.ordinal()]);
    }

    public static String key(Position style) {
        return "tooltip.umapyoi.uma_soul.aptitude.strategy." + style.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static int column(RacePhase phase) {
        return switch (phase) {
            case OPENING -> 0;
            case MIDDLE -> 1;
            case FINAL, LAST_SPURT -> 2;
        };
    }

    private static double scaled(double coefficient) {
        return 1 + UmapyoiConfig.RACE_STYLE_STRENGTH.get() * (coefficient - 1);
    }
}
