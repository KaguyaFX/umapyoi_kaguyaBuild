package net.tracen.umapyoi.race;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;

/**
 * Server to client: a player's own horse tickets.
 *
 * @param staked     on the race being held
 * @param last       what the last race brought
 * @param jewelPrice what a jewel costs
 */
public record RaceAccountPayload(long balance, long staked, long last, int jewelPrice) implements CustomPacketPayload {
    public static final Type<RaceAccountPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_account"));
    public static final StreamCodec<FriendlyByteBuf, RaceAccountPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.balance);
                buf.writeLong(p.staked);
                buf.writeLong(p.last);
                buf.writeVarInt(p.jewelPrice);
            },
            buf -> new RaceAccountPayload(buf.readLong(), buf.readLong(), buf.readLong(), buf.readVarInt()));
    public static final RaceAccountPayload NONE = new RaceAccountPayload(0, 0, 0, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceAccountPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RaceBetClient.account(payload));
    }
}
