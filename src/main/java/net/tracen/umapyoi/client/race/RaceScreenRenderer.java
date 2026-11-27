package net.tracen.umapyoi.client.race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.utils.ClientUtils;
import net.tracen.umapyoi.race.RaceBetClient;
import net.tracen.umapyoi.race.RaceLivePayload.Spot;
import net.tracen.umapyoi.race.RaceMeeting;
import net.tracen.umapyoi.race.RaceMeetingClient;
import net.tracen.umapyoi.race.RaceMeetingPayload;
import net.tracen.umapyoi.race.RaceWorld;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.race.track.TrackScreens;
import net.tracen.umapyoi.registry.races.Race;
import net.tracen.umapyoi.utils.Surface;

/**
 * Draws the big screen and the results board in the infield ({@link TrackScreens}) from the race being
 * held, on the frames the course has for them: before the start the field and the win odds, during the
 * race the course with everyone on it and the running order (and the finish, until the result is
 * announced), after it the result and what the bets paid. The board beside it lights up with the first
 * five and the margins once the result is in.
 * <p>
 * Both are painted like a GUI into a texture of their own, four texels to a screen pixel with the fourth
 * row and column darker so that it reads as LEDs, and that texture is put on one quad for each, full
 * bright, eight screen pixels to a block. Mipmapped, so that the LEDs blur together further away instead
 * of shimmering.
 */
@EventBusSubscriber(modid = Umapyoi.MODID, value = Dist.CLIENT)
public final class RaceScreenRenderer {
    private static final int W = TrackScreens.SCREEN_WIDTH * TrackScreens.PIXELS;
    private static final int H = TrackScreens.HEIGHT * TrackScreens.PIXELS;
    private static final int BW = TrackScreens.BOARD_WIDTH * TrackScreens.PIXELS;
    /** Texels to a screen pixel. */
    private static final int TEXELS = 4;
    /** Seen from further away than this nothing is drawn. */
    private static final double SEEN_WITHIN = 320;
    /** How far in front of the black faces the pictures are, to stay clear of them however far away. */
    private static final double PROUD = 0.25;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_screen");

    private static final int NAVY = 0xFF101826, NAVY2 = 0xFF1A2638, PAPER = 0xFF07090C, PAPER2 = 0xFF0F141B, GOLD = 0xFFD8B04A;
    private static final int WHITE = 0xFFF2F2F2, AMBER = 0xFFFFAE1A, RED = 0xFFE2342B, GREY = 0xFF7A8494, LINE = 0xFF2A3340;
    private static final int TURF = 0xFF2F8A3A, DIRT = 0xFF9A7A4A, RAIL = 0xFFC8C2AF, POND = 0xFF1F4F86;
    private static final int LIGHT = LightTexture.FULL_BRIGHT;

    @Nullable
    private static TextureTarget target;
    /** The 50 ms step it was last painted in; it is painted at most that often. */
    private static long paintedIn = -1;
    /** Whether there's a picture to put up this frame. */
    private static boolean ready;

    private RaceScreenRenderer() {
    }

    /** Where the screens are, if they're up and near enough to be seen. */
    private record Where(RaceMeetingPayload held, TrackScreens.Place place, BlockPos origin) {
    }

    @Nullable
    private static Where where(Minecraft minecraft, Vec3 camera) {
        RaceMeetingPayload held = RaceMeetingClient.current();
        if (minecraft.level == null || held == null || !RaceWorld.isRaceWorld(minecraft.level))
            return null;
        RaceMeetingPayload.Course course = held.course();
        TrackScreens.Place place = course.radius() <= 0 ? null : TrackScreens.place(course.straight(), course.radius());
        if (place == null)
            return null;
        BlockPos origin = BlockPos.of(course.origin());
        double middle = origin.getX() + place.screenX() + (TrackScreens.SCREEN_WIDTH + TrackScreens.GAP + TrackScreens.BOARD_WIDTH) / 2.0;
        if (camera.distanceTo(new Vec3(middle, origin.getY() + TrackScreens.BOTTOM + TrackScreens.HEIGHT / 2.0, origin.getZ() + place.faceZ())) > SEEN_WITHIN)
            return null;
        return new Where(held, place, origin);
    }

    // ---- painting the texture, before the frame is drawn

