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
 * Server to client, to everyone at the course: what is bet on whom.
 *
 * @param ready  the odds are known
 * @param open   bets are taken
 * @param left   ticks until they aren't any more
 * @param places how many places a place bet pays on (0 = there are none in so small a field)
 * @param keep   the share of the money that is paid out
 * @param unit   bets are multiples of this
 * @param lines  the money on each runner, by stall
 * @param mine   the player's own bets
 */
public record RaceOddsPayload(boolean ready, boolean open, int left, int places, float keep, int unit, List<Line> lines, List<Slip> mine) implements CustomPacketPayload {
    /** The money on a runner. Who isn't running any more has no line. */
    public record Line(int gate, float win, float place) {
    }

    /** The states of a slip. */
    public static final byte OPEN = 0, WON = 1, LOST = 2, BACK = 3;

    /** @param paid what it brought (the stake included) */
    public record Slip(int gate, boolean place, long amount, byte state, long paid) {
    }

    public static final Type<RaceOddsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_odds"));
    public static final StreamCodec<FriendlyByteBuf, RaceOddsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.ready);
                buf.writeBoolean(p.open);
                buf.writeVarInt(p.left);
                buf.writeVarInt(p.places);
                buf.writeFloat(p.keep);
                buf.writeVarInt(p.unit);
                buf.writeCollection(p.lines, (b, line) -> {
                    b.writeVarInt(line.gate);
                    b.writeFloat(line.win);
                    b.writeFloat(line.place);
                });
                buf.writeCollection(p.mine, (b, slip) -> {
                    b.writeVarInt(slip.gate);
                    b.writeBoolean(slip.place);
                    b.writeLong(slip.amount);
                    b.writeByte(slip.state);
                    b.writeLong(slip.paid);
                });
            },
            buf -> new RaceOddsPayload(buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readVarInt(),
                    buf.readCollection(ArrayList::new, b -> new Line(b.readVarInt(), b.readFloat(), b.readFloat())),
                    buf.readCollection(ArrayList::new, b -> new Slip(b.readVarInt(), b.readBoolean(), b.readLong(), b.readByte(), b.readLong()))));
    public static final RaceOddsPayload NONE = new RaceOddsPayload(false, false, 0, 0, 0, 1, List.of(), List.of());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceOddsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RaceBetClient.odds(payload));
    }
}
