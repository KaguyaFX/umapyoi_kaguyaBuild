package net.tracen.umapyoi.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.race.RaceAccountPayload;
import net.tracen.umapyoi.race.RaceBetPayload;
import net.tracen.umapyoi.race.RaceLivePayload;
import net.tracen.umapyoi.race.RaceMeetingPayload;
import net.tracen.umapyoi.race.RaceOddsPayload;
import net.tracen.umapyoi.race.RaceGatePayload;
import net.tracen.umapyoi.race.RaceNoticePayload;
import net.tracen.umapyoi.race.RaceStatePayload;

@EventBusSubscriber(modid = Umapyoi.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetPacketHandler {
	public static final String PROTOCOL_VERSION = "1.0";

	@SubscribeEvent
	public static void onNetworkRegistry(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                         UseSkillPacket.TYPE,
                         UseSkillPacket.STREAM_CODEC,
                         UseSkillPacket::handle
                 )
                 .playToServer(
                         SelectSkillPacket.TYPE,
                         SelectSkillPacket.STREAM_CODEC,
                         SelectSkillPacket::handle
                 )
                 .playToServer(
                         SetupResultPacket.TYPE,
                         SetupResultPacket.STREAM_CODEC,
                         SetupResultPacket::handle
                 )
                 .playToServer(
                         EmptyResultPacket.TYPE,
                         EmptyResultPacket.STREAM_CODEC,
                         EmptyResultPacket::handle
                 )
                 .playToClient(
                         RaceStatePayload.TYPE,
                         RaceStatePayload.STREAM_CODEC,
                         RaceStatePayload::handle
                 )
                 .playToClient(
                         RaceGatePayload.TYPE,
                         RaceGatePayload.STREAM_CODEC,
                         RaceGatePayload::handle
                 )
                 .playToClient(
                         RaceNoticePayload.TYPE,
                         RaceNoticePayload.STREAM_CODEC,
                         RaceNoticePayload::handle
                 )
                 .playToClient(
                         RaceMeetingPayload.TYPE,
                         RaceMeetingPayload.STREAM_CODEC,
                         RaceMeetingPayload::handle
                 )
                 .playToClient(
                         RaceLivePayload.TYPE,
                         RaceLivePayload.STREAM_CODEC,
                         RaceLivePayload::handle
                 )
                 .playToClient(
                         RaceAccountPayload.TYPE,
                         RaceAccountPayload.STREAM_CODEC,
                         RaceAccountPayload::handle
                 )
                 .playToClient(
                         RaceOddsPayload.TYPE,
                         RaceOddsPayload.STREAM_CODEC,
                         RaceOddsPayload::handle
                 )
                 .playToServer(
                         RaceBetPayload.TYPE,
                         RaceBetPayload.STREAM_CODEC,
                         RaceBetPayload::handle
                 );
    }
}