    @SubscribeEvent
    public static void paint(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        Where where = where(minecraft, minecraft.gameRenderer.getMainCamera().getPosition());
        if (where == null) {
            ready = false;
            return;
        }
        long step = Util.getMillis() / 50;
        if (ready && step == paintedIn)
            return;
        paintedIn = step;
        if (target == null) {
            target = new TextureTarget((W + BW) * TEXELS, H * TEXELS, false, Minecraft.ON_OSX);
            minecraft.getTextureManager().register(TEXTURE, new Picture());
        }
        target.setClearColor(0, 0, 0, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        VertexSorting sorting = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, target.width, target.height, 0, 1000, 3000), VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.translation(0, 0, -2000);
        RenderSystem.applyModelViewMatrix();
        try {
            GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            paintAll(new Painter(graphics, minecraft.font), where.held());
            graphics.flush();
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            minecraft.getMainRenderTarget().bindWrite(true);
        }
        GlStateManager._bindTexture(target.getColorTextureId());
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GlStateManager._bindTexture(0);
        ready = true;
    }

    private static void paintAll(Painter paint, RaceMeetingPayload held) {
        PoseStack pose = paint.graphics.pose();
        pose.pushPose();
        pose.scale(TEXELS, TEXELS, 1);
        RaceMeeting.Stage stage = held.stageNow();
        // The result is only known once it's announced; until then the finish stays on the screen.
        boolean result = stage == RaceMeeting.Stage.LEAVING;
        if (result)
            after(paint, held);
        else if (stage == RaceMeeting.Stage.RUNNING || stage == RaceMeeting.Stage.RESULTS)
            during(paint, held);
        else
            before(paint, held);
        pose.translate(W, 0, 0);
        board(paint, held, result);
        pose.popPose();
        paint.leds(0, W, 0x47000000);
        paint.leds(W, BW, 0xAA000000);
    }

    // ---- putting it up, with the blocks

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        // Before the water and glass, and late enough that the camera's turn is already applied.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !ready)
            return;
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        Where where = where(minecraft, camera);
        if (where == null)
            return;
        TrackScreens.Place place = where.place();
        BlockPos origin = where.origin();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(origin.getX() - camera.x, origin.getY() - camera.y, origin.getZ() + place.faceZ() + PROUD - camera.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer to = buffers.getBuffer(RenderType.text(TEXTURE));
        float split = (float) W / (W + BW);
        quad(to, m, place.screenX(), TrackScreens.SCREEN_WIDTH, 0, split);
        quad(to, m, place.boardX(), TrackScreens.BOARD_WIDTH, split, 1);
        buffers.endBatch(RenderType.text(TEXTURE));
        pose.popPose();
    }

    /** Facing the stand; the texture's top row is its last (it was drawn into a framebuffer). */
    private static void quad(VertexConsumer to, Matrix4f m, int x, int width, float u0, float u1) {
        float bottom = TrackScreens.BOTTOM, top = TrackScreens.BOTTOM + TrackScreens.HEIGHT;
        to.addVertex(m, x, bottom, 0).setColor(-1).setUv(u0, 0).setLight(LIGHT);
        to.addVertex(m, x + width, bottom, 0).setColor(-1).setUv(u1, 0).setLight(LIGHT);
        to.addVertex(m, x + width, top, 0).setColor(-1).setUv(u1, 1).setLight(LIGHT);
        to.addVertex(m, x, top, 0).setColor(-1).setUv(u0, 1).setLight(LIGHT);
    }

    /** The painted framebuffer, as a texture the game can bind by name: sharp up close, mipmapped further away. */
    private static final class Picture extends AbstractTexture {
        @Override
        public void load(ResourceManager manager) {
        }

        @Override
        public int getId() {
            return target == null ? 0 : target.getColorTextureId();
        }

        @Override
        public void setFilter(boolean blur, boolean mipmap) {
            this.bind();
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }

        @Override
        public void releaseId() {
            // The framebuffer owns it.
        }
    }

    // ---- the big screen

