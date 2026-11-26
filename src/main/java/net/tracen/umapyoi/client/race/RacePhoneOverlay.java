package net.tracen.umapyoi.client.race;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.client.screen.RacePhoneScreen;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.race.RaceMeeting;
import net.tracen.umapyoi.race.RaceMeetingClient;
import net.tracen.umapyoi.race.RaceMeetingPayload;
import net.tracen.umapyoi.race.RaceWorld;

/** A phone in the hand shows the course in the corner, so one can look up at the race itself. */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@EventBusSubscriber(modid = Umapyoi.MODID, value = Dist.CLIENT)
public class RacePhoneOverlay implements LayeredDraw.Layer {
    public static final RacePhoneOverlay INSTANCE = new RacePhoneOverlay();
    private static final int WIDTH = 76, HEIGHT = 180, SCREEN_X = 5, SCREEN_Y = 21, W = 66, H = 152;
    /** Room for the action bar at the edge of the screen. */
    private static final int FROM_RIGHT = 42;
    /** Its lower edge is out of the picture. */
    private static final int BELOW = 4;
    private static final int SKILL_WIDTH = 96;

    private RacePhoneOverlay() {
    }

    /** Whether the corner shows the phone. */
    private static boolean shown(Minecraft minecraft) {
        Player player = minecraft.player;
        return !minecraft.options.hideGui && player != null && RaceMeetingClient.current() != null && RaceWorld.isRaceWorld(player.level())
                && (player.getMainHandItem().is(ItemRegistry.RACE_PHONE.get()) || player.getOffhandItem().is(ItemRegistry.RACE_PHONE.get()));
    }

    /** It's in the corner already: not in the hand as well. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getItemStack().is(ItemRegistry.RACE_PHONE.get()) && shown(Minecraft.getInstance()))
            event.setCanceled(true);
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        RaceMeetingPayload held = RaceMeetingClient.current();
        if (!shown(minecraft) || player == null || held == null || minecraft.screen instanceof RacePhoneScreen)
            return;
        int x = graphics.guiWidth() - FROM_RIGHT - WIDTH;
        int bottom = graphics.guiHeight() + BELOW;
        // Not over the selected skill, nor the mood above it.
        if (UmapyoiConfig.OVERLAY_SWITCH.get() && !UmapyoiAPI.getUmaSoul(player).isEmpty()
                && x < graphics.guiWidth() / 2 + UmapyoiConfig.TOPLEFT_COORD_SKILL_X.get() + SKILL_WIDTH + 2)
            bottom = graphics.guiHeight() + Math.min(UmapyoiConfig.TOPLEFT_COORD_SKILL_Y.get(), UmapyoiConfig.TOPLEFT_COORD_MOTIVATION_Y.get()) - 2;
        int y = bottom - HEIGHT;
        graphics.blit(RaceArt.PHONE, x, y, 148, 0, WIDTH, HEIGHT, 256, 256);
        screen(graphics, minecraft, held, x + SCREEN_X, y + SCREEN_Y);
    }

    private static void screen(GuiGraphics graphics, Minecraft minecraft, RaceMeetingPayload held, int x, int y) {
        RaceMeeting.Stage stage = held.stageNow();
        boolean running = stage == RaceMeeting.Stage.RUNNING && RaceMeetingClient.watching();
        graphics.fill(x, y, x + W, y + H, RaceMap.GROUND);
        graphics.fill(x, y, x + W, y + 11, RaceArt.WHITE);
        if (running) {
            Component left = RaceMeetingClient.home() ? Component.translatable(RaceMeeting.Stage.RESULTS.key()) : Component.literal(RaceMeetingClient.metersLeft() + "m");
            RaceArt.line(graphics, left, x + 3, y + 2, RaceMeetingClient.home() ? W - 4 : 30, RaceArt.RED);
            if (!RaceMeetingClient.home())
                RaceArt.right(graphics, Component.literal(RaceMeetingClient.raceClockText()), x + W - 2, y + 2, RaceArt.DARK);
        } else if (stage == RaceMeeting.Stage.OPEN || stage == RaceMeeting.Stage.CLOSED || stage == RaceMeeting.Stage.LEAVING) {
            String clock = RaceMeetingClient.clock();
            Component what = Component.translatable(stage.key());
            if (minecraft.font.width(what) + minecraft.font.width(clock) + 8 <= W)
                RaceArt.line(graphics, what, x + 3, y + 2, W, stage == RaceMeeting.Stage.OPEN ? RaceArt.GREEN : RaceArt.RED);
            RaceArt.right(graphics, Component.literal(clock), x + W - 2, y + 2, RaceArt.DARK);
        } else {
            RaceArt.line(graphics, Component.translatable(stage.key()), x + 3, y + 2, W - 4, RaceArt.RED);
        }

        RaceMap map = RaceMap.of(held, x, y + 11, W, H - 22);
        if (map != null) {
            map.render(graphics);
            if (stage.ordinal() < RaceMeeting.Stage.RUNNING.ordinal())
                map.arrows(graphics);
            map.runners(graphics, RaceMeetingClient.spots());
        }

        graphics.fill(x, y + H - 11, x + W, y + H, RaceArt.WHITE);
        List<Integer> gates = held.announced()
                ? held.rows().stream().filter(row -> row.place() > 0).sorted((a, b) -> Integer.compare(a.place(), b.place())).map(RaceMeetingPayload.Row::gate).toList()
                : RaceMeetingClient.order();
        if (gates.isEmpty()) {
            Component what = Component.empty().append(RaceArt.surface(held)).append(" " + RaceArt.meters(held.meters()));
            RaceArt.center(graphics, what, x, W, y + H - 9, RaceArt.DARK, false);
            return;
        }
        for (int i = 0; i < Math.min(5, gates.size()); i++)
            RaceArt.chip(graphics, x + 1 + i * 13, y + H - 10, 12, 9, gates.get(i), held.rows().size(), false);
    }
}
