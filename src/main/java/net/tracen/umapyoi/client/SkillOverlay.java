package net.tracen.umapyoi.client;


import net.minecraft.MethodsReturnNonnullByDefault;
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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SkillOverlay implements LayeredDraw.Layer {
    public static final SkillOverlay INSTANCE = new SkillOverlay();
    private final Minecraft minecraft = Minecraft.getInstance();

    private SkillOverlay() {
    }

    /** Its empty frame, at (0, 20): no skill. The frames by rarity are {@link SkillTiers}. */
    public static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "textures/gui/skill_hud.png");
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
            boolean burning = race != null && race.status() == RaceSkillPayload.RUNNING;
            UmaSkill skill = UmaSkillRegistry.REGISTRY.get(UmaSoulUtils.getSelectedSkill(UmapyoiAPI.getUmaSoul(player)));
            renderFrame(skill, guiGraphics, fx, fy, dim && !burning);
            // The fire dies down over the last second.
            if (skill != null && burning)
                SkillTiers.fire(guiGraphics, skill.getRarity(), fx, fy, Math.min(1F, race.left() / (float) FADE_TICKS));
            renderSkill(skill, minecraft.font, guiGraphics, fx, fy, dim);
        }
    }

    /** The frame for a skill's rarity, or the empty frame. */
    public static void renderFrame(@Nullable UmaSkill skill, GuiGraphics guiGraphics, int x, int y, boolean dim) {
        if (skill != null)
            SkillTiers.plate(guiGraphics, skill.getRarity(), x, y, dim);
    }

    public static void renderSkill(UmaSkill skill, Font font, GuiGraphics guiGraphics, int x, int y) {
        renderSkill(skill, font, guiGraphics, x, y, false);
    }

    /** The skill's icon and name on its frame; greyed if it can't be used now. */
    public static void renderSkill(@Nullable UmaSkill skill, Font font, GuiGraphics guiGraphics, int x, int y, boolean dim) {
        if (skill != null) {
            SkillTiers.icon(guiGraphics, skill, x + 3, y + 2, 16, dim);
            guiGraphics.drawString(font, skill.getDescription(), x + 22, y + 6, dim ? TEXT_DIM : TEXT);
        } else {
            guiGraphics.blit(HUD, x, y, 0, 20, 96, 20, 128, 64);
        }
    }

}
