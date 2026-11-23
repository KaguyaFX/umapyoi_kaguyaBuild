package net.tracen.umapyoi.race;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;

/**
 * Server to client, every tick of a race: the runner's stamina and the numbers {@link RaceMotion}
 * needs. Speeds are fractions of the plain player's sprint; accelerations are per tick in the same
 * unit.
 *
 * @param sprintSpeed full speed right now (style, bonus left after stamina, skills)
 * @param cruiseSpeed walking speed right now (a fraction of the sprint)
 * @param accel       acceleration from power
 * @param decel       deceleration when above the target
 * @param dash        extra acceleration out of the gates
 * @param kick        how far from cruise to full speed starting a sprint jumps at once
 * @param startSpeed  speed when the gates open
 */
public record RaceStatePayload(boolean active, float stamina, float maxStamina, float sprintSpeed, float cruiseSpeed,
        float accel, float decel, float dash, float kick, float startSpeed) implements CustomPacketPayload {
    public static final Type<RaceStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_state"));
    public static final StreamCodec<ByteBuf, RaceStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.active);
                buf.writeFloat(p.stamina);
                buf.writeFloat(p.maxStamina);
                buf.writeFloat(p.sprintSpeed);
                buf.writeFloat(p.cruiseSpeed);
                buf.writeFloat(p.accel);
                buf.writeFloat(p.decel);
                buf.writeFloat(p.dash);
                buf.writeFloat(p.kick);
                buf.writeFloat(p.startSpeed);
            },
            buf -> new RaceStatePayload(buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));

    public static final RaceStatePayload INACTIVE = new RaceStatePayload(false, 0, 0, 1, 1, 0, 0, 0, 0, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RaceClientState.set(payload));
    }
}
