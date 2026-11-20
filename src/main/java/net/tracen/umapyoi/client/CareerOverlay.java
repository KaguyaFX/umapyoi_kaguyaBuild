package net.tracen.umapyoi.client;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.career.UmaCareerData;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;

/**
 * Career calendar, only while the worn soul is in its three-year career: a banner with the
 * current period, and a calendar page counting the periods left (62 down to 1).
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CareerOverlay implements LayeredDraw.Layer {
    public static final CareerOverlay INSTANCE = new CareerOverlay();
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/gui/career_calendar.png");
    private static final int TEX_W = 128, TEX_H = 64;
    private static final int DIGIT_W = 8, DIGIT_H = 12, DIGIT_GAP = 2;
    private static final int BANNER_H = 14, BANNER_CAP = 6, BANNER_TILE = 16, BANNER_MIN_W = 44;
    private static final int PAGE_W = 32, PAGE_H = 44;
    private static final int FOOTER_COLOR = 0x1F3F72;

    private final Minecraft minecraft = Minecraft.getInstance();

    private CareerOverlay() {
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker tracker) {
        if (minecraft.options.hideGui || !UmapyoiConfig.OVERLAY_SWITCH.get())
            return;
        Player player = minecraft.player;
        if (player == null || player.isSpectator())
            return;
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (data == null || !CareerManager.isInCareer(soul))
            return;

        int period = data.periodIndex();
        int left = CareerManager.FIRST_PERIOD + CareerManager.PERIODS - period;
        int x = UmapyoiConfig.TOPLEFT_COORD_CAREER_X.get();
        int y = UmapyoiConfig.TOPLEFT_COORD_CAREER_Y.get();
        Font font = minecraft.font;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Banner: left cap, tiled middle, right cap; sized to the period name.
        Component name = CareerManager.periodName(period);
        int bannerW = Math.max(BANNER_MIN_W, font.width(name) + 14);
        graphics.blit(TEXTURE, x, y, 0, 16, BANNER_CAP, BANNER_H, TEX_W, TEX_H);
        for (int bx = BANNER_CAP; bx < bannerW - BANNER_CAP; bx += BANNER_TILE) {
            int w = Math.min(BANNER_TILE, bannerW - BANNER_CAP - bx);
            graphics.blit(TEXTURE, x + bx, y, 6, 16, w, BANNER_H, TEX_W, TEX_H);
        }
        graphics.blit(TEXTURE, x + bannerW - BANNER_CAP, y, 22, 16, BANNER_CAP, BANNER_H, TEX_W, TEX_H);
        graphics.drawString(font, name, x + 7, y + 3, 0xFFFFFF, true);

        // Calendar page hanging from the banner; its rings are drawn over the banner.
        int cx = x + 3;
        int cy = y + 10;
        graphics.blit(TEXTURE, cx, cy, 32, 16, PAGE_W, PAGE_H, TEX_W, TEX_H);
        Component header = Component.translatable("umapyoi.career.calendar.header");
        graphics.drawString(font, header, cx + (PAGE_W - font.width(header)) / 2, cy + 6, 0xFFFFFF, false);

        String digits = String.valueOf(left);
        int digitsW = digits.length() * DIGIT_W + (digits.length() - 1) * DIGIT_GAP;
        int dx = cx + (PAGE_W - digitsW) / 2;
        for (char c : digits.toCharArray()) {
            graphics.blit(TEXTURE, dx, cy + 18, (c - '0') * 10, 0, DIGIT_W, DIGIT_H, TEX_W, TEX_H);
            dx += DIGIT_W + DIGIT_GAP;
        }

        Component footer = Component.translatable("umapyoi.career.calendar.footer");
        graphics.drawString(font, footer, cx + (PAGE_W - font.width(footer)) / 2, cy + 31, FOOTER_COLOR, false);

        RenderSystem.disableBlend();
    }
}
