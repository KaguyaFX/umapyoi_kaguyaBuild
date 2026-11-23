package net.tracen.umapyoi.race.track;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * The things placed one by one after the ground is laid: the goal post and distance poles, lamp
 * posts on the apron, lily pads, and trees around the outside and in the infield. Random choices
 * are seeded from the position, so the same spot always gets the same course.
 */
final class TrackScenery {
    /** Poles stand this far inside the surface's inner edge. */
    private static final double POLE_INSET = 2.5;
    private static final int LAMP_SPACING = 12;
    /** Rows in front of the stand's front wall. */
    private static final double LAMP_FROM_STAND = 2;
    private static final double TREE_SPACING = 8;
    /** Outside trees stand this far beyond the surface's outer edge, plus up to 3; infield trees at least this far inside. */
    private static final double TREE_RING = 7;
    private static final int INFIELD_TREES = 26;
    private static final double INFIELD_TREE_GAP = 7;

    private static final BlockState POLE_WHITE = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState POLE_RED = Blocks.RED_CONCRETE.defaultBlockState();
    private static final BlockState LAMP_POST = Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState();
    private static final BlockState LAMP = Blocks.LANTERN.defaultBlockState();
    private static final BlockState LILY_PAD = Blocks.LILY_PAD.defaultBlockState();

    private TrackScenery() {
    }

    static void decorate(ServerLevel level, CourseLayout layout, double poleSpacing, List<BlockPos> connect) {
        RandomSource random = RandomSource.create(layout.origin().asLong());
        poles(level, layout, poleSpacing);
        lamps(level, layout, connect);
        lilyPads(level, layout, random);
        outsideTrees(level, layout, random);
        infieldTrees(level, layout, random);
    }

    private static void poles(ServerLevel level, CourseLayout layout, double poleSpacing) {
        TrackGeometry track = layout.track();
        double offset = layout.track().inner() - POLE_INSET;
        pole(level, layout, track.pointAt(0, offset), true);
        for (double p = track.lap() - poleSpacing; p > poleSpacing / 2; p -= poleSpacing)
            pole(level, layout, track.pointAt(p, offset), false);
    }

    private static void pole(ServerLevel level, CourseLayout layout, TrackGeometry.Point point, boolean goal) {
        int i = Mth.floor(point.x());
        int k = Mth.floor(point.z());
        int height = goal ? 5 : 3;
        for (int h = 1; h <= height; h++) {
            BlockState state;
            if (goal)
                state = h == height ? POLE_RED : POLE_WHITE;
            else
                state = h % 2 == 1 ? POLE_RED : POLE_WHITE;
            TrackBuilder.place(level, layout.world(i, h, k), state);
        }
    }

    /** A row of lamp posts along the apron, in front of the stand. */
    private static void lamps(ServerLevel level, CourseLayout layout, List<BlockPos> connect) {
        int k = Mth.floor(layout.track().radius() + layout.standStart() - LAMP_FROM_STAND);
        for (int along = LAMP_SPACING / 2; along < layout.standLength(); along += LAMP_SPACING) {
            int i = layout.standMinX() + along;
            for (int h = 1; h <= 3; h++) {
                BlockPos pos = layout.world(i, h, k);
                TrackBuilder.place(level, pos, LAMP_POST);
                connect.add(pos);
            }
            TrackBuilder.place(level, layout.world(i, 4, k), LAMP);
        }
    }

    private static void lilyPads(ServerLevel level, CourseLayout layout, RandomSource random) {
        double a = layout.track().straight() / 2;
        double b = layout.track().radius();
        for (int n = 0; n < 40; n++) {
            double x = (random.nextDouble() * 2 - 1) * a;
            double z = (random.nextDouble() * 2 - 1) * b;
            if (layout.pondNorm(x, z) < 0.8)
                TrackBuilder.place(level, layout.world(Mth.floor(x), 1, Mth.floor(z)), LILY_PAD);
        }
    }

    /** A loose ring of trees outside the course, except on the stand's side of the home straight. */
    private static void outsideTrees(ServerLevel level, CourseLayout layout, RandomSource random) {
        TrackGeometry track = layout.track();
        for (double p = 0; p < track.lap(); p += TREE_SPACING) {
            double along = p + (random.nextDouble() * 2 - 1) * 3;
            double offset = track.outer() + TREE_RING + random.nextDouble() * 3;
            TrackGeometry.Point point = track.pointAt(along, offset);
            if (point.z() > 0 && Math.abs(point.x()) <= track.straight() / 2 + 4)
                continue;
            tree(level, layout.world(Mth.floor(point.x()), 1, Mth.floor(point.z())), random);
        }
    }

    /** Trees scattered over the infield, clear of the pond, the poles and each other. */
    private static void infieldTrees(ServerLevel level, CourseLayout layout, RandomSource random) {
        TrackGeometry track = layout.track();
        double halfX = track.straight() / 2 + track.radius();
        double halfZ = track.radius();
        double inside = track.inner() - TREE_RING;
        List<double[]> placed = new ArrayList<>();
        for (int attempt = 0; attempt < 600 && placed.size() < INFIELD_TREES; attempt++) {
            double x = (random.nextDouble() * 2 - 1) * halfX;
            double z = (random.nextDouble() * 2 - 1) * halfZ;
            if (track.locate(x, z).offset() > inside || layout.pondNorm(x, z) < 1.5)
                continue;
            boolean crowded = false;
            for (double[] other : placed)
                if (Math.hypot(other[0] - x, other[1] - z) < INFIELD_TREE_GAP)
                    crowded = true;
            if (crowded)
                continue;
            placed.add(new double[] { x, z });
            tree(level, layout.world(Mth.floor(x), 1, Mth.floor(z)), random);
        }
    }

    /** Oak, birch or cherry, grown the way saplings grow them. */
    private static void tree(ServerLevel level, BlockPos pos, RandomSource random) {
        float roll = random.nextFloat();
        ResourceKey<ConfiguredFeature<?, ?>> kind = roll < 0.5F ? TreeFeatures.OAK
                : roll < 0.8F ? TreeFeatures.BIRCH : TreeFeatures.CHERRY;
        level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(kind)
                .ifPresent(feature -> feature.value().place(level, level.getChunkSource().getGenerator(), random, pos));
    }
}
