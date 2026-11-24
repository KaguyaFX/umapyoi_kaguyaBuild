package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.registry.races.RaceRegistry;
import net.tracen.umapyoi.utils.Surface;

/**
 * Server to client, when something about the race being held changes: what the race terminal shows of it.
 *
 * @param left       ticks to the start (while open or closed), or to the end of the stage
 * @param closeTicks entries close this many ticks before the start
 * @param rows       who runs, by stall
 */
public record RaceMeetingPayload(boolean active, ResourceLocation race, String applicant, int stage, int left, int closeTicks,
        float meters, int surface, boolean leftHanded, int weather, int runnerLevel, List<Row> rows) implements CustomPacketPayload {

    /** @param player the name of the player who runs, or empty for a computer runner */
    public record Row(int gate, ResourceLocation uma, String player, boolean scratched) {
    }

    public static final Type<RaceMeetingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_meeting"));
    public static final StreamCodec<FriendlyByteBuf, RaceMeetingPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.active);
                buf.writeResourceLocation(p.race);
                buf.writeUtf(p.applicant);
                buf.writeVarInt(p.stage);
                buf.writeVarInt(p.left);
                buf.writeVarInt(p.closeTicks);
                buf.writeFloat(p.meters);
                buf.writeVarInt(p.surface);
                buf.writeBoolean(p.leftHanded);
                buf.writeVarInt(p.weather);
                buf.writeVarInt(p.runnerLevel);
                buf.writeCollection(p.rows, (b, row) -> {
                    b.writeVarInt(row.gate);
                    b.writeResourceLocation(row.uma);
                    b.writeUtf(row.player);
                    b.writeBoolean(row.scratched);
                });
            },
            buf -> new RaceMeetingPayload(buf.readBoolean(), buf.readResourceLocation(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                    buf.readCollection(ArrayList::new, b -> new Row(b.readVarInt(), b.readResourceLocation(), b.readUtf(), b.readBoolean()))));

    public static final RaceMeetingPayload NONE = new RaceMeetingPayload(false, RaceRegistry.DEFAULT.location(), "", 0, 0, 0, 0,
            Surface.TURF.ordinal(), false, 0, 0, List.of());

    public RaceMeeting.Stage stageNow() {
        RaceMeeting.Stage[] stages = RaceMeeting.Stage.values();
        return stages[Math.floorMod(stage, stages.length)];
    }

    public RaceWeather weatherNow() {
        RaceWeather[] all = RaceWeather.values();
        return all[Math.floorMod(weather, all.length)];
    }

    public Surface surfaceNow() {
        Surface[] all = Surface.values();
        return all[Math.floorMod(surface, all.length)];
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceMeetingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RaceMeetingClient.set(payload));
    }
}
