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
 * off the inside she runs; nobody bumps into anybody, but she steps out around runners in her way.
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
    /** Room a runner takes up: along the course (behind, ahead) and across it. */
    private static final double BESIDE = 1.0;
    private static final double AHEAD = 2.5;
    private static final double ACROSS = 0.95;

    private final RaceRunner runner;
    private final RaceAi ai;
    private final ItemStack soul;
    private final double lane;
    private final double stallOffset;
    private final int delay;
    private double offset;
    @Nullable
    private RunnerEntity entity;
    /** Seconds since the gates opened; below zero while still running. */
    private double time = -1;

    /** Someone to step around. */
    record Other(double progress, double offset) {
    }

    private RaceOpponent(RaceRunner runner, RaceAi ai, ItemStack soul, double lane, double stallOffset, int delay) {
        this.runner = runner;
        this.ai = ai;
        this.soul = soul;
        this.lane = lane;
        this.stallOffset = stallOffset;
        this.offset = stallOffset;
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

    private static ResourceLocation identifier(Registry<UmaData> registry, ItemStack soul) {
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

    boolean finished() {
        return time >= 0;
    }

    double time() {
        return time;
    }

    Other place() {
        return new Other(runner.progress(), offset);
    }

    /**
     * One tick of the race.
     *
     * @param sinceOpen ticks since the gates opened
     * @param start     progress she starts from
     */
    void tick(TrackGeometry track, double distance, int sinceOpen, double start, List<Other> others) {
        if (sinceOpen < delay)
            return;
        if (!runner.started())
            runner.start(start);
        double before = runner.progress();
        RaceAi.step(runner, track, offset, ai.sprint(runner));
        double step = runner.progress() - before;
        if (!finished() && runner.progress() >= distance && step > 0)
            time = (sinceOpen - 1 + (distance - before) / step) / 20.0;
        steer(track, others);
        if (entity != null && !entity.isRemoved())
            entity.show(runner.progress(), offset, runner.speed());
    }

    /** Over to the lane she likes, around whoever is level with her or just ahead there. */
    private void steer(TrackGeometry track, List<Other> others) {
        double want = runner.progress() < KEEP_LANE ? stallOffset : lane;
        List<Other> near = new ArrayList<>();
        for (Other other : others) {
            double ahead = other.progress() - runner.progress();
            // Exactly level can only be told apart by who is further in; the one outside gives way.
            if (ahead > 0 ? ahead < AHEAD : ahead > -BESIDE && other.offset() < offset)
                near.add(other);
        }
        near.sort((a, b) -> Double.compare(a.offset(), b.offset()));
        for (Other other : near)
            if (Math.abs(other.offset() - want) < ACROSS)
                want = other.offset() + ACROSS;
        want = Mth.clamp(want, track.inner() + 0.5, track.outer() - 0.5);
        offset += Mth.clamp(want - offset, -SIDESTEP, SIDESTEP);
    }
}
