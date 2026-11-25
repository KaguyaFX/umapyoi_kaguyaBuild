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
 * @param exhausted   stamina ran out: no jumping (the speeds here already allow for it)
 * @param limit       the most the runner in front allows (a big number when nobody is there)
 * @param limitDecel  deceleration down to that
 * @param charge      saved up for the last leg, 0 to 1
 * @param releasing   it is being used (the speeds here already allow for it)
 * @param lostTouch   too far behind to save anything up
 */
public record RaceStatePayload(boolean active, float stamina, float maxStamina, float sprintSpeed, float cruiseSpeed,
        float accel, float decel, float dash, float kick, float startSpeed, boolean exhausted,
        float limit, float limitDecel, float charge, boolean releasing, boolean lostTouch) implements CustomPacketPayload {
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
                buf.writeBoolean(p.exhausted);
                buf.writeFloat(p.limit);
                buf.writeFloat(p.limitDecel);
                buf.writeFloat(p.charge);
                buf.writeBoolean(p.releasing);
                buf.writeBoolean(p.lostTouch);
            },
            buf -> new RaceStatePayload(buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean()));

    public static final RaceStatePayload INACTIVE = new RaceStatePayload(false, 0, 0, 1, 1, 0, 0, 0, 0, 0, false, 1000, 0, 0, false, false);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RaceClientState.set(payload));
    }
}
