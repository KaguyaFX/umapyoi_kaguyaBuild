package net.tracen.umapyoi.client;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.career.UmaCareerData;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;

/** Career calendar in the top-left corner, only while the worn soul is in its three-year career. */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CareerOverlay implements LayeredDraw.Layer {
    public static final CareerOverlay INSTANCE = new CareerOverlay();
    private final Minecraft minecraft = Minecraft.getInstance();

    private CareerOverlay() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker tracker) {
        if (minecraft.options.hideGui || !UmapyoiConfig.OVERLAY_SWITCH.get())
            return;
        Player player = minecraft.player;
        if (player == null || player.isSpectator())
            return;
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (data == null || !CareerManager.isInCareer(soul))
            return;
        Component text = Component.translatable("umapyoi.career.overlay", CareerManager.periodName(data.periodIndex()));
        guiGraphics.drawString(minecraft.font, text, 4, 4, 0xFFFFFF);
    }
}