    private static void header(Painter paint, RaceMeetingPayload held, Component right) {
        paint.rect(0, 0, W, 22, NAVY);
        paint.rect(0, 22, W, 2, GOLD);
        Race race = ClientUtils.getRaceRegistry().get(held.race());
        int x = 4;
        if (race != null) {
            Component tier = RaceArt.tier(race.ranking());
            int w = paint.width(tier) + 8;
            paint.rect(4, 4, w, 14, RaceArt.tierColor(race.ranking()));
            paint.text(tier, 8, 7, WHITE, false);
            x += w + 6;
        }
        int rightWidth = paint.width(right) * 2;
        paint.text(paint.cut(RaceArt.raceName(held.race()), (W - x - rightWidth - 12) / 2), x, 3, WHITE, true);
        paint.text(right, W - 6 - rightWidth, 3, WHITE, true);
    }

    private static List<RaceMeetingPayload.Row> runners(RaceMeetingPayload held) {
        List<RaceMeetingPayload.Row> rows = new ArrayList<>(held.rows());
        rows.sort(Comparator.comparingInt(RaceMeetingPayload.Row::gate));
        return rows;
    }

    /** Win odds as boards show them; from 100 on without the tenths, to leave the name room. */
    private static String odds(double odds) {
        return odds <= 0 ? "--" : odds >= 100 ? String.valueOf((int) odds) : String.format("%.1f", odds);
    }

    private static void before(Painter paint, RaceMeetingPayload held) {
        paint.rect(0, 0, W, H, PAPER);
        Component clock = held.stageNow() == RaceMeeting.Stage.CLOSED && RaceMeetingClient.left() == 0
                ? Component.translatable("gui.umapyoi.meeting.gate")
                : Component.translatable("umapyoi.screen.off", RaceMeetingClient.clock());
        header(paint, held, clock);
        List<RaceMeetingPayload.Row> rows = runners(held);
        int field = rows.size();
        int columns = field > 12 ? 3 : 2;
        int perColumn = (field + columns - 1) / columns;
        int colW = W / columns;
        int rowH = Math.min(18, (H - 36) / Math.max(1, perColumn));
        boolean wide = columns == 2;
        int oddsRight = wide ? 168 : colW - 4;
        paint.rect(0, 24, W, 12, NAVY2);
        for (int c = 0; c < columns; c++) {
            int x0 = c * colW;
            paint.text(Component.translatable("umapyoi.screen.number"), x0 + 4, 26, WHITE, false);
            paint.text(Component.translatable("umapyoi.screen.name"), x0 + 34, 26, WHITE, false);
            paint.right(Component.translatable("umapyoi.screen.win"), x0 + oddsRight, 26, WHITE);
            if (wide)
                paint.text(Component.translatable("umapyoi.screen.favourite"), x0 + 172, 26, WHITE, false);
            if (c > 0)
                paint.rect(x0 - 1, 24, 1, H - 24, LINE);
        }
        for (int i = 0; i < field; i++) {
            RaceMeetingPayload.Row row = rows.get(i);
            int x = (i / perColumn) * colW, y = 36 + (i % perColumn) * rowH;
            paint.rect(x, y, colW - (i / perColumn < columns - 1 ? 1 : 0), rowH, i % perColumn % 2 == 0 ? PAPER : PAPER2);
            paint.rect(x, y, 4, rowH, RaceArt.bracketColor(row.gate(), field));
            paint.chip(x + 6, y + (rowH - 14) / 2, 22, 14, row.gate(), field, false);
            int favourite = RaceBetClient.favourite(row.gate());
            Component shown = Component.literal(row.scratched() ? "--" : odds(RaceBetClient.odds(row.gate(), false, 0)));
            int oddsLeft = x + oddsRight - paint.width(shown) * 2;
            paint.text(shown, oddsLeft, y + (rowH - 16) / 2, favourite >= 1 && favourite <= 3 ? AMBER : WHITE, true);
            // Three columns leave too little room for names in the big letters.
            int room = oddsLeft - 4 - (x + 34);
            int nameColor = row.scratched() ? GREY : WHITE;
            if (wide)
                paint.text(paint.cut(RaceArt.name(row), room / 2), x + 34, y + (rowH - 16) / 2, nameColor, true);
            else
                paint.text(paint.cut(RaceArt.name(row), room), x + 34, y + (rowH - 8) / 2, nameColor, false);
            if (wide && favourite > 0)
                paint.text(Component.literal(String.valueOf(favourite)), x + 176, y + (rowH - 8) / 2, GREY, false);
        }
    }

