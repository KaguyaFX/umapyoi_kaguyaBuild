package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * The race world's sky. Other dimensions all share the overworld's weather; the race world's own is
 * switched off ({@code ServerLevelMixin}) and its players are shown the weather of the race instead
 * ({@link RaceSetup#weather()}), fading in and out. It is only for show: the server's race world is
 * always dry, so nothing freezes or burns, and lightning strikes off the course, as decoration.
 * <p>
 * It can only rain where the biome allows it; chunks made before the race world had a biome of its
 * own get it when they load.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class RaceSky {
    /** How much the rain and thunder change per tick: two and a half seconds from clear to full. */
    private static final float FADE = 0.02F;
    /** Ticks between lightning strikes. */
    private static final int BOLT_MIN = 100;
    private static final int BOLT_MAX = 300;
    /** Lightning strikes this far from a player, and at least {@link #BOLT_OFF_COURSE} outside the course. */
    private static final double BOLT_NEAR = 50;
    private static final double BOLT_FAR = 110;
    private static final double BOLT_OFF_COURSE = 40;

    private static float rain;
    private static float thunder;
    private static int boltIn = BOLT_MIN;

    private RaceSky() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = RaceWorld.level(event.getServer());
        if (level == null)
            return;
        RaceSetup setup = RaceMeeting.active() ? RaceMeeting.setup() : TimeTrial.setup();
        RaceWeather weather = setup == null ? RaceWeather.SUNNY : setup.weather();
        float rainBefore = rain;
        float thunderBefore = thunder;
        rain = Mth.approach(rain, weather.raining() ? 1 : 0, FADE);
        thunder = Mth.approach(thunder, weather.thundering() ? 1 : 0, FADE);
        // Starting and stopping set the client's rain level themselves, so the real level goes after them.
        if (rainBefore == 0 && rain > 0)
            broadcast(level, ClientboundGameEventPacket.START_RAINING, 0);
        else if (rainBefore > 0 && rain == 0)
            broadcast(level, ClientboundGameEventPacket.STOP_RAINING, 0);
        if (rain != rainBefore)
            broadcast(level, ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rain);
        if (thunder != thunderBefore)
            broadcast(level, ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunder);
        lightning(level);
    }

    private static void broadcast(ServerLevel level, ClientboundGameEventPacket.Type type, float value) {
        level.getServer().getPlayerList().broadcastAll(new ClientboundGameEventPacket(type, value), RaceWorld.RACECOURSE);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        show(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        show(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        show(event.getEntity());
    }

    /** Someone arriving starts with a clear sky: tell them what it is now. */
    private static void show(Player player) {
        if (!(player instanceof ServerPlayer arrived) || !RaceWorld.isRaceWorld(arrived.level()) || rain <= 0)
            return;
        arrived.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
        arrived.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rain));
        arrived.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunder));
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        rain = 0;
        thunder = 0;
    }

    /** Now and then a harmless bolt somewhere outside the course. */
    private static void lightning(ServerLevel level) {
        if (thunder < 1 || level.players().isEmpty() || --boltIn > 0)
            return;
        RandomSource random = level.getRandom();
        boltIn = Mth.nextInt(random, BOLT_MIN, BOLT_MAX);
        ServerPlayer near = level.players().get(random.nextInt(level.players().size()));
        RacecourseData data = RacecourseData.get(level);
        TrackGeometry track = data.track();
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double far = Mth.lerp(random.nextDouble(), BOLT_NEAR, BOLT_FAR);
            BlockPos pos = BlockPos.containing(near.getX() + Math.cos(angle) * far, data.origin().getY() + 1,
                    near.getZ() + Math.sin(angle) * far);
            if (!level.isLoaded(pos) || onCourse(track, pos.getX() - data.origin().getX(), pos.getZ() - data.origin().getZ()))
                continue;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null)
                return;
            bolt.moveTo(Vec3.atBottomCenterOf(pos));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
            return;
        }
    }

    private static boolean onCourse(@Nullable TrackGeometry track, double x, double z) {
        return track != null && track.locate(x, z).offset() < track.outer() + BOLT_OFF_COURSE;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !RaceWorld.isRaceWorld(level))
            return;
        ChunkAccess chunk = event.getChunk();
        Holder<Biome> biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(RaceWorld.BIOME).orElse(null);
        if (biome == null || chunk.getNoiseBiome(0, 0, 0).is(RaceWorld.BIOME))
            return;
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, Climate.empty());
        chunk.setUnsaved(true);
    }
}
