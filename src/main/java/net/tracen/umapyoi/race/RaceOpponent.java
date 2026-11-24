package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.registry.umadata.UmaData;
import net.tracen.umapyoi.registry.umadata.UmaDataBasicStatus;
import net.tracen.umapyoi.utils.GachaRanking;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * A computer runner: an Umamusume with made-up stats, her pacing ({@link RaceAi}) and
 * the figure people see ({@link RunnerEntity}). Where she is comes from her race progress and how far
 * off the inside she runs.
 * <p>
 * She likes the inside. A slower runner in her way ({@link RaceTraffic}) makes her look for a lane to
 * pass in: inside or outside of the bunch that one is in, whichever is free and nearer. She can't move
 * into someone beside her, and she doesn't tuck in behind someone slower.
 */
final class RaceOpponent {
    /** Each stat is the one asked for plus or minus up to this. */
    private static final int STAT_SPREAD = 2;
    /** Well suited: aptitude A or better for the race's surface and distance. Suited: B. */
    private static final double WELL_SUITED = 1.0;
    private static final double SUITED = 0.9;
    /** Everyone keeps their stall's lane this far, then moves over to the lane they like. */
    private static final double KEEP_LANE = 30;
    /** They like to run up to this far off the innermost lane: further out is further to run. */
    private static final double LANE_SPREAD = 1;
    /** Blocks sideways per tick. */
    private static final double SIDESTEP = 0.06;
    /** Beside her: within this along the course. */
    private static final double BESIDE = 0.9;
    /** Room between two runners side by side. */
    private static final double ACROSS = 0.95;
    /** On top of each other: the outer one is moved off. */
    private static final double OVERLAP = 0.5;
    /** How far ahead she looks for someone to pass, and how many seconds she'd need to get to them at most. */
    private static final double LOOK_AHEAD = 9;
    private static final double CATCH_UP_SECONDS = 15;
    /** Runners this close to each other along the course, and side by side, are one bunch. */
    private static final double BUNCH_LENGTH = 3;
    private static final double BUNCH_WIDTH = 1.7;
    /** She keeps to the lane she passes in for this many ticks after there's nobody left to pass. */
    private static final int LINGER = 30;
    /** What a block outward counts for against a block inward, early and later on. */
    private static final double OUTWARD_EARLY = 4;
    private static final double OUTWARD = 1.15;

    private final RaceRunner runner;
    private final RaceAi ai;
    private final ItemStack soul;
    private final double lane;
    private final double stallOffset;
    private final int delay;
    private double offset;
    /** The lane she passes someone in, and until when. */
    private double passing;
    private int passingUntil = -1;
    @Nullable
    private RunnerEntity entity;
    /** Seconds since the gates opened; below zero while still running. */
    private double time = -1;

    private RaceOpponent(RaceRunner runner, RaceAi ai, ItemStack soul, double lane, double stallOffset, int delay) {
        this.runner = runner;
        this.ai = ai;
        this.soul = soul;
        this.lane = lane;
        this.stallOffset = stallOffset;
        this.offset = stallOffset;
        this.passing = stallOffset;
        this.delay = delay;
    }

    /**
     * Picks the field: different Umamusume, the best suited to the race first.
     *
     * @param stats  what each one's stats are around: speed, stamina, power, guts, wisdom
     * @param stalls where each starts, as an offset from the course's reference line
     * @param except the player's own soul: she doesn't run against herself, in whatever outfit
     */
    static List<RaceOpponent> field(ServerLevel level, TrackGeometry track, RaceSetup setup, RaceLayout layout, double distance,
            double plainSprint, int[] stats, List<Double> stalls, ItemStack except, RandomSource random) {
        Registry<UmaData> registry = UmapyoiAPI.getUmaDataRegistry(level);
        List<ItemStack> souls = new ArrayList<>();
        List<ItemStack> suited = new ArrayList<>();
        List<ItemStack> others = new ArrayList<>();
        for (Map.Entry<ResourceKey<UmaData>, UmaData> entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (id.equals(UmaData.DEFAULT_UMA_ID) || entry.getValue().ranking() == GachaRanking.EASTER_EGG)
                continue;
            ItemStack soul = UmaSoulUtils.initUmaSoul(ItemRegistry.UMA_SOUL.get().getDefaultInstance(), id, entry.getValue());
            double aptitude = Math.min(setup.surface().GetMultiplier(soul), setup.category().GetMultiplier(soul));
            (aptitude >= WELL_SUITED ? souls : aptitude >= SUITED ? suited : others).add(soul);
        }
        shuffle(souls, random);
        shuffle(suited, random);
        shuffle(others, random);
        // When there aren't enough of the best, the next fill up.
        souls.addAll(suited);
        souls.addAll(others);
        // The same Umamusume has several entries, one per outfit: each runs once.
        Set<ResourceLocation> taken = new HashSet<>();
        if (UmaSoulUtils.hasSoulData(except))
            taken.add(identifier(registry, except));
        souls.removeIf(soul -> !taken.add(identifier(registry, soul)));
        List<RaceOpponent> field = new ArrayList<>();
        for (int i = 0; i < stalls.size() && i < souls.size(); i++) {
            ItemStack soul = souls.get(i);
            soul.set(DataComponentsTypeRegistry.UMADATA_BASIC_STATUS.get(), new UmaDataBasicStatus(stat(stats[0], random),
                    stat(stats[1], random), stat(stats[2], random), stat(stats[3], random), stat(stats[4], random)));
            RaceStats worth = RaceStats.of(soul, RaceStyle.of(soul, level), setup.surface(), setup.category(),
                    UmapyoiConfig.RACE_AI_BOOST.get());
            RaceRunner runner = new RaceRunner(setup, worth, layout, plainSprint);
            field.add(new RaceOpponent(runner, new RaceAi(worth.style(), distance, random), soul,
                    track.inner() + 0.5 + random.nextDouble() * LANE_SPREAD, stalls.get(i), random.nextInt(6)));
        }
        return field;
    }

