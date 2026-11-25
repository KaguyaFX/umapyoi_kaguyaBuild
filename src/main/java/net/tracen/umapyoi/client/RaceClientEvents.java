package net.tracen.umapyoi.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.race.RaceBetClient;
import net.tracen.umapyoi.race.RaceClientState;
import net.tracen.umapyoi.race.RaceMeetingClient;

/** Forgets the race state when leaving a world, in case the server never said the race ended. */
@EventBusSubscriber(modid = Umapyoi.MODID, value = Dist.CLIENT)
public final class RaceClientEvents {
    private RaceClientEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RaceClientState.reset();
        RaceMeetingClient.reset();
        RaceBetClient.reset();
    }
}
