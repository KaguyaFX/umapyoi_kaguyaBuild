package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.registry.races.RaceRegistry;
import net.tracen.umapyoi.registry.umadata.Motivations;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.Position;
import net.tracen.umapyoi.utils.Surface;

/**
 * Server to client, when something about the race being held changes: what the race terminal and the phone
 * show of it.
 *
 * @param left       ticks to the start (while open or closed), or to the end of the stage; while running,
 *                   ticks since the gates opened
 * @param closeTicks entries close this many ticks before the start
 * @param rows       who runs, by stall
 */
public record RaceMeetingPayload(boolean active, ResourceLocation race, String applicant, int stage, int left, int closeTicks,
        float meters, int surface, boolean leftHanded, int weather, int runnerLevel, Course course, List<Row> rows) implements CustomPacketPayload {

    /**
     * Where it's run (all zero until the course is laid out).
     *
     * @param distance  blocks from the start to the finish
     * @param finalFrom where the final leg begins
     */
    public record Course(float straight, float radius, float width, float finish, float distance, float finalFrom, long origin) {
        static final Course NONE = new Course(0, 0, 0, 0, 0, 0, 0);

        private static Course read(FriendlyByteBuf buf) {
            return new Course(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readLong());
        }

        private static void write(FriendlyByteBuf buf, Course course) {
            buf.writeFloat(course.straight);
            buf.writeFloat(course.radius);
            buf.writeFloat(course.width);
            buf.writeFloat(course.finish);
            buf.writeFloat(course.distance);
            buf.writeFloat(course.finalFrom);
            buf.writeLong(course.origin);
        }
    }

    /**
     * What anyone may know of a runner.
     *
     * @param stats     grades: speed, stamina, power, guts, wisdom
     * @param aptitudes for the surface and the distance, then for running in front, with the pace, from behind and
     *                  from the very back
     * @param mood      a {@link Motivations}
     * @param skills    a player's; computer runners have none
     */
    public record Form(byte[] stats, byte[] aptitudes, byte mood, List<ResourceLocation> skills) {
        public static final int STATS = 5, SURFACE = 0, DISTANCE = 1, STYLES = 2, APTITUDES = 6;

        public int stat(int index) {
            return stats[index];
        }

        public Aptitude aptitude(int index) {
            return Aptitude.values()[aptitudes[index]];
        }

        public Aptitude style(Position style) {
            return aptitude(STYLES + style.ordinal());
        }

        public Motivations motivation() {
            return Motivations.values()[Math.floorMod(mood, Motivations.values().length)];
        }

        private static Form read(FriendlyByteBuf buf) {
            return new Form(buf.readByteArray(STATS), buf.readByteArray(APTITUDES), buf.readByte(), buf.readCollection(ArrayList::new, FriendlyByteBuf::readResourceLocation));
        }

        private static void write(FriendlyByteBuf buf, Form form) {
            buf.writeByteArray(form.stats);
            buf.writeByteArray(form.aptitudes);
            buf.writeByte(form.mood);
            buf.writeCollection(form.skills, FriendlyByteBuf::writeResourceLocation);
        }
    }

    /**
     * @param player     the name of the player who runs, or empty for a computer runner
     * @param popularity 1 = the favourite, by the odds; 0 = not running any more
     * @param tips       one per column, 1 (best) to 4, or 0
     * @param place      how she came in; 0 = not announced, -1 = didn't finish
     * @param time       seconds, if she finished
     */
    public record Row(int gate, ResourceLocation uma, String player, boolean scratched, Form form, int popularity, byte[] tips,
            int place, float time) {
        private static Row read(FriendlyByteBuf buf) {
            return new Row(buf.readVarInt(), buf.readResourceLocation(), buf.readUtf(), buf.readBoolean(), Form.read(buf), buf.readVarInt(),
                    buf.readByteArray(RacePopularity.COLUMNS), buf.readVarInt(), buf.readFloat());
        }

        private static void write(FriendlyByteBuf buf, Row row) {
            buf.writeVarInt(row.gate);
            buf.writeResourceLocation(row.uma);
            buf.writeUtf(row.player);
            buf.writeBoolean(row.scratched);
            Form.write(buf, row.form);
            buf.writeVarInt(row.popularity);
            buf.writeByteArray(row.tips);
            buf.writeVarInt(row.place);
            buf.writeFloat(row.time);
        }
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
                Course.write(buf, p.course);
                buf.writeCollection(p.rows, Row::write);
            },
            buf -> new RaceMeetingPayload(buf.readBoolean(), buf.readResourceLocation(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                    Course.read(buf), buf.readCollection(ArrayList::new, Row::read)));

    public static final RaceMeetingPayload NONE = new RaceMeetingPayload(false, RaceRegistry.DEFAULT.location(), "", 0, 0, 0, 0,
            Surface.TURF.ordinal(), false, 0, 0, Course.NONE, List.of());

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

    /** The course, or null while it isn't laid out. */
    @Nullable
    public TrackGeometry track() {
        return course.radius <= 0 ? null : new TrackGeometry(course.straight, course.radius, course.width, course.finish, leftHanded);
    }

    /** The result is out. */
    public boolean announced() {
        return rows.stream().anyMatch(row -> row.place != 0);
    }

    @Nullable
    public Row row(int gate) {
        for (Row row : rows)
            if (row.gate == gate)
                return row;
        return null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaceMeetingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RaceMeetingClient.set(payload));
    }
}
