package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;

/**
 * Server to client, to a runner in a starting stall: everyone is in, and the gates will open at a
 * moment nobody knows. Until then the player should keep still; whoever can't is late out of it.
 * When the gates open is up to the player's own eyes: the door in front of them.
 *
 * @param door    the door of their stall
 * @param allowed how often they may press a movement key meanwhile (and are warned) before they'll be late
 * @param late    ticks a late runner is held in the stall after the gates open
 */
public record RaceGatePayload(BlockPos door, int allowed, int late) implements CustomPacketPayload {
    public static final Type<RaceGatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_gate"));
    public static final StreamCodec<ByteBuf, RaceGatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RaceGatePayload::door,
            ByteBufCodecs.VAR_INT, RaceGatePayload::allowed,
            ByteBufCodecs.VAR_INT, RaceGatePayload::late,
            RaceGatePayload::new);

    @Nullable
    private static RaceGatePayload called;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceGatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> called = payload);
    }

    /** The call this client was given since it last asked, if any. */
    @Nullable
    public static RaceGatePayload take() {
        RaceGatePayload payload = called;
        called = null;
        return payload;
    }
}
