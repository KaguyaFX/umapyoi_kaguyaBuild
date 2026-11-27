package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.registry.races.RaceDifficulty;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.registry.umadata.Motivations;
import net.tracen.umapyoi.registry.umadata.UmaDataBasicStatus;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * Computer runners: their stats and moods. They are made very different from each other, some fast with no
 * stamina and some the other way round, and yet about as good: each one's stats are moved until she takes, all
 * by herself, as long as she is meant to. How long that is depends on the kind of field it is (wide open, or
 * with a star or two), and now and then someone is simply not up to it.
 * <p>
 * Speed and stamina are what a computer runner goes by; power matters a little, guts only once half the stamina
 * is gone and wisdom not at all, so those three vary more freely.
 */
final class RaceCast {
    /** A runner's soul, and what her stats are multiplied by so that she is just as good as she's meant to be: stats are whole grades. */
    record Part(ItemStack soul, double trim) {
    }

    /** What kind of field it is: how far apart they are (as a part of their time), and what makes the stars better. */
    private enum Kind {
        OPEN(0.30, 0.0030, 0, 0), USUAL(0.45, 0.0060, 0, 0), STAR(0.15, 0.0045, 1, 0.013), RIVALS(0.10, 0.0045, 2, 0.010);

        private final double chance;
        private final double spread;
        private final int stars;
        private final double lead;

        Kind(double chance, double spread, int stars, double lead) {
            this.chance = chance;
            this.spread = spread;
            this.stars = stars;
            this.lead = lead;
        }

        private static Kind pick(RandomSource random) {
            double at = random.nextDouble();
            for (Kind kind : values())
                if ((at -= kind.chance) < 0)
                    return kind;
            return USUAL;
        }
    }

    private static final int SPEED = 0, STAMINA = 1, POWER = 2, GUTS = 3, WISDOM = 4;
    /** The share of runners who aren't up to it, and by how much (as a part of their time). */
    private static final double HOPELESS = 0.08;
    private static final double HOPELESS_FROM = 0.015, HOPELESS_TO = 0.03;
    /** How many grades speed and stamina are moved at most. */
    private static final int SPEED_RANGE = 5, STAMINA_RANGE = 8;
    private static final double TRIM_RANGE = 0.04;
    /** Perfect, good, normal, down and bad, out of 100. */
    private static final int[] MOODS = { 20, 30, 30, 15, 5 };
    /** The highest a stat goes. */
    private static final int MAX = 39;

    private final ServerLevel level;
    private final TrackGeometry track;
    private final RaceSetup setup;
    private final RaceLayout layout;
    private final double distance;
    private final double plainSprint;

    RaceCast(ServerLevel level, TrackGeometry track, RaceSetup setup, RaceLayout layout, double distance, double plainSprint) {
        this.level = level;
        this.track = track;
        this.setup = setup;
        this.layout = layout;
        this.distance = distance;
        this.plainSprint = plainSprint;
    }

    /** @param around what their stats are around: speed, stamina, power, guts, wisdom */
    List<Part> cast(List<ItemStack> souls, int[] around, RandomSource random) {
        return cast(souls, around, null, random);
    }

