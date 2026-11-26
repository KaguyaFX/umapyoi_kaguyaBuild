package net.tracen.umapyoi.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.race.RaceClientState;
import net.tracen.umapyoi.race.RaceSkillPayload;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.UmaSoulUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SkillOverlay implements LayeredDraw.Layer {
    public static final SkillOverlay INSTANCE = new SkillOverlay();
    private final Minecraft minecraft = Minecraft.getInstance();

    private SkillOverlay() {
    }

    public static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "textures/gui/skill_hud.png");
    /** The same, greyed: a skill that can't be used right now (in a race). */
    public static final ResourceLocation HUD_DIM = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "textures/gui/skill_hud_dim.png");
    /** The frame burning, 12 frames of 112 x 36 one under the other; the plain frame sits at (8, 12) in each. */
    public static final ResourceLocation BURNING = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "textures/gui/skill_frame_burning.png");
    private static final int BURN_FRAMES = 12, BURN_W = 112, BURN_H = 36;
    /** The fire dies down over the last second. */
    private static final int FADE_TICKS = 20;
    private static final int TEXT = 0x794016, TEXT_DIM = 0x40342B;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (minecraft.options.hideGui || !UmapyoiConfig.OVERLAY_SWITCH.get())
            return;
        int x = guiGraphics.guiWidth() / 2;
        int y = guiGraphics.guiHeight();

        Player player = minecraft.player;
        if (player.isSpectator())
            return;

        if (!UmapyoiAPI.getUmaSoul(player).isEmpty()) {
            int fx = x + UmapyoiConfig.TOPLEFT_COORD_SKILL_X.get();
            int fy = y + UmapyoiConfig.TOPLEFT_COORD_SKILL_Y.get();
            // In a race: greyed when it can't be used now, burning while it is the one running.
            RaceSkillPayload race = RaceClientState.skill();
            boolean dim = race != null && race.status() == RaceSkillPayload.NOT_NOW;
            if (race != null && race.status() == RaceSkillPayload.RUNNING)
                renderBurning(guiGraphics, fx, fy, Math.min(1F, race.left() / (float) FADE_TICKS));
            else
                guiGraphics.blit(dim ? HUD_DIM : HUD, fx, fy, 0, 0, 96, 20, 128, 64);
            UmaSkill skill = UmaSkillRegistry.REGISTRY.get(UmaSoulUtils.getSelectedSkill(UmapyoiAPI.getUmaSoul(player)));
            renderSkill(skill, minecraft.font, guiGraphics, fx, fy, dim);
        }
    }

    /** The frame on fire; it dies down to the plain frame as the skill runs out. */
    private static void renderBurning(GuiGraphics guiGraphics, int x, int y, float strength) {
        if (strength < 1)
            guiGraphics.blit(HUD, x, y, 0, 0, 96, 20, 128, 64);
        int frame = (int) (Util.getMillis() / 100 % BURN_FRAMES);
        RenderSystem.enableBlend();
        guiGraphics.setColor(1F, 1F, 1F, strength);
        guiGraphics.blit(BURNING, x - 8, y - 12, 0, frame * BURN_H, BURN_W, BURN_H, BURN_W, BURN_H * BURN_FRAMES);
        guiGraphics.setColor(1F, 1F, 1F, 1F);
    }

    public static void renderSkill(UmaSkill skill, Font font, GuiGraphics guiGraphics, int x, int y) {
        renderSkill(skill, font, guiGraphics, x, y, false);
    }

    /** The skill's icon and name on its frame; greyed if it can't be used now. */
    public static void renderSkill(UmaSkill skill, Font font, GuiGraphics guiGraphics, int x, int y, boolean dim) {
        ResourceLocation sheet = dim ? HUD_DIM : HUD;
        if (skill != null) {
            switch (skill.getType()) {
                case BUFF -> guiGraphics.blit(sheet, x + 3, y + 2, 0, 48, 16, 16, 128, 64);
                case HINDER -> guiGraphics.blit(sheet, x + 3, y + 2, 16, 48, 16, 16, 128, 64);
                case HEAL -> guiGraphics.blit(sheet, x + 3, y + 2, 32, 48, 16, 16, 128, 64);
                case PASSIVE -> guiGraphics.blit(sheet, x + 3, y + 2, 48, 48, 16, 16, 128, 64);
            }
            guiGraphics.drawString(font, skill.getDescription(), x + 22, y + 6, dim ? TEXT_DIM : TEXT);
        } else {
            guiGraphics.blit(HUD, x, y, 0, 20, 96, 20, 128, 64);
        }
    }

}
