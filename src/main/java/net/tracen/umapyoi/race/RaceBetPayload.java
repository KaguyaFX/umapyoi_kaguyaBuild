package net.tracen.umapyoi.race;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;

/** Client to server: a bet, from the phone. */
public record RaceBetPayload(int gate, boolean place, long amount) implements CustomPacketPayload {
    public static final Type<RaceBetPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_bet"));
    public static final StreamCodec<FriendlyByteBuf, RaceBetPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.gate);
                buf.writeBoolean(p.place);
                buf.writeLong(p.amount);
            },
            buf -> new RaceBetPayload(buf.readVarInt(), buf.readBoolean(), buf.readLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceBetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            Component problem = RaceMeeting.bet(player, payload.gate, payload.place, payload.amount);
            if (problem != null)
                player.displayClientMessage(problem.copy().withStyle(ChatFormatting.RED), true);
        });
    }
}