    private static void during(Painter paint, RaceMeetingPayload held) {
        paint.rect(0, 0, W, H, PAPER);
        TrackGeometry track = held.track();
        if (track != null) {
            double half = track.straight() / 2 + track.radius() + track.outer();
            double tall = track.radius() + track.outer();
            // As if seen from the stand, a little from above: as wide as the screen, squashed to fit its height.
            double sx = (W - 24) / (2 * half), sz = Math.min(sx, 84 / (2 * tall));
            double cx = W / 2.0, cy = 72;
            double lap = track.lap();
            int band = Math.max(2, (int) Math.round(track.width() * sz));
            int turf = held.surfaceNow() == Surface.DIRT ? DIRT : TURF;
            paint.rect((int) Math.round(cx - track.straight() / 2 * 0.62 * sx), (int) Math.round(cy - track.radius() * 0.35 * sz),
                    (int) Math.round(track.straight() * 0.62 * sx), (int) Math.round(track.radius() * 0.7 * sz), POND);
            paint.graphics.drawManaged(() -> {
                for (int k = 0; k < 720; k++) {
                    TrackGeometry.Point outer = track.pointAt(lap * k / 720, track.outer());
                    paint.rect((int) Math.round(cx + outer.x() * sx) - 1, (int) Math.round(cy + outer.z() * sz) - 1, 2, 2, RAIL);
                }
            });
            paint.graphics.drawManaged(() -> {
                for (int k = 0; k < 720; k++) {
                    TrackGeometry.Point middle = track.pointAt(lap * k / 720, track.middle());
                    paint.rect((int) Math.round(cx + middle.x() * sx) - band / 2, (int) Math.round(cy + middle.z() * sz) - band / 2, band, band, turf);
                }
            });
            TrackGeometry.Point line = track.pointAt(track.wrap(held.course().distance()), track.middle());
            paint.rect((int) Math.round(cx + line.x() * sx), (int) Math.round(cy + line.z() * sz) - band, 1, band * 2, WHITE);
            List<Spot> spots = new ArrayList<>(RaceMeetingClient.spots());
            spots.sort(Comparator.comparingDouble(Spot::progress));
            int field = held.rows().size();
            for (Spot spot : spots) {
                TrackGeometry.Point at = track.pointAt(track.wrap(spot.progress()), spot.offset());
                paint.chip((int) Math.round(cx + at.x() * sx) - 5, (int) Math.round(cy + at.z() * sz) - 5, 10, 9, spot.gate(), field, true);
            }
        }
        // what a broadcast lays over the pictures
        Race race = ClientUtils.getRaceRegistry().get(held.race());
        int x = 4;
        Component name = paint.cut(RaceArt.raceName(held.race()), 64);
        int nameW = paint.width(name) * 2;
        if (race != null) {
            Component tier = RaceArt.tier(race.ranking());
            int w = paint.width(tier) + 8;
            paint.rect(4, 4, w, 18, RaceArt.tierColor(race.ranking()));
            paint.text(tier, 8, 9, WHITE, false);
            x += w;
        }
        paint.rect(x, 4, nameW + 8, 18, 0xDC101826);
        paint.text(name, x + 4, 5, WHITE, true);
        if (held.stageNow() == RaceMeeting.Stage.RESULTS) {
            Component waiting = Component.translatable("umapyoi.meeting.stage.results");
            int boxW = paint.width(waiting) * 2 + 8;
            paint.rect(W - 4 - boxW, 4, boxW, 18, 0xD2101010);
            paint.text(waiting, W - boxW, 5, AMBER, true);
        } else {
            Component left = Component.translatable("umapyoi.screen.left", RaceMeetingClient.metersLeft() + "m");
            Component clock = Component.literal(RaceMeetingClient.raceClockText());
            int boxW = paint.width(left) * 2 + paint.width(clock) * 2 + 20;
            paint.rect(W - 4 - boxW, 4, boxW, 18, 0xD2101010);
            paint.text(left, W - boxW, 5, AMBER, true);
            paint.text(clock, W - 8 - paint.width(clock) * 2, 5, WHITE, true);
        }
        // the running order along the bottom, the gaps as on the course
        paint.rect(0, 118, W, 26, 0xE10C1424);
        List<Integer> order = RaceMeetingClient.order();
        int field = held.rows().size();
        if (!order.isEmpty()) {
            List<Double> progress = new ArrayList<>();
            for (int gate : order)
                progress.add(RaceMeetingClient.spots().stream().filter(s -> s.gate() == gate).mapToDouble(Spot::progress).findFirst().orElse(0));
            double spare = W - 12 - order.size() * 26.0, gaps = 0;
            for (int k = 1; k < order.size(); k++)
                gaps += Math.max(0, progress.get(k - 1) - progress.get(k));
            double perBlock = gaps <= 0 ? 0 : Math.min(3, Math.max(0, spare) / gaps);
            double at = 6;
            for (int k = 0; k < order.size(); k++) {
                if (k > 0)
                    at += 2 + Math.max(0, progress.get(k - 1) - progress.get(k)) * perBlock;
                paint.chip((int) Math.round(at), 122, 24, 18, order.get(k), field, false);
                at += 24;
            }
        }
    }