    /**
     * @param around what their stats are around: speed, stamina, power, guts, wisdom
     * @param major  for a Major Match: every stat raised to its floor and then some, and the moods it says
     */
    List<Part> cast(List<ItemStack> souls, int[] around, @Nullable RaceDifficulty.Major major, RandomSource random) {
        Kind kind = Kind.pick(random);
        double usual = 0;
        for (ItemStack soul : souls) {
            set(soul, around);
            usual += time(soul, 1) / souls.size();
        }
        int star = random.nextInt(Math.max(1, souls.size()));
        int rival = souls.size() < 2 ? star : (star + 1 + random.nextInt(souls.size() - 1)) % souls.size();
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < souls.size(); i++) {
            double off = Mth.clamp(random.nextGaussian(), -2, 2) * kind.spread;
            if (kind.stars > 0 && i == star || kind.stars > 1 && i == rival)
                off = -kind.lead;
            else if (random.nextDouble() < HOPELESS)
                off = Mth.lerp(random.nextDouble(), HOPELESS_FROM, HOPELESS_TO);
            parts.add(make(souls.get(i), around, usual * (1 + off), random));
        }
        // Moods come on top: everyone can see them. So do skills, by how good the field is.
        int level = 0;
        for (int stat : around)
            level += stat;
        level = Math.round(level / (float) Math.max(1, around.length));
        if (major != null)
            for (Part part : parts)
                set(part.soul(), raised(UmaSoulUtils.getProperty(part.soul()).toArray(), major, random));
        for (Part part : parts) {
            UmaSoulUtils.setMotivation(part.soul(), major == null ? mood(random) : mood(major.moods(), random));
            RaceAiSkills.handOut(part.soul(), level, setup.surface(), random);
        }
        return parts;
    }

    /** Each stat at least the floor, and a few grades more on top, never past MAX. */
    private static int[] raised(int[] stats, RaceDifficulty.Major major, RandomSource random) {
        int[] raised = new int[stats.length];
        for (int i = 0; i < stats.length; i++)
            raised[i] = Math.min(MAX, Math.max(stats[i], major.floor()) + major.rise().draw(random));
        return raised;
    }

    private static Motivations mood(RaceDifficulty.Moods moods, RandomSource random) {
        return switch (moods) {
            case PERFECT -> Motivations.PERFECT;
            case NO_BAD -> {
                Motivations mood;
                do
                    mood = mood(random);
                while (mood == Motivations.BAD);
                yield mood;
            }
            default -> mood(random);
        };
    }

    private Part make(ItemStack soul, int[] around, double wanted, RandomSource random) {
        int[] stats = shape(around, random);
        // The estimate is off by a little that is hers (her style, mostly): found out by running her.
        double error = 0;
        for (int i = 0; i < 2; i++) {
            stats = fit(soul, around, stats, wanted - error);
            error = time(soul, 1) - estimate(stats(soul, 1));
        }
        stats = fit(soul, around, stats, wanted - error);
        double left = time(soul, 1) - wanted;
        double gain = (estimate(stats(soul, 1)) - estimate(stats(soul, 1.01))) / 0.01;
        double trim = gain <= 0 ? 1 : Mth.clamp(1 + left / gain, 1 - TRIM_RANGE, 1 + TRIM_RANGE);
        return new Part(soul, trim);
    }

    /** What she's like, before it's known how good that makes her. */
    private static int[] shape(int[] around, RandomSource random) {
        int[] off = switch (random.nextInt(25)) {
            case 0, 1, 2, 3, 4 -> new int[] { 0, 0, 0, 0, 0 }; // good at everything
            case 5, 6, 7, 8, 9, 10 -> new int[] { 2, -4, 1, 0, 0 }; // fast
            case 11, 12, 13, 14, 15, 16 -> new int[] { -2, 5, 0, 1, 0 }; // goes on and on
            case 17, 18, 19 -> new int[] { -1, 0, 5, 2, -2 }; // strong
            default -> new int[] { spread(random, 3), spread(random, 5), spread(random, 4), spread(random, 3), spread(random, 3) }; // one of a kind
        };
        off[STAMINA] += spread(random, 1);
        off[POWER] += spread(random, 2);
        off[GUTS] += spread(random, 2) + spread(random, 2);
        off[WISDOM] += spread(random, 2) + spread(random, 2);
        int[] stats = new int[5];
        for (int i = 0; i < 5; i++)
            stats[i] = Math.max(1, around[i] + off[i]);
        return stats;
    }

    private static int spread(RandomSource random, int range) {
        return random.nextInt(range * 2 + 1) - range;
    }

    /** Moves speed and stamina until the estimate is as wanted; the soul ends up with those stats. */
    private int[] fit(ItemStack soul, int[] around, int[] from, double wanted) {
        int[] stats = from.clone();
        for (int step = 0; step < 40; step++) {
            set(soul, stats);
            double off = estimate(stats(soul, 1)) - wanted;
            int up = off > 0 ? 1 : -1;
            int[] faster = stats.clone();
            faster[SPEED] = Math.max(1, faster[SPEED] + up);
            set(soul, faster);
            double speedStep = Math.abs(estimate(stats(soul, 1)) - wanted - off);
            int[] longer = stats.clone();
            longer[STAMINA] = Math.max(1, longer[STAMINA] + up);
            set(soul, longer);
            double staminaStep = Math.abs(estimate(stats(soul, 1)) - wanted - off);
            boolean speed = Math.abs(faster[SPEED] - around[SPEED]) <= SPEED_RANGE && faster[SPEED] != stats[SPEED];
            boolean stamina = Math.abs(longer[STAMINA] - around[STAMINA]) <= STAMINA_RANGE && longer[STAMINA] != stats[STAMINA] && staminaStep > 1e-3;
            if (speed && Math.abs(off) > speedStep * 0.6)
                stats = faster;
            else if (stamina && Math.abs(off) > staminaStep * 0.6)
                stats = longer;
            else
                break;
        }
        set(soul, stats);
        return stats;
    }

    private static void set(ItemStack soul, int[] stats) {
        soul.set(DataComponentsTypeRegistry.UMADATA_BASIC_STATUS.get(), UmaDataBasicStatus.init(stats));
    }

    private static Motivations mood(RandomSource random) {
        int at = random.nextInt(100);
        for (int i = 0; i < MOODS.length; i++)
            if ((at -= MOODS[i]) < 0)
                return Motivations.values()[i];
        return Motivations.NORMAL;
    }

    private RaceStats stats(ItemStack soul, double trim) {
        return RaceStats.of(soul, RaceStyle.of(soul, level), setup.surface(), setup.category(), UmapyoiConfig.RACE_AI_BOOST.get() * trim);
    }

    /** How long she takes by herself, pacing herself as well as she can. */
    private double time(ItemStack soul, double trim) {
        return alone(stats(soul, trim), RaceStyle.aptitudes(soul, level));
    }

    private double alone(RaceStats stats, Aptitude[] styles) {
        RaceAi ai = RaceAi.exact(stats.style(), distance);
        double lane = track.inner() + 0.5;
        ai.plan(() -> new RaceRunner(setup, stats, layout, plainSprint, styles), track, lane);
        RaceRunner runner = new RaceRunner(setup, stats, layout, plainSprint, styles);
        runner.start(RaceSession.START);
        for (int tick = 1; tick < 20 * 600; tick++) {
            double before = runner.progress();
            RaceAi.step(runner, track, lane, ai.sprint(runner), RaceTraffic.FREE);
            if (runner.progress() >= distance)
                return (tick - 1 + (distance - before) / (runner.progress() - before)) / 20;
        }
        return 600;
    }

    /** About the same, worked out instead of run. */
    private double estimate(RaceStats stats) {
        double style = 0;
        RacePhase[] sixths = { RacePhase.OPENING, RacePhase.MIDDLE, RacePhase.MIDDLE, RacePhase.MIDDLE, RacePhase.FINAL, RacePhase.LAST_SPURT };
        for (RacePhase phase : sixths)
            style += RaceStyle.speed(stats.style(), phase) / sixths.length;
        double full = plainSprint * style * (1 + stats.bonusRatio());
        double cruise = UmapyoiConfig.RACE_CRUISE_RATIO.get();
        // Below the low line the bonus goes: seconds per block, on average.
        double tired = 0;
        for (int i = 0; i < 10; i++)
            tired += 1 / (plainSprint * style * (1 + stats.bonusRatio() * RaceStats.bonusLeft(0.5 * (i + 0.5) / 10 - 1e-6, stats.emptyFloor()))) / 10;
        double blocks = stats.stamina() / setup.ground().drain();
        double sprinted = Math.min(distance, blocks);
        double fresh = Math.min(sprinted, blocks / 2);
        double gain = full * (1 - cruise);
        return (distance - sprinted) / (full * cruise) + fresh / full + (sprinted - fresh) * tired + gain * gain / (2 * stats.accel() * full);
    }
}
