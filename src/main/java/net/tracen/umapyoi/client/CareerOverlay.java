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
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.tracen.umapyoi.career.event.CareerEventData;
import net.tracen.umapyoi.career.event.CareerEventType;
import net.tracen.umapyoi.client.key.SkillKeyMapping;

/**
 * Career calendar, only while the worn soul is in its three-year career: a banner with the
 * current period, and a calendar page counting the periods left (62 down to 1). A red "!" on the
 * banner means there are events the player hasn't looked at; the "show events" key fades in the
 * event list to the right of the calendar.
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
    private static final int BADGE_U = 100, BADGE_SIZE = 9;
    private static final int LIST_W = 156, ROW_H = 22;
    private static final int NAVY = 0x1F3F72, OUTLINE = 0x1968B3, SEPARATOR = 0xC3CEDC, HINT = 0x66768C,
            RED = 0xDD3C3C, HEADER_KEY = 0xD8EEFC;

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

        renderEvents(graphics, font, data, soul.get(DataComponentsTypeRegistry.CAREER_EVENTS), x, y, bannerW);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    private void renderEvents(GuiGraphics graphics, Font font, UmaCareerData career, @Nullable CareerEventData events,
            int x, int y, int bannerW) {
        List<CareerEventData.Active> active = events == null ? List.of() : events.active();
        UUID careerId = career.careerId();
        CareerEventsClient.markSeenIfOpen(careerId, active);
        float visibility = CareerEventsClient.visibility();
        if (visibility <= 0F) {
            if (CareerEventsClient.hasUnseen(careerId, active))
                graphics.blit(TEXTURE, x + bannerW - 6, y - 3, BADGE_U, 0, BADGE_SIZE, BADGE_SIZE, TEX_W, TEX_H);
            return;
        }

        int alpha = Math.max(4, Math.round(visibility * 255));
        int lx = x + bannerW + 4;
        int ly = y - Math.round((1F - visibility) * 6);
        int rows = Math.max(1, active.size());
        int height = 14 + rows * ROW_H + 2;

        // Header: the calendar's own banner, with the count and the key that closes it.
        RenderSystem.setShaderColor(1F, 1F, 1F, visibility);
        graphics.blit(TEXTURE, lx, ly, 0, 16, BANNER_CAP, BANNER_H, TEX_W, TEX_H);
        for (int bx = BANNER_CAP; bx < LIST_W - BANNER_CAP; bx += BANNER_TILE) {
            int w = Math.min(BANNER_TILE, LIST_W - BANNER_CAP - bx);
            graphics.blit(TEXTURE, lx + bx, ly, 6, 16, w, BANNER_H, TEX_W, TEX_H);
        }
        graphics.blit(TEXTURE, lx + LIST_W - BANNER_CAP, ly, 22, 16, BANNER_CAP, BANNER_H, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.drawString(font, Component.translatable("umapyoi.career.events.header", active.size()), lx + 7, ly + 3,
                argb(alpha, 0xFFFFFF), true);
        Component close = Component.translatable("umapyoi.career.events.close",
                SkillKeyMapping.KEY_CAREER_EVENTS.getTranslatedKeyMessage());
        graphics.drawString(font, close, lx + LIST_W - 6 - font.width(close), ly + 3, argb(alpha, HEADER_KEY), true);

        // Body: white paper with the calendar's blue outline.
        int top = ly + 13;
        graphics.fill(lx + 1, top, lx + LIST_W - 1, ly + height - 1, argb(alpha, 0xFFFFFF));
        graphics.fill(lx, top, lx + 1, ly + height - 1, argb(alpha, OUTLINE));
        graphics.fill(lx + LIST_W - 1, top, lx + LIST_W, ly + height - 1, argb(alpha, OUTLINE));
        graphics.fill(lx + 1, ly + height - 1, lx + LIST_W - 1, ly + height, argb(alpha, OUTLINE));

        if (active.isEmpty()) {
            graphics.drawString(font, Component.translatable("umapyoi.career.events.empty"), lx + 6, ly + 21,
                    argb(alpha, HINT), false);
            return;
        }
        int turn = career.periodIndex() - CareerManager.FIRST_PERIOD;
        for (int i = 0; i < active.size(); i++) {
            CareerEventData.Active event = active.get(i);
            CareerEventType type = CareerEventType.byId(event.id()).orElse(null);
            int ry = ly + 14 + i * ROW_H;
            if (i > 0)
                graphics.fill(lx + 3, ry, lx + LIST_W - 3, ry + 1, argb(alpha, SEPARATOR));
            if (type == null)
                continue;
            Component label = type.kind().label();
            int pillW = font.width(label) + 4;
            graphics.fill(lx + 4, ry + 3, lx + 4 + pillW, ry + 13, argb(alpha, kindColor(type.kind())));
            graphics.drawString(font, label, lx + 6, ry + 4, argb(alpha, 0xFFFFFF), false);
            int textX = lx + 4 + pillW + 4;
            graphics.drawString(font, type.displayName(), textX, ry + 4, argb(alpha, NAVY), false);
            graphics.drawString(font, trim(font, type.hint().getString(), lx + LIST_W - 4 - textX), textX, ry + 13,
                    argb(alpha, HINT), false);

            int left = Math.max(1, event.deadline() - turn);
            Component turns = Component.translatable("umapyoi.career.events.turns", left);
            int tx = lx + LIST_W - 5 - font.width(turns);
            graphics.drawString(font, turns, tx, ry + 4, argb(alpha, left <= 2 ? RED : NAVY), false);
            if (CareerEventsClient.isNew(careerId, event))
                graphics.fill(tx - 6, ry + 6, tx - 3, ry + 9, argb(alpha, RED));
        }
    }

    private static int kindColor(CareerEventType.Kind kind) {
        return switch (kind) {
            case MANDATORY -> 0xD84343;
            case OPTIONAL -> 0x2F9A78;
            case STATE -> 0x8A5CC8;
            case NOTICE -> 0xD99A1E;
        };
    }

    private static int argb(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** Cuts a line that doesn't fit, with an ellipsis. */
    private static String trim(Font font, String text, int width) {
        if (font.width(text) <= width)
            return text;
        return font.plainSubstrByWidth(text, width - font.width("…")) + "…";
    }
}
