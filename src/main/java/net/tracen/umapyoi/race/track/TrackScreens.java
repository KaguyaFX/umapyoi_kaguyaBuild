package net.tracen.umapyoi.race.track;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The big screen and the results board beside it, in the infield between the pond and the back
 * straight, facing the stand across the course. Only their frames are blocks: what they show is drawn
 * by the client, from the race being held (client/race/RaceScreenRenderer).
 * <p>
 * The screen is 48 x 18 blocks, the board 12 x 18, two blocks apart, inside a dark frame that stands a
 * block proud of them, on short legs. Eight screen pixels to a block.
 */
public final class TrackScreens {
    public static final int PIXELS = 8;
    public static final int SCREEN_WIDTH = 48, BOARD_WIDTH = 12, HEIGHT = 18, GAP = 2;
    /** Height of the screens' lower edge above the ground. */
    public static final int BOTTOM = 4;

    private static final BlockState FACE = Blocks.BLACK_CONCRETE.defaultBlockState();
    private static final BlockState FRAME = Blocks.POLISHED_DEEPSLATE.defaultBlockState();

    private TrackScreens() {
    }

    /**
     * Where they are, in the course's own coordinates (x along the straights, z towards the stand).
     *
     * @param faceZ   the screens' face is the stand side of the blocks at {@code faceZ - 1}
     * @param screenX the screen's left edge, seen from the stand
     * @param boardX  the board's
     */
    public record Place(int faceZ, int screenX, int boardX) {
    }

    /** Null if the infield is too small for them. */
    @Nullable
    public static Place place(double straight, double radius) {
        double infield = radius - TrackGeometry.MEASURE_FROM_INNER;
        int total = SCREEN_WIDTH + GAP + BOARD_WIDTH;
        if (straight < total + 8 || infield < 30)
            return null;
        int left = -total / 2;
        return new Place(-(int) Math.round(infield * 0.68), left, left + SCREEN_WIDTH + GAP);
    }

    /** The frames, faces and legs. */
    static void build(ServerLevel level, CourseLayout layout) {
        TrackGeometry track = layout.track();
        Place place = place(track.straight(), track.radius());
        if (place == null)
            return;
        int from = place.screenX() - 1, to = place.boardX() + BOARD_WIDTH;
        int k = place.faceZ() - 1;
        for (int i = from; i <= to; i++)
            for (int h = BOTTOM - 1; h <= BOTTOM + HEIGHT; h++) {
                boolean edge = i == from || i == to || h == BOTTOM - 1 || h == BOTTOM + HEIGHT
                        || i >= place.screenX() + SCREEN_WIDTH && i < place.boardX();
                TrackBuilder.place(level, layout.world(i, h, k), edge ? FRAME : FACE);
                // The frame stands a block proud of the faces.
                TrackBuilder.place(level, layout.world(i, h, k + 1), edge ? FRAME : Blocks.AIR.defaultBlockState());
            }
        for (int i : new int[] { from + 4, place.screenX() + SCREEN_WIDTH / 2, place.screenX() + SCREEN_WIDTH - 4, to - 4 })
            for (int h = 1; h < BOTTOM - 1; h++)
                TrackBuilder.place(level, layout.world(i, h, k), FRAME);
    }
}
