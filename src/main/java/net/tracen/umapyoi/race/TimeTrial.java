package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.race.track.TrackBuilder;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * Test races, started by command: one player against the clock, or with computer runners
 * ({@link RaceSession}). The course is repainted first if the race's surface or handedness isn't what is
 * there.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class TimeTrial {
    /** Once the winner is in, the rest get this long. */
    private static final int STRAGGLER_TICKS = 20 * 60;

    public enum Start {
        STARTED, PREPARING, BUSY, NOT_THERE, NO_COURSE
    }

    private record Run(ServerPlayer player, RaceSession session, int field) {
    }

    @Nullable
    private static Run current;
    /** The race the course is being repainted for, and who will run it; null when there's none. */
    @Nullable
    private static RaceSetup preparing;
    @Nullable
    private static ServerPlayer waiting;

    private TimeTrial() {
    }

    /**
     * Starts a race, repainting the course first if it isn't laid out for it.
     *
     * @param opponents how many computer runners
     * @param statLevel what all their stats are around; 0 means each around the player's own
     */
    public static Start start(ServerPlayer player, RaceSetup setup, int opponents, int statLevel) {
        if (running() || waiting != null || RaceMeeting.active() || TrackBuilder.busy())
            return Start.BUSY;
        if (!RaceWorld.isRaceWorld(player.level()))
            return Start.NOT_THERE;
        ServerLevel level = (ServerLevel) player.level();
        RacecourseData data = RacecourseData.get(level);
        if (!data.built() || data.track() == null)
            return Start.NO_COURSE;
        boolean dressing = RaceWorld.dress(level, setup,
                percent -> {
                    if (!player.hasDisconnected())
                        player.displayClientMessage(Component.translatable("umapyoi.race.trial.preparing", percent), true);
                },
                () -> {
                    if (waiting != player)
                        return; // cancelled in the meantime
                    preparing = null;
                    waiting = null;
                    if (!running() && !player.hasDisconnected() && player.level() == level)
                        run(player, level, data, setup, opponents, statLevel);
                });
        if (dressing) {
            preparing = setup;
            waiting = player;
            return Start.PREPARING;
        }
        run(player, level, data, setup, opponents, statLevel);
        return Start.STARTED;
    }

    private static void run(ServerPlayer player, ServerLevel level, RacecourseData data, RaceSetup setup, int opponentCount, int statLevel) {
        TrackGeometry track = data.track();
        RaceSession session = new RaceSession(level, track, data.origin(), setup, opponentCount > 0 ? STRAGGLER_TICKS : 0, new RaceSession.Listener() {
        });
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        // The field draws the inner stalls by lot; a lone runner gets the innermost.
        RandomSource random = level.getRandom();
        List<Integer> draw = new ArrayList<>();
        for (int lane = 0; lane <= Math.min(opponentCount, RaceSession.stalls(track) - 1); lane++)
            draw.add(lane);
        for (int i = draw.size() - 1; i > 0; i--)
            Collections.swap(draw, i, random.nextInt(i + 1));
        int own = draw.removeFirst();
        List<Double> theirs = draw.stream().map(lane -> RaceSession.stallOffset(track, lane)).toList();
        for (RaceOpponent opponent : RaceOpponent.field(level, track, setup, session.layout(), session.distance(), RaceSession.PLAIN_SPRINT,
                statsAround(statLevel, soul), theirs, soul, random))
            session.add(opponent);
        session.gather(false);
        session.add(player, own);
        session.begin(UmapyoiConfig.RACE_COUNTDOWN_SECONDS.get() * 20);
        current = new Run(player, session, draw.size() + 1);
        sendStats(player, setup, RaceStats.of(soul, RaceStyle.of(soul, level), setup.surface(), setup.category()));
    }

    /** A soul's own five stats, or five times a level. */
    private static int[] statsAround(int level, ItemStack soul) {
        if (level <= 0 && UmaSoulUtils.hasSoulData(soul))
            return UmaSoulUtils.getProperty(soul).toArray();
        int[] stats = new int[5];
        Arrays.fill(stats, Math.max(1, level));
        return stats;
    }

    /** The race, and what it makes of this soul: shown once at the start, for testing. */
    private static void sendStats(ServerPlayer player, RaceSetup setup, RaceStats stats) {
        RaceGround ground = setup.ground();
        Component surface = Component.translatable("umapyoi.race.surface." + setup.surface().name().toLowerCase(Locale.ROOT));
        player.sendSystemMessage(Component.translatable("umapyoi.race.trial.setup",
                String.format("%.0f", setup.distanceMeters()),
                Component.translatable("tooltip.umapyoi.race.distance." + setup.category().name().toLowerCase(Locale.ROOT)),
                surface,
                Component.translatable(setup.leftHanded() ? "umapyoi.race.direction.left" : "umapyoi.race.direction.right"),
                Component.translatable(setup.weather().key()),
                Component.translatable(ground.key()),
                String.format("%.2f", ground.accel()),
                String.format("%.2f", ground.drain())));
        player.sendSystemMessage(Component.translatable("umapyoi.race.trial.stats",
                String.format("%.1f", stats.bonusRatio() * 100),
                String.format("%.2f", stats.accel() * ground.accel()),
                String.format("%.0f", stats.stamina()),
                String.format("%.0f", stats.emptyFloor() * 100),
                String.format("%.2f", stats.mood()),
                surface,
                String.format("%.2f", stats.surfaceFactor()),
                String.format("%.2f", stats.distanceFactor())));
    }

    public static boolean running() {
        return current != null;
    }

    /** The race being run or prepared; null if there's none. */
    @Nullable
    static RaceSetup setup() {
        return current != null ? current.session().setup() : preparing;
    }

    /** @return false if this player has no race running or being prepared */
    public static boolean cancel(ServerPlayer player) {
        if (waiting != null && waiting == player) {
            preparing = null;
            waiting = null;
            if (!player.hasDisconnected())
                player.sendSystemMessage(Component.translatable("umapyoi.race.trial.cancelled"));
            return true;
        }
        if (current == null || current.player() != player)
            return false;
        if (!player.hasDisconnected())
            player.sendSystemMessage(Component.translatable("umapyoi.race.trial.cancelled"));
        end();
        return true;
    }

    private static void end() {
        Run run = current;
        current = null;
        if (run != null)
            run.session().cleanUp();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (current == null)
            return;
        Run run = current;
        if (run.player().hasDisconnected() || !RaceWorld.isRaceWorld(run.player().level())) {
            end();
        } else if (run.session().tick() && current == run) {
            if (run.field() > 1)
                sendResult(run.player(), run.session());
            end();
        }
    }

    /** Everyone's places, in chat. */
    static void sendResult(ServerPlayer player, RaceSession session) {
        List<RaceSession.Place> places = session.places();
        double best = places.getFirst().time();
        player.sendSystemMessage(Component.translatable("umapyoi.race.result.header", String.format("%.0f", session.setup().distanceMeters()))
                .withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < places.size(); i++) {
            RaceSession.Place place = places.get(i);
            Component when = place.time() < 0 ? Component.translatable("umapyoi.race.result.unplaced")
                    : Component.literal(i == 0 ? RaceSession.formatTime(place.time())
                            : RaceSession.formatTime(place.time()) + String.format(" (+%.2f)", place.time() - best));
            MutableComponent line = Component.literal((i + 1) + ". ").append(place.name()).append("  ").append(when);
            player.sendSystemMessage(place.player() == player ? line.withStyle(ChatFormatting.GREEN) : line);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            cancel(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            cancel(player);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            cancel(player);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        preparing = null;
        waiting = null;
        current = null; // the session cleans up after itself
    }
}
