package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;

/**
 * Server to client, to a runner: something to show for a moment just above the action bar, which the
 * race keeps busy (a skill used, or why it can't be).
 */
public record RaceNoticePayload(Component text) implements CustomPacketPayload {
    public static final Type<RaceNoticePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_notice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RaceNoticePayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, RaceNoticePayload::text,
            RaceNoticePayload::new);

    @Nullable
    private static Component waiting;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceNoticePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> waiting = payload.text());
    }

    /** What this client was last told, if it hasn't shown it yet. */
    @Nullable
    public static Component take() {
        Component text = waiting;
        waiting = null;
        return text;
    }
}
