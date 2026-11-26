package net.tracen.umapyoi.race;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;

/**
 * Server to client, to a runner, whenever it changes: how the selected skill stands, for the skill frame
 * and the AP bar.
 *
 * @param status {@link #READY}, {@link #NOT_NOW} or {@link #RUNNING}
 * @param cost   its action points (0 for a passive skill)
 * @param left   ticks it still runs, while running
 * @param lasts  ticks it runs in all, while running
 */
public record RaceSkillPayload(byte status, int cost, int left, int lasts) implements CustomPacketPayload {
    /** It can be used now. */
    public static final byte READY = 0;
    /** Not now: before the start, the wrong part of the race, resting, too few action points, or nothing it does in a race. */
    public static final byte NOT_NOW = 1;
    /** It is the one running right now. */
    public static final byte RUNNING = 2;

    public static final Type<RaceSkillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_skill"));
    public static final StreamCodec<ByteBuf, RaceSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, RaceSkillPayload::status,
            ByteBufCodecs.VAR_INT, RaceSkillPayload::cost,
            ByteBufCodecs.VAR_INT, RaceSkillPayload::left,
            ByteBufCodecs.VAR_INT, RaceSkillPayload::lasts,
            RaceSkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RaceClientState.skill(payload));
    }
}
