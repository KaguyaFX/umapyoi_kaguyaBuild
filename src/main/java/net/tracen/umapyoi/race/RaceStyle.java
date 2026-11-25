package net.tracen.umapyoi.race;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.registry.umadata.UmaData;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.Position;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * Running styles in a race. Two things by that name:
 * <ul>
 * <li>An Umamusume's own style (her registry data) gives her pace a shape, with the original game's
 * coefficients: front runners are quick early and fade late, closers start slow and finish strong. By
 * itself that makes closers about one percent faster, which is part of what makes up for the traffic
 * they run into. {@code race_style_strength} scales how far each coefficient is from 1.</li>
 * <li>The style somebody runs in is where in the field they are ({@link #zone}), whatever their own is.
 * How well they run in that one (their aptitude for it) decides how much they save up for the last leg
 * there ({@link RaceRunner#chargeUp}).</li>
 * </ul>
 */
public final class RaceStyle {
    /** Opening, middle, late (also the last spurt). */
    private static final double[][] SPEED = {
            { 1.0, 0.98, 0.962 }, // front runner
            { 0.978, 0.991, 0.975 }, // pace chaser
            { 0.938, 0.998, 0.994 }, // late surger
            { 0.931, 1.0, 1.0 }, // end closer
            { 1.058, 0.957, 0.945 } }; // runaway: a little under the original's, whose runaways have less stamina instead
    private static final double[][] ACCEL = {
            { 1.0, 1.0, 0.996 },
            { 0.985, 1.0, 0.996 },
            { 0.975, 1.0, 1.0 },
            { 0.945, 1.0, 0.997 },
            { 1.17, 0.94, 0.956 } };
    /** Up to this far back in the field (0 = first, 1 = last) it's front running, pace chasing, late surging; behind that, closing. */
    private static final double FRONT = 1 / 6.0;
    private static final double PACE = 5 / 12.0;
    private static final double LATE = 3 / 4.0;

    private RaceStyle() {
    }

    public static Position of(ItemStack soul, Level level) {
        if (!UmaSoulUtils.hasSoulData(soul))
            return Position.FRONT_RUNNER;
        ResourceLocation name = UmaSoulUtils.getName(soul);
        return UmapyoiAPI.getUmaDataRegistry(level).getOptional(name).map(UmaData::position).orElse(Position.FRONT_RUNNER);
    }

    /** How well a soul runs in front, with the pace, from behind and from the very back. */
    static Aptitude[] aptitudes(ItemStack soul, Level level) {
        UmaData data = !UmaSoulUtils.hasSoulData(soul) ? null
                : UmapyoiAPI.getUmaDataRegistry(level).getOptional(UmaSoulUtils.getName(soul)).orElse(null);
        Aptitude[] aptitudes = new Aptitude[4];
        for (int i = 0; i < 4; i++)
            aptitudes[i] = data == null ? Aptitude.C : data.styleAptitude(Position.values()[i]);
        return aptitudes;
    }

    /** The style of somebody who is {@code rank}th of {@code field}. */
    static Position zone(int rank, int field) {
        double back = (rank - 0.5) / field;
        return back < FRONT ? Position.FRONT_RUNNER : back < PACE ? Position.PACE_CHASER : back < LATE ? Position.LATE_SURGER : Position.END_CLOSER;
    }

    public static double speed(Position style, RacePhase phase) {
        return scaled(SPEED[style.ordinal()][column(phase)]);
    }

    public static double accel(Position style, RacePhase phase) {
        return scaled(ACCEL[style.ordinal()][column(phase)]);
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