    static ResourceLocation identifier(Registry<UmaData> registry, ItemStack soul) {
        ResourceLocation name = UmaSoulUtils.getName(soul);
        return registry.getOptional(name).map(UmaData::identifier).orElse(name);
    }

    /** Not capped at the highest grade, so the strongest field isn't a little weak on average. */
    private static int stat(int around, RandomSource random) {
        return Math.max(1, around + random.nextInt(STAT_SPREAD * 2 + 1) - STAT_SPREAD);
    }

    private static <T> void shuffle(List<T> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--)
            Collections.swap(list, i, random.nextInt(i + 1));
    }

    /** Works out her pacing, if that wasn't done yet. */
    boolean plan(RaceSetup setup, double plainSprint, TrackGeometry track) {
        if (ai.planned())
            return false;
        ai.plan(() -> new RaceRunner(setup, runner.stats(), runner.layout(), plainSprint), track, lane);
        return true;
    }

    /** Puts her figure in her stall. */
    void appear(ServerLevel level, TrackGeometry track, BlockPos origin, double progress) {
        entity = RaceEntities.RUNNER.get().create(level);
        if (entity == null)
            return;
        entity.place(soul, track, origin);
        entity.setCustomName(name());
        entity.setCustomNameVisible(true);
        entity.show(progress, offset, 0);
        level.addFreshEntity(entity);
    }

    void disappear() {
        if (entity != null)
            entity.discard();
        entity = null;
    }

    Component name() {
        return UmaSoulUtils.getTranslatedUmaName(soul);
    }

    RaceRunner runner() {
        return runner;
    }

    ItemStack soul() {
        return soul;
    }

    /** Her stall (0 = innermost). */
    int lane(TrackGeometry track) {
        return (int) Math.round(stallOffset - track.inner() - 0.5);
    }

    boolean finished() {
        return time >= 0;
    }

    double time() {
        return time;
    }

    RaceTraffic.Body body() {
        return new RaceTraffic.Body(this, runner.progress(), offset, runner.speed());
    }

    /**
     * One tick of the race.
     *
     * @param sinceOpen ticks since the gates opened
     * @param start     progress she starts from
     * @param all       everyone still running (herself included)
     */
    void tick(TrackGeometry track, double distance, int sinceOpen, double start, List<RaceTraffic.Body> all) {
        if (sinceOpen < delay)
            return;
        if (!runner.started())
            runner.start(start);
        double before = runner.progress();
        RaceTraffic.Body me = body();
        boolean sprint = ai.sprint(runner);
        RaceStatePayload state = RaceAi.step(runner, track, offset, sprint, finished() ? RaceTraffic.FREE : RaceTraffic.limit(me, all));
        double step = runner.progress() - before;
        if (!finished() && runner.progress() >= distance && step > 0)
            time = (sinceOpen - 1 + (distance - before) / step) / 20.0;
        steer(track, sinceOpen, runner.wanted(state, sprint), me, all);
        if (entity != null && !entity.isRemoved())
            entity.show(runner.progress(), offset, runner.speed());
    }

    /** One tick of choosing a lane and moving over to it. */
    private void steer(TrackGeometry track, int tick, double wanted, RaceTraffic.Body me, List<RaceTraffic.Body> all) {
        double inner = track.inner() + 0.5;
        double outer = track.outer() - 0.5;
        List<RaceTraffic.Body> others = all.stream().filter(other -> other.who() != this).toList();

        double want;
        RaceTraffic.Body slower = me.progress() < KEEP_LANE ? null : inTheWay(me, wanted, others);
        if (me.progress() < KEEP_LANE) {
            want = stallOffset;
        } else if (slower != null) {
            boolean arrived = Math.abs(passing - offset) < 0.3 || tick > passingUntil;
            if (arrived || blockedSide(me, Math.signum(passing - offset), others))
                passing = passingLane(me, slower, others, inner, outer);
            passingUntil = tick + LINGER;
            want = passing;
        } else if (tick <= passingUntil) {
            want = passing;
        } else {
            want = lane < offset && !worthMovingIn(me, wanted, others) ? offset : lane;
        }

        double step = Mth.clamp(Mth.clamp(want, inner, outer) - offset, -SIDESTEP, SIDESTEP);
        if (step != 0 && !blockedSide(me, Math.signum(step), others))
            offset += step;
        // Never two in one place: the outer one is moved off.
        for (RaceTraffic.Body other : others)
            if (Math.abs(other.progress() - me.progress()) < OVERLAP && Math.abs(other.offset() - offset) < OVERLAP
                    && (offset > other.offset() || offset == other.offset() && stallOffset > other.offset()))
                offset = Math.min(outer, offset + SIDESTEP);
    }

    /** The nearest runner ahead in her lane whom she would catch. */
    @Nullable
    private RaceTraffic.Body inTheWay(RaceTraffic.Body me, double wanted, List<RaceTraffic.Body> others) {
        RaceTraffic.Body nearest = null;
        for (RaceTraffic.Body other : others) {
            double gap = other.progress() - me.progress();
            double faster = wanted - other.speed();
            if (gap <= 0 || gap > LOOK_AHEAD || faster < 0.05 || gap / faster > CATCH_UP_SECONDS
                    || Math.abs(other.offset() - offset) >= ACROSS)
                continue;
            if (nearest == null || other.progress() < nearest.progress())
                nearest = other;
        }
        return nearest;
    }

    /** The free lane to pass {@code slower} in that costs least; her own if there is none. */
    private double passingLane(RaceTraffic.Body me, RaceTraffic.Body slower, List<RaceTraffic.Body> others, double inner, double outer) {
        // The bunch: everyone about level with them, side by side without a gap to slip through.
        double innermost = slower.offset();
        double outermost = slower.offset();
        boolean grown = true;
        while (grown) {
            grown = false;
            for (RaceTraffic.Body other : others) {
                if (Math.abs(other.progress() - slower.progress()) > BUNCH_LENGTH)
                    continue;
                if (other.offset() < innermost && innermost - other.offset() < BUNCH_WIDTH) {
                    innermost = other.offset();
                    grown = true;
                } else if (other.offset() > outermost && other.offset() - outermost < BUNCH_WIDTH) {
                    outermost = other.offset();
                    grown = true;
                }
            }
        }
        double best = offset;
        double cost = Double.MAX_VALUE;
        double outward = runner.phase() == RacePhase.OPENING ? OUTWARD_EARLY : OUTWARD;
        for (double candidate : new double[] { innermost - 1, outermost + 1 }) {
            if (candidate < inner - 0.01 || candidate > outer || !free(me, candidate, slower.progress() + 1, others))
                continue;
            double price = Math.abs(candidate - offset) * (candidate > offset ? outward : 1);
            if (price < cost) {
                cost = price;
                best = candidate;
            }
        }
        return best;
    }

    /** Nobody in that lane from beside her up to {@code until}, and nobody beside her on the way over. */
    private boolean free(RaceTraffic.Body me, double lane, double until, List<RaceTraffic.Body> others) {
        for (RaceTraffic.Body other : others) {
            double gap = other.progress() - me.progress();
            if (gap < -BESIDE || other.progress() > until)
                continue;
            if (Math.abs(other.offset() - lane) < ACROSS - 0.1)
                return false;
            if (gap < BESIDE && other.offset() > Math.min(lane, offset) && other.offset() < Math.max(lane, offset))
                return false;
        }
        return true;
    }

    /** Someone right beside her on that side (-1 inside, 1 outside). */
    private boolean blockedSide(RaceTraffic.Body me, double side, List<RaceTraffic.Body> others) {
        for (RaceTraffic.Body other : others) {
            double across = (other.offset() - offset) * side;
            if (Math.abs(other.progress() - me.progress()) < BESIDE && across > 0 && across < ACROSS)
                return true;
        }
        return false;
    }

    /** Further in there's nobody slower just ahead to get stuck behind. */
    private boolean worthMovingIn(RaceTraffic.Body me, double wanted, List<RaceTraffic.Body> others) {
        for (RaceTraffic.Body other : others) {
            double gap = other.progress() - me.progress();
            if (gap > -BESIDE && gap < LOOK_AHEAD && other.offset() < offset && offset - other.offset() < 1 + ACROSS
                    && other.speed() < wanted + 0.1)
                return false;
        }
        return true;
    }
}
