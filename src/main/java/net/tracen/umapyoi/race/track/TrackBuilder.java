package net.tracen.umapyoi.race.track;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a {@link TrackGeometry} into a level with vanilla blocks: striped turf, white rails on
 * both sides, a finish line, a goal post and a pole every 200 m before the finish. A prototype to
 * look at; it overwrites whatever is there and runs in one go.
 */
public final class TrackBuilder {
    /** Ground kept around the rails. */
    private static final int MARGIN = 6;
    /** Blocks cleared above the ground. */
    private static final int CLEAR_HEIGHT = 12;
    /** Length of each turf stripe along the course. */
    private static final double STRIPE = 4;
    /** Poles stand this far inside the running surface's inner edge. */
    private static final double POLE_INSET = 2.5;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final BlockState TURF = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState TURF_STRIPE = Blocks.MOSS_BLOCK.defaultBlockState();
    private static final BlockState GROUND = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState SUBSOIL = Blocks.DIRT.defaultBlockState();
    private static final BlockState RAIL = Blocks.BIRCH_FENCE.defaultBlockState();
    private static final BlockState FINISH = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState POLE_WHITE = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState POLE_RED = Blocks.RED_CONCRETE.defaultBlockState();

    /** @param sizeX blocks along X @param sizeZ blocks along Z @param view where to stand to see the home straight */
    public record Result(int sizeX, int sizeZ, TrackGeometry.Point view) {
    }

    private TrackBuilder() {
    }

    /**
     * @param origin      the infield's middle, at ground height
     * @param poleSpacing blocks between distance poles, counted back from the finish
     */
    public static Result build(ServerLevel level, BlockPos origin, TrackGeometry track, double poleSpacing) {
        int minX = Mth.floor(-track.halfLengthX(MARGIN));
        int maxX = Mth.ceil(track.halfLengthX(MARGIN));
        int minZ = Mth.floor(-track.halfLengthZ(MARGIN));
        int maxZ = Mth.ceil(track.halfLengthZ(MARGIN));
        int groundY = origin.getY();
        double lap = track.lap();
        List<BlockPos> rails = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = minX; i < maxX; i++) {
            for (int k = minZ; k < maxZ; k++) {
                int x = origin.getX() + i;
                int z = origin.getZ() + k;
                TrackGeometry.Spot spot = track.locate(i + 0.5, k + 0.5);
                boolean onTrack = onTrack(track, i, k);

                BlockState surface = GROUND;
                if (onTrack) {
                    // The finish line is the last block before progress wraps to 0.
                    if (spot.progress() >= lap - 1)
                        surface = FINISH;
                    else
                        surface = ((int) (spot.progress() / STRIPE)) % 2 == 0 ? TURF : TURF_STRIPE;
                }
                level.setBlock(pos.set(x, groundY - 1, z), SUBSOIL, FLAGS);
                level.setBlock(pos.set(x, groundY, z), surface, FLAGS);

                for (int y = groundY + 1; y <= groundY + CLEAR_HEIGHT; y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir())
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                }

                if (!onTrack && Math.abs(spot.offset()) < track.width() / 2 + 2 && besideTrack(track, i, k)) {
                    BlockPos railPos = new BlockPos(x, groundY + 1, z);
                    level.setBlock(railPos, RAIL, FLAGS);
                    rails.add(railPos);
                }
            }
        }

        // Fences only join up once all their neighbours exist.
        for (BlockPos railPos : rails)
            level.setBlock(railPos, Block.updateFromNeighbourShapes(RAIL, level, railPos), FLAGS);

        double poleOffset = -(track.width() / 2 + POLE_INSET);
        placePole(level, origin, track.pointAt(0, poleOffset), true);
        for (double p = lap - poleSpacing; p > poleSpacing / 2; p -= poleSpacing)
            placePole(level, origin, track.pointAt(p, poleOffset), false);

        // Stand at the start of the home straight, looking down it toward the finish.
        TrackGeometry.Point view = track.pointAt(lap - track.straight() + 2, 0);
        return new Result(maxX - minX, maxZ - minZ, view);
    }

    private static boolean onTrack(TrackGeometry track, int i, int k) {
        return Math.abs(track.locate(i + 0.5, k + 0.5).offset()) < track.width() / 2;
    }

    /** Any of the 8 neighbours is running surface. Using all 8 keeps each rail a 4-connected line, so the fence has no diagonal gaps. */
    private static boolean besideTrack(TrackGeometry track, int i, int k) {
        for (int di = -1; di <= 1; di++)
            for (int dk = -1; dk <= 1; dk++)
                if ((di != 0 || dk != 0) && onTrack(track, i + di, k + dk))
                    return true;
        return false;
    }

    private static void placePole(ServerLevel level, BlockPos origin, TrackGeometry.Point point, boolean goal) {
        int x = origin.getX() + Mth.floor(point.x());
        int z = origin.getZ() + Mth.floor(point.z());
        int height = goal ? 5 : 3;
        for (int h = 1; h <= height; h++) {
            BlockState state;
            if (goal)
                state = h == height ? POLE_RED : POLE_WHITE;
            else
                state = h % 2 == 1 ? POLE_RED : POLE_WHITE;
            level.setBlock(new BlockPos(x, origin.getY() + h, z), state, FLAGS);
        }
    }
}
