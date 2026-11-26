package net.tracen.umapyoi.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
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
import net.tracen.umapyoi.registry.skills.SkillType;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.UmaSoulUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ActionBarOverlay implements LayeredDraw.Layer {
    public static final ActionBarOverlay INSTANCE = new ActionBarOverlay();
    private final Minecraft minecraft = Minecraft.getInstance();

    private ActionBarOverlay() {
    }

    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "textures/gui/actionbar.png");

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker tracker) {
        if (minecraft.options.hideGui || !UmapyoiConfig.OVERLAY_SWITCH.get())
            return;

        int x = guiGraphics.guiWidth();
        int y = guiGraphics.guiHeight();

        Player player = minecraft.player;
        if (player.isSpectator())
            return;

        if (!UmapyoiAPI.getUmaSoul(player).isEmpty()) {
            renderSkill(UmapyoiAPI.getUmaSoul(player), guiGraphics, x - 8, y - 64);
        }
    }

    private void renderSkill(ItemStack soul, GuiGraphics guiGraphics, int x, int y) {
        int ap = UmaSoulUtils.getActionPoint(soul);
        int maxAp = UmaSoulUtils.getMaxActionPoint(soul);
        // In a race it stays up even when full, so the selected skill's cost can be seen.
        RaceSkillPayload race = RaceClientState.skill();
        if (ap == maxAp && race == null || maxAp <= 0)
            return;
        guiGraphics.blit(HUD, x, y - 128, 11, 0, 5, 128, 16, 128);
        int apbar = ap != 0 ? ap * 128 / maxAp : 0;
        guiGraphics.blit(HUD, x, y - apbar, 6, 128 - apbar, 5, apbar, 16, 128);
        guiGraphics.blit(HUD, x - 7, y - 3 - apbar, 0, 0, 6, 7, 16, 128);
        String str = String.valueOf(ap);
        guiGraphics.drawString(this.minecraft.font, str, x - 8 - this.minecraft.font.width(str), y - 3 - apbar, 0xFFFFFF);
        // What the selected skill costs: white if there is enough, red if not.
        int cost = race != null ? race.cost() : cost(soul);
        if (cost > 0 && cost <= maxAp) {
            int line = y - cost * 128 / maxAp;
            guiGraphics.fill(x - 1, line, x + 6, line + 1, ap >= cost ? 0xDCFFFFFF : 0xEBFF4040);
        }
    }

    private static int cost(ItemStack soul) {
        if (UmaSoulUtils.getSkills(soul).isEmpty())
            return 0;
        UmaSkill skill = UmaSkillRegistry.REGISTRY.get(UmaSoulUtils.getSelectedSkill(soul));
        return skill == null || skill.getType() == SkillType.PASSIVE ? 0 : skill.getActionPoint();
    }
}
