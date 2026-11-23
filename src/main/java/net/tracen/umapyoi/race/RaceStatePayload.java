package net.tracen.umapyoi.race;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;

/**
 * Server to client: the runner's stamina and the speed bonus it leaves, every tick of a race.
 * The client turns this into its movement speed (players move on their own client) and the
 * stamina bar.
 *
 * @param bonusRatio the Umamusume bonus still in effect, as a fraction of the plain sprint
 * @param cruiseRatio walking speed as a fraction of the sprint
 */
public record RaceStatePayload(boolean active, float stamina, float maxStamina, float bonusRatio, float cruiseRatio)
        implements CustomPacketPayload {
    public static final Type<RaceStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_state"));
    public static final StreamCodec<ByteBuf, RaceStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RaceStatePayload::active,
            ByteBufCodecs.FLOAT, RaceStatePayload::stamina,
            ByteBufCodecs.FLOAT, RaceStatePayload::maxStamina,
            ByteBufCodecs.FLOAT, RaceStatePayload::bonusRatio,
            ByteBufCodecs.FLOAT, RaceStatePayload::cruiseRatio,
            RaceStatePayload::new);

    public static final RaceStatePayload INACTIVE = new RaceStatePayload(false, 0, 0, 0, 1);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RaceClientState.set(payload));
    }
}
