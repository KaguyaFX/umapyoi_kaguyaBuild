package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;

/**
 * Server to client, several times a second while a race is run: where everyone is, for those who watch.
 *
 * @param clock ticks since the gates opened
 */
public record RaceLivePayload(int clock, List<Spot> spots) implements CustomPacketPayload {
    public static final byte RUNNING = 0, HOME = 1, OUT = 2;

    /**
     * @param progress blocks run
     * @param offset   off the course's reference line
     * @param state    {@link #RUNNING}, {@link #HOME} (crossed the line) or {@link #OUT}
     */
    public record Spot(int gate, float progress, float offset, byte state) {
    }

    public static final Type<RaceLivePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_live"));
    public static final StreamCodec<FriendlyByteBuf, RaceLivePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.clock);
                buf.writeCollection(p.spots, (b, spot) -> {
                    b.writeByte(spot.gate);
                    b.writeFloat(spot.progress);
                    b.writeFloat(spot.offset);
                    b.writeByte(spot.state);
                });
            },
            buf -> new RaceLivePayload(buf.readVarInt(),
                    buf.readCollection(ArrayList::new, b -> new Spot(b.readByte(), b.readFloat(), b.readFloat(), b.readByte()))));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceLivePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RaceMeetingClient.live(payload));
    }
}
