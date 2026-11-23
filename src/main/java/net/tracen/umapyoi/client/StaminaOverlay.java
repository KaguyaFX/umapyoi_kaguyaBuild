package net.tracen.umapyoi.client;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.race.RaceClientState;

/**
 * The race stamina bar: the action point bar's look, mirrored to the left side of the screen,
 * shown only while racing. Green above the low line, orange below it, red near empty; a tick marks
 * the low line, where the speed bonus drops.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StaminaOverlay implements LayeredDraw.Layer {
    public static final StaminaOverlay INSTANCE = new StaminaOverlay();
    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "textures/gui/stamina_bar.png");
    private static final int HEIGHT = 128;

    private final Minecraft minecraft = Minecraft.getInstance();

    private StaminaOverlay() {
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker tracker) {
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator() || !RaceClientState.active())
            return;
        float max = RaceClientState.maxStamina();
        if (max <= 0)
            return;
        float fraction = Math.max(0, Math.min(1, RaceClientState.stamina() / max));
        int x = 8;
        int y = graphics.guiHeight() - 64;

        graphics.blit(HUD, x, y - HEIGHT, 11, 0, 5, HEIGHT, 16, HEIGHT);
        int bar = Math.round(fraction * HEIGHT);
        double low = UmapyoiConfig.RACE_LOW_STAMINA.get();
        if (fraction >= low)
            graphics.setColor(0.45F, 1.0F, 0.45F, 1.0F);
        else if (fraction >= 0.15F)
            graphics.setColor(1.0F, 0.65F, 0.2F, 1.0F);
        else
            graphics.setColor(1.0F, 0.3F, 0.3F, 1.0F);
        graphics.blit(HUD, x, y - bar, 6, HEIGHT - bar, 5, bar, 16, HEIGHT);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int lowY = y - (int) Math.round(low * HEIGHT);
        graphics.fill(x - 1, lowY, x + 6, lowY + 1, 0xCCFFFFFF);

        String text = String.valueOf(Math.round(fraction * 100)) + "%";
        graphics.drawString(minecraft.font, text, x + 8, y - 3 - bar, 0xFFFFFF);

        // Speedometer under the bar: ground speed over the last tick, in race km/h.
        var player = minecraft.player;
        double blocksPerSecond = Math.hypot(player.getX() - player.xo, player.getZ() - player.zo) * 20;
        double kmh = blocksPerSecond * UmapyoiConfig.RACE_METERS_PER_BLOCK.get() * 3.6;
        graphics.drawString(minecraft.font, String.format("%.0f km/h", kmh), x - 4, y + 4, 0xFFFFFF);
    }
}
