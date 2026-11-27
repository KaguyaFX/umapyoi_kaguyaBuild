package net.tracen.umapyoi.race.track;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The grandstand along the home straight, as one cross-section repeated along the straight. A block
 * depends only on its row (0 = the front wall, counting away from the track), its height above the
 * ground and its place along the stand.
 * <pre>
 *  h30           ========================= roof (lights in it), front edge hangs one lower
 *                              upper  _/|back     |
 *                       ___/  seats _/  |building |  upper deck: rows up one block each, a flat landing every fourth
 *  h13          |2F walkway_/           |         |  2F walkway: along the upper deck's front, over the 1F one
 *  h12          ========== deck ========|         |
 *               |       |glass  1F hall (stairs up to the 2F walkway) |glass: looks out the back
 *   h6  __1F walkway____|___________________________|
 *      /lower tier                                   lower tier: one block up per two rows (seat + floor)
 *     |front wall ... tunnels under everything at ground level, every 36 blocks, closed at the back
 * </pre>
 * Seats are coloured; every 12 blocks along the stand (and beside each end wall) a white aisle runs
 * from front to back, like a cinema. In the hall's glass front a door is in line with each tunnel, and
 * beside it a staircase climbs to the 2F walkway.
 */
final class Grandstand {
    /** Rows from the front wall to the back of the building. */
    static final int DEPTH = 31;
    /** Heights the stand uses, roof included. */
    static final int HEIGHT = 31;