    /** The winner's time as boards give it, to the tenth. */
    private static String time(float seconds) {
        int tenths = (int) Math.floor(seconds * 10 + 1e-4);
        return String.format("%d:%02d.%d", tenths / 600, tenths / 10 % 60, tenths % 10);
    }

    private static void after(Painter paint, RaceMeetingPayload held) {
        paint.rect(0, 0, W, H, PAPER);
        List<RaceMeetingPayload.Row> placed = placed(held);
        Component time = placed.isEmpty() ? Component.empty() : Component.literal(time(placed.get(0).time()));
        header(paint, held, time);
        Component confirmed = Component.translatable("umapyoi.screen.confirmed");
        int cw = paint.width(confirmed) * 2 + 8;
        paint.rect(W - 18 - paint.width(time) * 2 - cw, 4, cw, 15, RED);
        paint.text(confirmed, W - 14 - paint.width(time) * 2 - cw, 3, WHITE, true);
        int field = held.rows().size();
        for (int k = 0; k < Math.min(3, placed.size()); k++) {
            RaceMeetingPayload.Row row = placed.get(k);
            int y = 30 + k * 20;
            paint.text(Component.translatable("umapyoi.screen.placed", k + 1), 8, y, AMBER, true);
            paint.chip(44, y, 24, 17, row.gate(), field, false);
            paint.text(paint.cut(RaceArt.name(row), 64), 74, y, WHITE, true);
        }
        if (!RaceBetClient.odds().ready() || placed.isEmpty())
            return;
        paint.rect(212, 28, 1, 64, LINE);
        paint.text(Component.translatable("umapyoi.screen.win"), 222, 30, GREY, false);
        RaceMeetingPayload.Row winner = placed.get(0);
        paint.chip(222, 42, 22, 16, winner.gate(), field, false);
        paint.text(Component.literal(pays(RaceBetClient.odds(winner.gate(), false, 0))), 252, 42, AMBER, true);
        paint.text(Component.translatable("umapyoi.screen.per"), 222, 64, GREY, false);
        int places = RaceBetClient.odds().places();
        if (places <= 0)
            return;
        paint.rect(0, 96, W, 12, NAVY2);
        paint.text(Component.translatable("umapyoi.screen.place"), 6, 98, WHITE, false);
        int x = 6;
        for (int k = 0; k < Math.min(places, placed.size()); k++) {
            RaceMeetingPayload.Row row = placed.get(k);
            paint.chip(x, 112, 22, 16, row.gate(), field, false);
            paint.text(Component.literal(pays(RaceBetClient.odds(row.gate(), true, 0))), x + 28, 112, WHITE, true);
            x += 120;
        }
    }

    private static String pays(double odds) {
        return odds <= 0 ? "--" : String.valueOf(Math.round(odds * 100));
    }

    private static List<RaceMeetingPayload.Row> placed(RaceMeetingPayload held) {
        List<RaceMeetingPayload.Row> rows = new ArrayList<>();
        for (RaceMeetingPayload.Row row : held.rows())
            if (row.place() > 0)
                rows.add(row);
        rows.sort(Comparator.comparingInt(RaceMeetingPayload.Row::place));
        return rows;
    }

    // ---- the results board

