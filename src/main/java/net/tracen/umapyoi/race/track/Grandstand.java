package net.tracen.umapyoi.race.track;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The two-tier grandstand along the home straight, as one cross-section repeated along the
 * straight. A block depends only on its row (0 = the front wall, counting away from the track),
 * its height above the ground and its place along the stand.
 * <pre>
 *  h20  ====================== roof (lights in it), front edge hangs one lower
 *                     |back   |
 *            upper  _/|rooms  |   upper tier: three seat rows up one block each, then a flat landing
 *          ___/     _/|       |
 *   __concourse       |       |   concourse: a walkway between the tiers
 *  /lower tier        |       |   lower tier: one block up per two rows (seat + floor)
 * |front wall ... tunnels under everything at ground level, every 36 blocks, closed at the back
 * </pre>
 * Seats are coloured; every 12 blocks along the stand (and beside each end wall) a white aisle
 * runs from front to back, like a cinema.
 */
final class Grandstand {
    /** Rows from the front wall to the back of the building. */
    static final int DEPTH = 27;
    /** Heights the stand uses, roof included. */
    static final int HEIGHT = 21;

    private static final int ROOF = 20;
    private static final int ROOF_FRONT = 4;
    private static final int LOWER_LAST = 10;
    private static final int CONCOURSE_LAST = 13;
    private static final int UPPER_LAST = 23;
    /** In the upper tier, every this many rows the last one is a flat landing. */
    private static final int UPPER_CYCLE = 4;
    private static final int TUNNEL_SPACING = 36;
    /** People come in on the walkway between the tiers. */
    static final int ENTRANCE_ROW = 12;
    private static final int AISLE_SPACING = 12;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WALL = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState RAILING = Blocks.GLASS_PANE.defaultBlockState();
    private static final BlockState SEAT = Blocks.MANGROVE_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.SOUTH);
    private static final BlockState AISLE_STEP = Blocks.QUARTZ_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.SOUTH);
    private static final BlockState FLOOR = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState CONCOURSE = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState WINDOW = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    private static final BlockState ROOF_SLAB = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState()
            .setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    private static final BlockState ROOF_EDGE = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();

    private static final int UPPER_TOP = seatHeight(UPPER_LAST);

    private Grandstand() {
    }

    /**
     * @param row    0 to DEPTH-1, counting back from the front wall
     * @param h      1 to HEIGHT, blocks above the ground
     * @param along  0 to length-1, from the west end
     * @param length the stand's length
     */
    static BlockState blockAt(int row, int h, int along, int length) {
        boolean end = along == 0 || along == length - 1;
        if (!end && h <= 3 && row < DEPTH - 1 && tunnel(along, length))
            return AIR;

        if (h == ROOF && row >= ROOF_FRONT) {
            boolean light = (row == 8 || row == 14 || row == 20) && along % 6 == 3;
            return light ? LIGHT : ROOF_SLAB;
        }
        if (h == ROOF - 1 && row == ROOF_FRONT)
            return ROOF_EDGE;
        if (h >= ROOF)
            return AIR;
        if (end && row >= 1)
            return WALL;

        if (row == 0)
            return h == 1 ? WALL : h == 2 ? RAILING : AIR;

        if (row > UPPER_LAST) {
            // The back building. Its back face has three floors of windows; above the upper tier
            // its front face looks onto the track.
            if (row == DEPTH - 1 && along % 4 != 0 && (h == 3 || h == 4 || h == 9 || h == 10 || h == 15 || h == 16))
                return WINDOW;
            if (row == UPPER_LAST + 1 && h > UPPER_TOP && h < ROOF - 1 && along % 3 != 0)
                return WINDOW;
            return WALL;
        }

        int seat = seatHeight(row);
        if (h < seat)
            return WALL;
        if (h > seat)
            return AIR;
        if (row <= LOWER_LAST)
            return row % 2 == 0 ? FLOOR : seatOrAisle(along, length);
        if (row <= CONCOURSE_LAST)
            return CONCOURSE;
        return landing(row) ? FLOOR : seatOrAisle(along, length);
    }

    private static BlockState seatOrAisle(int along, int length) {
        return aisle(along, length) ? AISLE_STEP : SEAT;
    }

    /** The height of the seat or floor block in a row. */
    private static int seatHeight(int row) {
        if (row <= LOWER_LAST)
            return 1 + (row - 1) / 2;
        if (row <= CONCOURSE_LAST)
            return 1 + (LOWER_LAST - 1) / 2 + 1;
        int u = row - CONCOURSE_LAST - 1;
        // Up one per row, except that each landing stays level with the seat row before it.
        return 7 + u - (u + 1) / UPPER_CYCLE;
    }

    /** How high above the ground people stand on the walkway. */
    static int entranceHeight() {
        return seatHeight(ENTRANCE_ROW) + 1;
    }

    private static boolean landing(int row) {
        return (row - CONCOURSE_LAST) % UPPER_CYCLE == 0;
    }

    /** One-block aisles from front to back: beside each end wall and every 12 blocks, halfway between tunnels. */
    private static boolean aisle(int along, int length) {
        if (along == 1 || along == length - 2)
            return true;
        return Math.floorMod(along - length / 2, AISLE_SPACING) == AISLE_SPACING / 2;
    }

    /** Three-wide passages into the stand at ground level, spaced out from the middle. The back wall ends them: there's only open country behind. */
    private static boolean tunnel(int along, int length) {
        if (along < 3 || along > length - 4)
            return false;
        int fromMiddle = Math.floorMod(along - length / 2, TUNNEL_SPACING);
        return fromMiddle <= 1 || fromMiddle >= TUNNEL_SPACING - 1;
    }

    /** True for blocks that join their neighbours (glass panes) and need a second pass. */
    static boolean connects(BlockState state) {
        return state.is(Blocks.GLASS_PANE);
    }
}