    private static final int ROOF = 30;
    private static final int ROOF_FRONT = 8;
    private static final int LOWER_LAST = 10;
    /** The 1F walkway, between the lower tier and the hall. */
    private static final int WALK_LAST = 14, WALK_HEIGHT = 6;
    /** The hall: glass at its front, rows of floor behind, glass in the back wall. */
    private static final int HALL_FRONT = 15;
    /** The upper deck's underside: the hall's ceiling. */
    private static final int DECK = 12;
    /** The 2F walkway along the upper deck's front, its floor height, and the upper seats behind it. */
    private static final int WALK2_FIRST = 10, WALK2_LAST = 13, WALK2_HEIGHT = 13;
    private static final int UPPER_FIRST = 14, UPPER_LAST = 25;
    /** In the upper deck, every this many rows the last one is a flat landing. */
    private static final int UPPER_CYCLE = 4;
    private static final int TUNNEL_SPACING = 36;
    /** People come in on the 1F walkway. */
    static final int ENTRANCE_ROW = 12;
    private static final int AISLE_SPACING = 12;
    /** The staircases: from this far past a tunnel's middle along the stand, in these two rows of the hall. */
    private static final int STAIR_FROM = 2, STAIR_ROW = 16;
    private static final int STEPS = DECK - WALK_HEIGHT + 1;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WALL = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState RAILING = Blocks.GLASS_PANE.defaultBlockState();
    private static final BlockState SEAT = Blocks.MANGROVE_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.SOUTH);
    private static final BlockState AISLE_STEP = Blocks.QUARTZ_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.SOUTH);
    /** Up towards the east, along the stand. */
    private static final BlockState STAIR = Blocks.QUARTZ_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.EAST);
    private static final BlockState FLOOR = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState WALKWAY = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState WINDOW = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    private static final BlockState ROOF_SLAB = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState()
            .setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    private static final BlockState ROOF_EDGE = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();

    private static final int UPPER_TOP = upperHeight(UPPER_LAST);

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
        BlockState state = structure(row, h, along, length, end);
        // A light in each tunnel's ceiling every few rows, where it has one (the front rows are open above).
        if (h == 4 && row % 4 == 0 && row < DEPTH - 1 && tunnelMiddle(along, length) && !state.isAir())
            return LIGHT;
        return state;
    }

    private static BlockState structure(int row, int h, int along, int length, boolean end) {
        if (h == ROOF && row >= ROOF_FRONT) {
            boolean light = (row == 12 || row == 18 || row == 24) && along % 6 == 3;
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

        BlockState stairs = stairs(row, h, along, length);
        if (stairs != null)
            return stairs;

        if (row >= HALL_FRONT)
            return hallAndAbove(row, h, along, length);

        // In front of the hall: the lower tier and the 1F walkway, and over the walkway the 2F one.
        if (row >= WALK2_FIRST && h >= DECK) {
            if (row <= WALK2_LAST) {
                if (h < WALK2_HEIGHT)
                    return WALL;
                if (h == WALK2_HEIGHT)
                    return WALKWAY;
                return h == WALK2_HEIGHT + 1 && row == WALK2_FIRST ? RAILING : AIR;
            }
            return upperRow(row, h, along, length);
        }
        int seat = row <= LOWER_LAST ? 1 + (row - 1) / 2 : WALK_HEIGHT;
        if (h < seat)
            return WALL;
        if (h > seat)
            return AIR;
        if (row <= LOWER_LAST)
            return row % 2 == 0 ? FLOOR : seatOrAisle(along, length);
        return WALKWAY;
    }

    /** The hall, and over it the upper deck and the back building. */
    private static BlockState hallAndAbove(int row, int h, int along, int length) {
        if (h < WALK_HEIGHT)
            return WALL;
        if (h == WALK_HEIGHT)
            return row == DEPTH - 1 ? WALL : WALKWAY;
        if (h < DECK) {
            if (row == HALL_FRONT)
                return tunnel(along, length) ? AIR : WINDOW;
            if (row == DEPTH - 1)
                return along % 4 != 0 ? WINDOW : WALL;
            return AIR;
        }
        if (h == DECK && (row == 19 || row == 23 || row == 27) && along % 6 == 3)
            return LIGHT;
        if (row > UPPER_LAST) {
            // The back building. Its back face has more floors of windows; above the upper deck its
            // front face looks onto the track.
            if (row == DEPTH - 1 && along % 4 != 0 && (h == 15 || h == 16 || h == 21 || h == 22 || h == 26 || h == 27))
                return WINDOW;
            if (row == UPPER_LAST + 1 && h > UPPER_TOP && h < ROOF - 1 && along % 3 != 0)
                return WINDOW;
            return WALL;
        }
        return upperRow(row, h, along, length);
    }

    /** A row of the upper deck, solid from the deck up to its seat. */
    private static BlockState upperRow(int row, int h, int along, int length) {
        int seat = upperHeight(row);
        if (h < seat)
            return WALL;
        if (h > seat)
            return AIR;
        return landing(row) ? FLOOR : seatOrAisle(along, length);
    }

    private static BlockState seatOrAisle(int along, int length) {
        return aisle(along, length) ? AISLE_STEP : SEAT;
    }

    /**
     * The staircase by each tunnel: steps in two rows of the hall rising east along the stand, then a
     * landing at the 2F walkway's height that cuts through the first upper rows to reach it. Null where
     * the stairs don't change anything.
     */
    @Nullable
    private static BlockState stairs(int row, int h, int along, int length) {
        Integer from = stairStart(along, length);
        if (from == null)
            return null;
        int step = along - from;
        boolean stairRows = row == STAIR_ROW || row == STAIR_ROW + 1;
        if (step < STEPS) {
            if (!stairRows || h <= WALK_HEIGHT)
                return null;
            int stepHeight = WALK_HEIGHT + 1 + step;
            if (h < stepHeight)
                return WALL;
            if (h == stepHeight)
                return STAIR;
            return h <= stepHeight + 3 ? AIR : null;
        }
        if (step < STEPS + 2) {
            // the landing, from the stairs forward to the 2F walkway
            if (row < WALK2_LAST || row > STAIR_ROW + 1 || h < WALK2_HEIGHT)
                return null;
            if (h == WALK2_HEIGHT)
                return WALKWAY;
            return h <= WALK2_HEIGHT + 3 ? AIR : null;
        }
        return null;
    }

    /** Where along the stand the staircase that covers this place starts, if one does. */
    @Nullable
    private static Integer stairStart(int along, int length) {
        for (int middle : tunnels(length)) {
            int from = middle + STAIR_FROM;
            if (along >= from && along < from + STEPS + 2 && from + STEPS + 2 < length - 1)
                return from;
        }
        return null;
    }

    /** The height of the seat or floor block in an upper row. */
    private static int upperHeight(int row) {
        int u = row - UPPER_FIRST;
        // Up one per row, except that each landing stays level with the seat row before it.
        return WALK2_HEIGHT + 1 + u - (u + 1) / UPPER_CYCLE;
    }

    /** How high above the ground people stand on the 1F walkway. */
    static int entranceHeight() {
        return WALK_HEIGHT + 1;
    }

    private static boolean landing(int row) {
        return (row - WALK2_LAST) % UPPER_CYCLE == 0;
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

    /** The middle of a tunnel. */
    private static boolean tunnelMiddle(int along, int length) {
        return along >= 4 && along <= length - 5 && Math.floorMod(along - length / 2, TUNNEL_SPACING) == 0;
    }

    /** Where the tunnels are along a stand of this length: their middles. */
    static List<Integer> tunnels(int length) {
        List<Integer> tunnels = new ArrayList<>();
        for (int along = 0; along < length; along++)
            if (tunnelMiddle(along, length))
                tunnels.add(along);
        return tunnels;
    }

    /** True for blocks that join their neighbours (glass panes) and need a second pass. */
    static boolean connects(BlockState state) {
        return state.is(Blocks.GLASS_PANE);
    }
}
