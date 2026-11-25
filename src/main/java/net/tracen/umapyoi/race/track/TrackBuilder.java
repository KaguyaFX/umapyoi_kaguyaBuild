package net.tracen.umapyoi.race.track;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.utils.Surface;

/**
 * Builds a racecourse into a level with vanilla blocks, a slice of columns per tick: striped turf,
 * rails on both sides, a hedge behind the outer rail (flowers by the finish), the apron and the
 * two-tier {@link Grandstand} along the home straight, and a pond in the infield. Then
 * {@link TrackScenery} adds poles, lamps and trees, and a last pass joins fences and panes.
 * It overwrites whatever is there, so it only runs in the race world.
 * <p>
 * {@link #dress} repaints an already built course for one race: the running surface (turf, dirt or
 * synthetic), the finish line, the goal post and distance poles, and the flowers by the finish. It
 * also turns a right-handed course into a left-handed one and back, because the course is symmetric;
 * only those things move. The stand, pond and trees stay.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class TrackBuilder {
    /** Ground kept around the outer rail, room for the trees. */
    private static final int MARGIN = 12;
    /** Blocks cleared above the ground: enough to clear a stand left by an earlier, differently sized course. */
    private static final int CLEAR_HEIGHT = Grandstand.HEIGHT + 2;
    /** Length of each turf stripe along the course. */
    private static final double STRIPE = 4;
    /** Flowers replace the hedge this far either side of the finish line. */
    private static final double FLOWER_SPAN = 15;
    private static final double POND_RIM = 1.15;
    /** Send to clients, skip neighbour shape updates (everything is placed explicitly). */
    public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    /** The two stripes of each surface. */
    private static final BlockState[] TURF = { Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState() };
    private static final BlockState[] DIRT = { Blocks.COARSE_DIRT.defaultBlockState(), Blocks.ROOTED_DIRT.defaultBlockState() };
    private static final BlockState[] SYNTHETIC = { Blocks.BROWN_TERRACOTTA.defaultBlockState(), Blocks.BLACK_TERRACOTTA.defaultBlockState() };
    private static final BlockState GROUND = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState SUBSOIL = Blocks.DIRT.defaultBlockState();
    private static final BlockState RAIL = Blocks.BIRCH_FENCE.defaultBlockState();
    private static final BlockState HEDGE = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState FINISH = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState APRON = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState[] FLOWERS = {
            Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(),
            Blocks.PINK_TULIP.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
            Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.RED_TULIP.defaultBlockState(),
            Blocks.ALLIUM.defaultBlockState(), Blocks.ORANGE_TULIP.defaultBlockState() };

    /** Work spread over ticks; each gets a budget of columns per tick. */
    private interface Work {
        /** @return true when finished */
        boolean step(int budget);
    }

    private static final List<Work> JOBS = new ArrayList<>();

    /** @param sizeX blocks along X @param sizeZ blocks along Z @param view where to stand to see the home straight */
    public record Result(int sizeX, int sizeZ, TrackGeometry.Point view, long millis) {
    }

    private TrackBuilder() {
    }

    public static boolean busy() {
        return !JOBS.isEmpty();
    }

    /**
     * @param origin      the infield's middle, at ground height
     * @param previous    a course built at the same origin before, whose area is cleared too; or null
     * @param poleSpacing blocks between distance poles, counted back from the finish
     * @param onProgress  told every 10% of the ground pass
     * @param onDone      told when everything is placed
     */
    public static void start(ServerLevel level, BlockPos origin, TrackGeometry track, @Nullable TrackGeometry previous,
            double poleSpacing, IntConsumer onProgress, Consumer<Result> onDone) {
        JOBS.add(new Job(level, new CourseLayout(track, origin), previous, poleSpacing, onProgress, onDone));
    }

    /**
     * Repaints the running surface, hedge flowers, finish line, goal post and distance poles of the
     * course already there (right- or left-handed as {@code track} says) for a race.
     *
     * @param track       the course, as it should be after dressing
     * @param surface     what it is run on
     * @param poleSpacing blocks between distance poles, counted back from the finish
     * @param onProgress  told every 10%
     * @param onDone      told when the course is ready
     */
    public static void dress(ServerLevel level, BlockPos origin, TrackGeometry track, Surface surface, double poleSpacing,
            IntConsumer onProgress, Runnable onDone) {
        JOBS.add(new DressJob(level, new CourseLayout(track, origin), surface, poleSpacing, onProgress, onDone));
    }

    /** How high above the ground (the origin's height) people stand at the stand's entrance. */
    public static final int STAND_ENTRANCE_HEIGHT = Grandstand.entranceHeight();

    /** Beyond the outer rail people may go this far (where there's no stand). */
    private static final double GROUNDS = 12;

    /** Whether a place (in the course's own coordinates) is on the racecourse's grounds: not behind the stand, nor far out. */
    public static boolean onGrounds(TrackGeometry track, double x, double z) {
        CourseLayout layout = new CourseLayout(track, BlockPos.ZERO);
        double limit = layout.besideStand(Mth.floor(x), z) ? layout.standStart() + Grandstand.DEPTH : track.outer() + GROUNDS;
        return track.locate(x, z).offset() < limit;
    }

    /** Where people come into the stand, facing the course; {@code aside} blocks from its middle. */
    public static TrackGeometry.Point standEntrance(TrackGeometry track, int aside) {
        CourseLayout layout = new CourseLayout(track, BlockPos.ZERO);
        double x = (layout.standMinX() + layout.standMaxX()) / 2.0 + aside;
        return new TrackGeometry.Point(x, track.radius() + layout.standStart() + Grandstand.ENTRANCE_ROW + 0.5, 180);
    }

    /** The area a course covers, in blocks around its origin: min X, max X (exclusive), min Z, max Z (exclusive). */
    private static int[] bounds(TrackGeometry track) {
        CourseLayout layout = new CourseLayout(track, BlockPos.ZERO);
        return new int[] {
                Mth.floor(-track.halfLengthX(MARGIN)),
                Mth.ceil(track.halfLengthX(MARGIN)),
                Mth.floor(-track.halfLengthZ(MARGIN)),
                Math.max(Mth.ceil(track.halfLengthZ(MARGIN)),
                        Mth.ceil(track.radius() + layout.standStart() + Grandstand.DEPTH + 3)) };
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty())
            return;
        int budget = UmapyoiConfig.TRACK_BUILD_COLUMNS_PER_TICK.get();
        // A copy: what is done with one job may start the next.
        for (Work job : List.copyOf(JOBS))
            if (job.step(budget))
                JOBS.remove(job);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
    }

    /** Places a block unless it's already there. */
    static void place(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos) != state)
            level.setBlock(pos, state, FLAGS);
    }

    private static final class Job implements Work {
        private final ServerLevel level;
        private final CourseLayout layout;
        private final TrackGeometry track;
        private final double poleSpacing;
        private final IntConsumer onProgress;
        private final Consumer<Result> onDone;
        /** The new course's area, for the result. */
        private final int[] own;
        /** The area to go over: the new course's plus the old one's. */
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private final long startNanos = System.nanoTime();
        /** Fences, panes and walls: they join their neighbours only once those exist. */
        private final List<BlockPos> connect = new ArrayList<>();
        private int i;
        private int k;
        private int phase;
        private int lastTenth;

        Job(ServerLevel level, CourseLayout layout, @Nullable TrackGeometry previous, double poleSpacing,
                IntConsumer onProgress, Consumer<Result> onDone) {
            this.level = level;
            this.layout = layout;
            this.track = layout.track();
            this.poleSpacing = poleSpacing;
            this.onProgress = onProgress;
            this.onDone = onDone;
            this.own = bounds(track);
            int[] old = previous == null ? own : bounds(previous);
            this.minX = Math.min(own[0], old[0]);
            this.maxX = Math.max(own[1], old[1]);
            this.minZ = Math.min(own[2], old[2]);
            this.maxZ = Math.max(own[3], old[3]);
            this.i = minX;
            this.k = minZ;
        }

        @Override
        public boolean step(int budget) {
            switch (phase) {
                case 0 -> {
                    for (int done = 0; done < budget && i < maxX; done++) {
                        column(i, k);
                        if (++k >= maxZ) {
                            k = minZ;
                            i++;
                        }
                    }
                    int tenth = 10 * (i - minX) / Math.max(1, maxX - minX);
                    if (tenth != lastTenth) {
                        lastTenth = tenth;
                        onProgress.accept(tenth * 10);
                    }
                    if (i >= maxX)
                        phase = 1;
                    return false;
                }
                case 1 -> {
                    TrackScenery.decorate(level, layout, poleSpacing, connect);
                    phase = 2;
                    return false;
                }
                default -> {
                    for (BlockPos pos : connect)
                        level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), FLAGS);
                    TrackGeometry.Point view = track.pointAt(track.homeStraightStart() + 2, track.middle());
                    onDone.accept(new Result(own[1] - own[0], own[3] - own[2], view, (System.nanoTime() - startNanos) / 1_000_000));
                    return true;
                }
            }
        }

        private void column(int i, int k) {
            double x = i + 0.5;
            double z = k + 0.5;
            TrackGeometry.Spot spot = track.locate(x, z);
            double d = spot.offset();
            boolean byStand = layout.besideStand(i, z);

            if (byStand) {
                int row = Mth.floor(d - layout.standStart());
                if (row >= 0 && row < Grandstand.DEPTH) {
                    stand(i, k, row, i - layout.standMinX());
                    return;
                }
            }

            BlockState surface = GROUND;
            BlockState above = null;
            if (track.onSurface(d)) {
                surface = surfaceBlock(track, spot, Surface.TURF);
            } else if (isRail(track, i, k, d)) {
                above = RAIL;
            } else if (isHedge(track, i, k, d)) {
                above = hedgeBlock(track, spot, i, k);
            } else if (byStand && d >= track.outer()) {
                surface = APRON;
            } else if (d < track.inner() - 3) {
                double norm = layout.pondNorm(x, z);
                if (norm < 1) {
                    pond(i, k);
                    return;
                }
                if (norm < POND_RIM)
                    surface = SAND;
            }

            place(level, layout.world(i, -1, k), SUBSOIL);
            place(level, layout.world(i, 0, k), surface);
            for (int h = 1; h <= CLEAR_HEIGHT; h++)
                place(level, layout.world(i, h, k), h == 1 && above != null ? above : AIR);
            if (above == RAIL)
                connect.add(layout.world(i, 1, k));
        }

        private void stand(int i, int k, int row, int along) {
            place(level, layout.world(i, -1, k), SUBSOIL);
            place(level, layout.world(i, 0, k), APRON);
            for (int h = 1; h <= Grandstand.HEIGHT + 2; h++) {
                BlockState state = h <= Grandstand.HEIGHT ? Grandstand.blockAt(row, h, along, layout.standLength()) : AIR;
                BlockPos pos = layout.world(i, h, k);
                place(level, pos, state);
                if (Grandstand.connects(state))
                    connect.add(pos);
            }
        }

        private void pond(int i, int k) {
            place(level, layout.world(i, -2, k), SAND);
            place(level, layout.world(i, -1, k), WATER);
            place(level, layout.world(i, 0, k), WATER);
            for (int h = 1; h <= CLEAR_HEIGHT; h++)
                place(level, layout.world(i, h, k), AIR);
        }
    }

    private static boolean onTrack(TrackGeometry track, int i, int k) {
        return track.onSurface(track.locate(i + 0.5, k + 0.5).offset());
    }

    /** Off the surface with surface among its 8 neighbours: using all 8 keeps the rail 4-connected, no diagonal gaps. */
    private static boolean isRail(TrackGeometry track, int i, int k, double d) {
        double beyond = track.beyondSurface(d);
        if (beyond <= 0 || beyond >= 2)
            return false;
        for (int di = -1; di <= 1; di++)
            for (int dk = -1; dk <= 1; dk++)
                if ((di != 0 || dk != 0) && onTrack(track, i + di, k + dk))
                    return true;
        return false;
    }

    /** The ring just outside the outer rail, built the same way. */
    private static boolean isHedge(TrackGeometry track, int i, int k, double d) {
        if (d < track.outer() || d >= track.outer() + 4)
            return false;
        for (int di = -1; di <= 1; di++)
            for (int dk = -1; dk <= 1; dk++) {
                if (di == 0 && dk == 0)
                    continue;
                double nd = track.locate(i + di + 0.5, k + dk + 0.5).offset();
                if (nd >= track.outer() && isRail(track, i + di, k + dk, nd))
                    return true;
            }
        return false;
    }

    /** The running surface: two alternating stripes of the race's surface, and the finish line across it. */
    private static BlockState surfaceBlock(TrackGeometry track, TrackGeometry.Spot spot, Surface surface) {
        if (spot.progress() >= track.lap() - 1)
            return FINISH;
        BlockState[] stripes = switch (surface) {
            case DIRT -> DIRT;
            case SYNTHETIC -> SYNTHETIC;
            default -> TURF;
        };
        return stripes[((int) (spot.progress() / STRIPE)) % 2];
    }

    /** The hedge behind the outer rail, with flowers by the finish. */
    private static BlockState hedgeBlock(TrackGeometry track, TrackGeometry.Spot spot, int i, int k) {
        double p = spot.progress();
        return p < FLOWER_SPAN || p > track.lap() - FLOWER_SPAN ? FLOWERS[Math.floorMod(i * 7 + k * 3, FLOWERS.length)] : HEDGE;
    }

    /** Repaints the running surface, hedge, finish line, goal post and poles of a course for one race. */
    private static final class DressJob implements Work {
        private final ServerLevel level;
        private final CourseLayout layout;
        private final TrackGeometry track;
        private final Surface surface;
        private final double poleSpacing;
        private final IntConsumer onProgress;
        private final Runnable onDone;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private int i;
        private int k;
        private int lastTenth;

        DressJob(ServerLevel level, CourseLayout layout, Surface surface, double poleSpacing, IntConsumer onProgress, Runnable onDone) {
            this.level = level;
            this.layout = layout;
            this.track = layout.track();
            this.surface = surface;
            this.poleSpacing = poleSpacing;
            this.onProgress = onProgress;
            this.onDone = onDone;
            int[] own = bounds(track);
            this.minX = own[0];
            this.maxX = own[1];
            this.minZ = own[2];
            this.maxZ = own[3];
            this.i = minX;
            this.k = minZ;
        }

        @Override
        public boolean step(int budget) {
            for (int done = 0; done < budget && i < maxX; done++) {
                column(i, k);
                if (++k >= maxZ) {
                    k = minZ;
                    i++;
                }
            }
            int tenth = 10 * (i - minX) / Math.max(1, maxX - minX);
            if (tenth != lastTenth) {
                lastTenth = tenth;
                onProgress.accept(tenth * 10);
            }
            if (i < maxX)
                return false;
            TrackScenery.clearPoles(level, new CourseLayout(track.withMirrored(false), layout.origin()), poleSpacing);
            TrackScenery.clearPoles(level, new CourseLayout(track.withMirrored(true), layout.origin()), poleSpacing);
            TrackScenery.poles(level, layout, poleSpacing);
            onDone.run();
            return true;
        }

        private void column(int i, int k) {
            TrackGeometry.Spot spot = track.locate(i + 0.5, k + 0.5);
            double d = spot.offset();
            if (track.onSurface(d))
                place(level, layout.world(i, 0, k), surfaceBlock(track, spot, surface));
            else if (!isRail(track, i, k, d) && isHedge(track, i, k, d))
                place(level, layout.world(i, 1, k), hedgeBlock(track, spot, i, k));
        }
    }
}
