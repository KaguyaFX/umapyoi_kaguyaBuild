package net.tracen.umapyoi.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.tracen.umapyoi.race.RaceSpeed;

/**
 * The server re-sends attributes whenever they change on its side, and the client replaces its own
 * modifiers with the server's. The race speed is only on the client, so put it straight back.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleUpdateAttributes", at = @At("TAIL"))
    private void umapyoi$keepRaceSpeed(ClientboundUpdateAttributesPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getId() == packet.getEntityId())
            RaceSpeed.reapply(player);
    }
}
