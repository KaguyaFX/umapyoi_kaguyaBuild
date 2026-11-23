package net.tracen.umapyoi.client;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.race.RaceClientState;

/**
 * Lets a racer roll to a stop. In Minecraft, letting go of the movement keys stops you almost at
 * once, so there'd be no way to drop from a sprint to a cruise without losing all the speed. Here,
 * when every movement key is released the player keeps going the way they were, slowing gently for a
 * moment, so pressing W again settles into cruising. If they don't, they stop quickly.
 * <p>
 * Vanilla sprinting is switched off as soon as nothing is pressed; the injected input is kept below
 * the amount that would start it again.
 */
@EventBusSubscriber(modid = Umapyoi.MODID, value = Dist.CLIENT)
public final class RaceInput {
    private RaceInput() {
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !RaceClientState.active())
            return;
        Input input = event.getInput();
        if (input.leftImpulse != 0 || input.forwardImpulse != 0) {
            RaceClientState.moving(player);
            return;
        }
        if (!RaceClientState.idle() && player.isSprinting())
            player.setSprinting(false);
        RaceClientState.idleTick();
        if (!RaceClientState.coasting())
            return;
        // Turn the way they were going into the player's own left/forward.
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        double left = RaceClientState.dirX() * cos + RaceClientState.dirZ() * sin;
        double forward = RaceClientState.dirZ() * cos - RaceClientState.dirX() * sin;
        input.leftImpulse = (float) (left * RaceClientState.COAST_INPUT);
        input.forwardImpulse = (float) (forward * RaceClientState.COAST_INPUT);
    }
}