    private static void board(Painter paint, RaceMeetingPayload held, boolean lit) {
        paint.rect(0, 0, BW, H, 0xFF050607);
        Component confirmed = Component.translatable("umapyoi.screen.confirmed"), inquiry = Component.translatable("umapyoi.screen.inquiry");
        paint.rect(4, 4, 42, 16, lit ? RED : 0xFF2A1210);
        paint.text(confirmed, 25 - paint.width(confirmed), 4, lit ? WHITE : 0xFF5A2A26, true);
        paint.rect(50, 4, 42, 16, 0xFF2A1D08);
        paint.text(inquiry, 71 - paint.width(inquiry), 4, 0xFF5A4A20, true);
        List<RaceMeetingPayload.Row> placed = lit ? placed(held) : List.of();
        int field = held.rows().size();
        double speed = placed.isEmpty() || placed.get(0).time() <= 0 ? 0 : held.meters() / placed.get(0).time();
        for (int k = 0; k < 5; k++) {
            int y = 26 + k * 20;
            boolean known = k < placed.size();
            paint.text(Component.literal(String.valueOf(k + 1)), 6, y, known ? AMBER : 0xFF3A2A10, true);
            if (known) {
                RaceMeetingPayload.Row row = placed.get(k);
                paint.chip(22, y, 26, 17, row.gate(), field, false);
                if (k > 0 && speed > 0)
                    paint.text(RaceArt.margin(RaceArt.lengths(row.time() - placed.get(k - 1).time(), speed)), 54, y - 6, AMBER, false);
            } else {
                paint.rect(22, y, 26, 17, 0xFF121418);
            }
        }
        paint.rect(0, 126, BW, 1, 0xFF1C1F24);
        paint.text(Component.translatable("umapyoi.screen.time"), 4, 131, 0xFF8A6A20, false);
        Component time = Component.literal(placed.isEmpty() ? "-:--.-" : time(placed.get(0).time()));
        paint.text(time, BW - 4 - paint.width(time) * 2, 128, placed.isEmpty() ? 0xFF3A2A10 : AMBER, true);
    }

    /** Paints in screen pixels, each thing over the last, in the order it's asked to. */
    private static final class Painter {
        private final GuiGraphics graphics;
        private final Font font;

        Painter(GuiGraphics graphics, Font font) {
            this.graphics = graphics;
            this.font = font;
        }

        void rect(int x, int y, int w, int h, int argb) {
            graphics.fill(x, y, x + w, y + h, argb);
        }

        int width(Component text) {
            return font.width(text);
        }

        /** The text cut to fit this many of its own pixels. */
        Component cut(Component text, int width) {
            String s = text.getString();
            return font.width(s) <= width ? text : Component.literal(font.plainSubstrByWidth(s, Math.max(0, width - font.width("…"))) + "…");
        }

        void text(Component text, int x, int y, int argb, boolean big) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            if (big)
                pose.scale(2, 2, 1);
            graphics.drawString(font, text, 0, 0, argb, false);
            pose.popPose();
        }

        void right(Component text, int right, int y, int argb) {
            text(text, right - width(text), y, argb, false);
        }

        /** A runner's number on her bracket's colour: doubled when it fits. */
        void chip(int x, int y, int w, int h, int gate, int field, boolean small) {
            rect(x, y, w, h, RaceArt.bracketColor(gate, field));
            Component number = Component.literal(String.valueOf(gate));
            int color = RaceArt.bracketTextColor(gate, field);
            boolean big = !small && h >= 16 && width(number) * 2 <= w - 2;
            if (big)
                text(number, x + (w - width(number) * 2) / 2 + 1, y + (h - 16) / 2 + 1, color, true);
            else
                text(number, x + (w - width(number)) / 2 + 1, y + (h - 8) / 2 + 1, color, false);
        }

        /**
         * Every screen pixel an LED: the last texel row and column of each darker. In texels, from
         * {@code from} screen pixels across for {@code width}.
         */
        void leds(int from, int width, int gap) {
            graphics.drawManaged(() -> {
                for (int i = 1; i <= width; i++)
                    graphics.fill((from + i) * TEXELS - 1, 0, (from + i) * TEXELS, H * TEXELS, gap);
                for (int j = 1; j <= H; j++)
                    graphics.fill(from * TEXELS, j * TEXELS - 1, (from + width) * TEXELS, j * TEXELS, gap);
            });
        }
    }
}
