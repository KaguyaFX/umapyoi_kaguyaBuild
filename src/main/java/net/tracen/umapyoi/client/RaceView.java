package net.tracen.umapyoi.client;

import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.race.RaceClientState;

/**
 * Keeps the view steady in a race. Minecraft widens the view with the movement speed attribute, and in a
 * race that attribute is the race speed: from barely moving out of the stall to a skill's burst, through
 * letting go of the keys (the attribute jumps to make up for the smaller push) and running out of
 * stamina. The view pumped in and out with all of it. Here it stays as wide as an ordinary sprint; the
 * game's FOV effects setting still scales it.
 */
@EventBusSubscriber(modid = Umapyoi.MODID, value = Dist.CLIENT)
public final class RaceView {
    /** Vanilla's view while sprinting: (1.3 + 1) / 2. */
    private static final float SPRINT_VIEW = 1.15F;

    private RaceView() {
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (event.getPlayer() instanceof LocalPlayer && RaceClientState.active() && UmapyoiConfig.RACE_STEADY_VIEW.get())
            event.setNewFovModifier(SPRINT_VIEW);
    }
}
