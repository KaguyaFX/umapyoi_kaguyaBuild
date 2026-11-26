package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.item.UmaRaceTicketItem;
import net.tracen.umapyoi.race.track.TrackBuilder;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.registry.races.Race;
import net.tracen.umapyoi.registry.races.RaceLoot;
import net.tracen.umapyoi.registry.races.RaceRegistry;
import net.tracen.umapyoi.registry.races.field.RaceField;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.Distance;
import net.tracen.umapyoi.utils.Surface;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * A race anyone on the server can come to: someone hands in a race ticket, the race world is laid out for
 * that race and opens, and after it the world closes again. There's one at a time; the rest of the time
 * nobody gets in.
 * <ul>
 * <li>PREPARING: the course is built and dressed.</li>
 * <li>OPEN: people come to run or to watch, and come and go as they like. The field is full of computer
 * runners from the start; whoever enters takes one's stall. A runner who is out when entries close has
 * withdrawn.</li>
 * <li>CLOSED: the field is settled; leaving now is giving up. Shortly before the start the runners
 * are put into the stalls.</li>
 * <li>RUNNING: the race ({@link RaceSession}). If nobody is there to run or watch, and nobody has bet on it, it's simply off.</li>
 * <li>RESULTS: everyone is in, and it takes a moment until the result is announced.</li>
 * <li>LEAVING: fireworks, and time to go.</li>
 * </ul>
 * Nothing of it is saved: when the server stops before the start, the ticket is returned
 * ({@link RaceMail}).
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class RaceMeeting {
    public enum Stage {
        PREPARING, OPEN, CLOSED, RUNNING, RESULTS, LEAVING;

        public String key() {
            return "umapyoi.meeting.stage." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Fireworks go up this long, one every so many ticks. */
    private static final int FIREWORK_TICKS = 20 * 20;
    private static final int FIREWORK_EVERY = 8;
    /** Those who watch are told where everyone is every so many ticks. */
    static final int LIVE_EVERY = 2;
    /** Computer runners start walking up to the stalls this long before the players are put in. */
    private static final int WALK_IN_SECONDS = 15;
    /** Runners arrive this many blocks before the line. */
    private static final double ARRIVE_BEFORE_LINE = 14;

    /** A stall: a computer runner, or the player who took her place. */
    private static final class Slot {
        private final int lane;
        private final RaceOpponent opponent;
        @Nullable
        private UUID player;
        private String playerName = "";
        /** The runner's soul: the player's, as far as known, or hers. */
        private ItemStack soul;
        /** The player gave up after entries closed: the stall stays empty. */
        private boolean scratched;

        private Slot(int lane, RaceOpponent opponent) {
            this.lane = lane;
            this.opponent = opponent;
            this.soul = opponent.soul();
        }
    }

    private record Finisher(UUID player, int place, ItemStack soul) {
    }

    @Nullable
    private static RaceMeeting current;

    private final MinecraftServer server;
    private final ServerLevel level;
    private final Race race;
    private final RaceSetup setup;
    private final UUID applicant;
    private final String applicantName;
    private final boolean applicantRuns;
    private final ItemStack ticket;
    private final int runnerLevel;
    private final ServerBossEvent bar;
    private final List<Slot> slots = new ArrayList<>();
    /** Who came through the meeting, as opposed to an operator who was there anyway. */
    private final Set<UUID> visitors = new HashSet<>();
    private final Map<UUID, Finisher> finishers = new LinkedHashMap<>();
    /** Runners from the stalls until the result is out: there's no leaving for them. */
    private final Set<UUID> bound = new HashSet<>();
    private Stage stage = Stage.PREPARING;
    private int preparedPercent;
    /** Ticks to the start, then (RESULTS, LEAVING) to the end of the stage. */
    private int left;
    private int age;
    /** The runners are on their way to the stalls; the players are in them. */
    private boolean called;
    private boolean loaded;
    private boolean awarded;
    /** The result, once it's announced. */
    private List<RaceSession.Place> result = List.of();
    @Nullable
    private RaceSession session;
    @Nullable
    private RaceBets bets;
    /** The stalls from the favourite on, as last told. */
    private List<Integer> favourites = List.of();

    private RaceMeeting(ServerPlayer applicant, ServerLevel level, Race race, RaceSetup setup, boolean applicantRuns, ItemStack ticket) {
        this.server = applicant.server;
        this.level = level;
        this.race = race;
        this.setup = setup;
        this.applicant = applicant.getUUID();
        this.applicantName = applicant.getScoreboardName();
        this.applicantRuns = applicantRuns;
        this.ticket = ticket.copyWithCount(1);
        this.runnerLevel = runnerLevel(race);
        this.bar = new ServerBossEvent(name(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    }

    // ---- asking for one

    public static boolean active() {
        return current != null;
    }

    /** The race being held; null if there's none. */
    @Nullable
    static RaceSetup setup() {
        return current == null ? null : current.setup;
    }

    /**
     * Asks for the race on a ticket.
     *
     * @param run whether the applicant means to run in it
     * @return why not, or null: then the ticket is used up
     */
    @Nullable
    public static Component apply(ServerPlayer player, ItemStack ticket, boolean run) {
        if (current != null || TimeTrial.setup() != null || RaceSession.current() != null || TrackBuilder.busy())
            return Component.translatable("umapyoi.meeting.problem.busy");
        ServerLevel level = RaceWorld.level(player.server);
        if (level == null)
            return Component.translatable("umapyoi.command.race.missing");
        ResourceLocation id = UmaRaceTicketItem.getRaceID(ticket);
        Race race = id.equals(RaceRegistry.DEFAULT.location()) ? null : UmapyoiAPI.getRaceRegistry(level).get(id);
        if (race == null)
            return Component.translatable("tooltip.umapyoi.race.unavailable.invalid");
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (adaptive(race) && !run)
            return Component.translatable("umapyoi.meeting.problem.adaptive");
        if (run) {
            Component problem = problem(race, soul);
            if (problem != null)
                return problem;
        }
        RandomSource random = level.getRandom();
        double sky = random.nextDouble();
        double storm = UmapyoiConfig.RACE_MEETING_STORM_CHANCE.get();
        RaceWeather weather = sky < storm ? RaceWeather.THUNDER
                : sky < storm + UmapyoiConfig.RACE_MEETING_RAIN_CHANCE.get() ? RaceWeather.RAIN : RaceWeather.SUNNY;
        RaceField field = race.field(level, soul);
        RaceSetup setup = new RaceSetup(race.length(soul), race.surface(soul), field != null && field.leftHanded(), weather);
        current = new RaceMeeting(player, level, race, setup, run, ticket);
        current.prepare();
        return null;
    }

    /** A race whose distance or surface goes by the soul that runs it. */
    public static boolean adaptive(Race race) {
        return race.distance == Distance.ADAPTIVE || race.surface == Surface.ADAPTIVE;
    }

    /** Why this soul can't run in the race, or null. */
    @Nullable
    public static Component problem(Race race, ItemStack soul) {
        if (!UmaSoulUtils.hasSoulData(soul))
            return Component.translatable("umapyoi.meeting.problem.no_soul");
        return race.getUnavailableReason(soul);
    }

    /** What the computer runners' stats are around, by the race's grade. */
    public static int runnerLevel(Race race) {
        List<? extends Integer> levels = UmapyoiConfig.RACE_MEETING_LEVELS.get();
        int index = Math.min(race.ranking().ordinal(), levels.size() - 1);
        return index < 0 ? 10 : Mth.clamp(levels.get(index), 1, 39);
    }

    private void prepare() {
        sync();
        RacecourseData data = RacecourseData.get(level);
        if (data.built()) {
            dress();
            return;
        }
        RaceWorld.build(level, RaceWorld.configuredTrack(), percent -> progress(percent / 2), result -> {
            if (current == this)
                dress();
        });
    }

    private void dress() {
        boolean dressing = RaceWorld.dress(level, setup, percent -> progress(50 + percent / 2), () -> {
            if (current == this)
                open();
        });
        if (!dressing)
            open();
    }

    private void progress(int percent) {
        preparedPercent = percent;
        ServerPlayer player = server.getPlayerList().getPlayer(applicant);
        if (current == this && player != null)
            player.displayClientMessage(Component.translatable("umapyoi.race.trial.preparing", percent), true);
    }

    private void open() {
        RacecourseData data = RacecourseData.get(level);
        TrackGeometry track = data.track();
        session = new RaceSession(level, track, data.origin(), setup, UmapyoiConfig.RACE_MEETING_STRAGGLER_SECONDS.get() * 20, new RaceSession.Listener() {
            @Override
            public void opened() {
                stage = Stage.RUNNING;
                sync();
            }

            @Override
            public void finished(ServerPlayer player, int place, double time) {
                ItemStack soul = UmapyoiAPI.getUmaSoul(player);
                finishers.put(player.getUUID(), new Finisher(player.getUUID(), place, soul.copy()));
                record(player, place == 1);
            }

            @Override
            public void gaveUp(ServerPlayer player) {
                Slot slot = slot(player.getUUID());
                if (slot != null && session != null && session.opened())
                    slot.scratched = true;
                else if (slot != null)
                    scratch(slot);
                if (session != null && session.opened())
                    record(player, false);
                sync();
            }
        });
        int runners = Mth.clamp(UmapyoiConfig.RACE_MEETING_RUNNERS.get(), 2, RaceSession.stalls(track));
        List<Double> stalls = new ArrayList<>();
        for (int lane = 0; lane < runners; lane++)
            stalls.add(RaceSession.stallOffset(track, lane));
        int[] stats = new int[5];
        Arrays.fill(stats, runnerLevel);
        for (RaceOpponent opponent : RaceOpponent.field(level, track, setup, session.layout(), session.distance(), RaceSession.PLAIN_SPRINT,
                stats, stalls, ItemStack.EMPTY, level.getRandom())) {
            slots.add(new Slot(opponent.lane(track), opponent));
            session.add(opponent);
        }
        List<? extends Integer> pools = UmapyoiConfig.RACE_BET_POOLS.get();
        int pool = pools.isEmpty() ? 0 : pools.get(Math.min(race.ranking().ordinal(), pools.size() - 1));
        bets = new RaceBets(server, level, track, setup, session, pool);
        bets.field(entries());
        stage = Stage.OPEN;
        left = UmapyoiConfig.RACE_MEETING_OPEN_SECONDS.get() * 20;
        announceOpening();
        ServerPlayer player = server.getPlayerList().getPlayer(applicant);
        if (applicantRuns && player != null && player.isAlive()) {
            Component problem = enterThis(player);
            if (problem != null)
                player.sendSystemMessage(problem.copy().withStyle(ChatFormatting.RED));
        }
        sync();
    }

    private void announceOpening() {
        MutableComponent message = Component.translatable("umapyoi.meeting.opened", name(), description(), minutes(left))
                .append(" ").append(link("umapyoi.meeting.action.enter", "/umapyoi meeting enter", ChatFormatting.GREEN))
                .append(" ").append(link("umapyoi.meeting.action.watch", "/umapyoi meeting watch", ChatFormatting.AQUA));
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static Component link(String key, String command, ChatFormatting color) {
        return Component.literal("[").append(Component.translatable(key)).append("]").withStyle(style -> style.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    // ---- coming and going

    private static Component none() {
        return Component.translatable("umapyoi.meeting.problem.none");
    }

    /** Enters a player as a runner, or brings an entered one back to the course. @return why not, or null */
    @Nullable
    public static Component enter(ServerPlayer player) {
        return current == null ? none() : current.enterThis(player);
    }

    @Nullable
    private Component enterThis(ServerPlayer player) {
        if (stage != Stage.OPEN)
            return Component.translatable("umapyoi.meeting.problem.closed");
        Slot slot = slot(player.getUUID());
        if (slot == null) {
            ItemStack soul = UmapyoiAPI.getUmaSoul(player);
            Component problem = problem(race, soul);
            if (problem != null)
                return problem;
            slot = freeSlot(soul);
            if (slot == null)
                return Component.translatable("umapyoi.meeting.problem.full");
            bets.giveBack(slot.lane);
            slot.player = player.getUUID();
            slot.playerName = player.getScoreboardName();
            slot.soul = soul.copy();
            session.remove(slot.opponent);
            tell(Component.translatable("umapyoi.meeting.entered", player.getDisplayName(), UmaSoulUtils.getTranslatedUmaName(slot.soul), slot.lane + 1));
            player.sendSystemMessage(Component.translatable("umapyoi.meeting.entered_self", slot.lane + 1, minutes(closeTicks())).withStyle(ChatFormatting.GREEN));
            bets.field(entries());
            sync();
        }
        sendToTrack(player);
        return null;
    }

    /** The stall of the same Umamusume, or else any computer runner's. */
    @Nullable
    private Slot freeSlot(ItemStack soul) {
        var registry = UmapyoiAPI.getUmaDataRegistry(level);
        ResourceLocation identifier = RaceOpponent.identifier(registry, soul);
        List<Slot> free = slots.stream().filter(slot -> slot.player == null).toList();
        if (free.isEmpty())
            return null;
        return free.stream().filter(slot -> RaceOpponent.identifier(registry, slot.opponent.soul()).equals(identifier)).findFirst()
                .orElseGet(() -> free.get(level.getRandom().nextInt(free.size())));
    }

    /** Takes an entry back while entries are open. @return why not, or null */
    @Nullable
    public static Component withdraw(ServerPlayer player) {
        if (current == null)
            return none();
        Slot slot = current.slot(player.getUUID());
        if (slot == null)
            return Component.translatable("umapyoi.meeting.problem.not_entered");
        if (current.stage != Stage.OPEN)
            return Component.translatable("umapyoi.meeting.problem.closed");
        current.free(slot);
        current.bets.field(current.entries());
        if (RaceWorld.isRaceWorld(player.level()))
            current.sendToStand(player);
        player.sendSystemMessage(Component.translatable("umapyoi.meeting.withdrawn"));
        current.sync();
        return null;
    }

    /** Gives a stall back to its computer runner. */
    private void free(Slot slot) {
        bets.giveBack(slot.lane);
        slot.player = null;
        slot.playerName = "";
        slot.soul = slot.opponent.soul();
        session.add(slot.opponent);
    }

    /** This runner doesn't start after all. */
    private void scratch(Slot slot) {
        if (slot.scratched)
            return;
        slot.scratched = true;
        bets.scratch(slot.lane);
        if (!bets.closed())
            bets.field(entries());
    }

    /** Who runs, for the odds. */
    private List<RaceOdds.Entry> entries() {
        refreshSouls();
        RaceAccounts accounts = RaceAccounts.get(server);
        return slots.stream().filter(slot -> !slot.scratched).map(slot -> new RaceOdds.Entry(slot.lane, slot.soul.copy(),
                slot.player == null ? slot.opponent.trim() : 1, slot.player != null, slot.player == null ? 0 : accounts.edge(slot.player))).toList();
    }

    /** What the players who run look like now. @return whether anything that counts is different */
    private boolean refreshSouls() {
        boolean different = false;
        for (Slot slot : slots) {
            ServerPlayer player = slot.player == null ? null : server.getPlayerList().getPlayer(slot.player);
            ItemStack soul = player == null ? ItemStack.EMPTY : UmapyoiAPI.getUmaSoul(player);
            if (!UmaSoulUtils.hasSoulData(soul))
                continue;
            different |= !UmaSoulUtils.getName(soul).equals(UmaSoulUtils.getName(slot.soul)) || UmaSoulUtils.getMotivation(soul) != UmaSoulUtils.getMotivation(slot.soul)
                    || !Arrays.equals(UmaSoulUtils.getProperty(soul).toArray(), UmaSoulUtils.getProperty(slot.soul).toArray());
            slot.soul = soul.copy();
        }
        return different;
    }

    /** Ticks until no more bets are taken. */
    private int betTicks() {
        return stage.ordinal() >= Stage.RUNNING.ordinal() ? 0 : Math.max(0, left - UmapyoiConfig.RACE_BET_CLOSE_SECONDS.get() * 20);
    }

    /** A bet. @return why not, or null */
    @Nullable
    static Component bet(ServerPlayer player, int gate, boolean place, long amount) {
        if (current == null || current.bets == null)
            return none();
        if (!RaceWorld.isRaceWorld(player.level()) || !current.visitors.contains(player.getUUID()))
            return Component.translatable("umapyoi.ticket.problem.not_there");
        Component problem = current.bets.bet(player, gate - 1, place, amount);
        if (problem == null) {
            player.displayClientMessage(Component.translatable("umapyoi.ticket.bought", amount, gate).withStyle(ChatFormatting.GREEN), true);
            current.bets.tell(player, current.betTicks());
        }
        return problem;
    }

    /** Lets a player in to watch. @return why not, or null */
    @Nullable
    public static Component watch(ServerPlayer player) {
        if (current == null)
            return none();
        if (current.stage == Stage.PREPARING || current.stage == Stage.LEAVING)
            return Component.translatable("umapyoi.meeting.problem.not_open");
        if (RaceWorld.isRaceWorld(player.level()) && current.session != null && current.session.running(player))
            return Component.translatable("umapyoi.meeting.problem.running");
        current.sendToStand(player);
        return null;
    }

    /** Sends a player home. @return why not, or null */
    @Nullable
    public static Component leave(ServerPlayer player) {
        if (bound(player))
            return Component.translatable("umapyoi.meeting.exit.racing");
        return RaceWorld.leave(player) ? null : Component.translatable("umapyoi.command.race.not_there");
    }

    /** Whether a player is in a race and has to stay until the result is out. */
    static boolean bound(ServerPlayer player) {
        return current != null && current.bound.contains(player.getUUID());
    }

    /** Whether leaving now would be giving up a race. */
    static boolean committed(ServerPlayer player) {
        if (current == null)
            return false;
        Slot slot = current.slot(player.getUUID());
        return slot != null && !slot.scratched && (current.stage == Stage.CLOSED
                || current.stage == Stage.RUNNING && current.session.running(player));
    }

    /** Someone who would have died: back to the stand, out of the race. @return false if they aren't here for one */
    static boolean rescue(ServerPlayer player) {
        if (current == null || !current.visitors.contains(player.getUUID()))
            return false;
        current.session.giveUp(player);
        current.sendToStand(player);
        return true;
    }

    private void sendToTrack(ServerPlayer player) {
        RacecourseData data = RacecourseData.get(level);
        TrackGeometry track = data.track();
        TrackGeometry.Point at = track.pointAt(track.lap() - ARRIVE_BEFORE_LINE, track.middle() + level.getRandom().nextDouble() * 6 - 3);
        arrive(player, at, data.origin(), 1);
    }

    /** Spectators come in at the far end of one of the tunnels under the stand and walk out to the front. */
    private void sendToStand(ServerPlayer player) {
        RacecourseData data = RacecourseData.get(level);
        TrackGeometry.Point tunnel = TrackBuilder.standTunnel(data.track(), level.getRandom());
        if (tunnel != null)
            arrive(player, tunnel, data.origin(), 1);
        else
            arrive(player, TrackBuilder.standEntrance(data.track(), level.getRandom().nextInt(9) - 4), data.origin(), TrackBuilder.STAND_ENTRANCE_HEIGHT);
    }

    private void arrive(ServerPlayer player, TrackGeometry.Point at, BlockPos origin, int height) {
        visitors.add(player.getUUID());
        RaceWorld.visit(player, level, new Vec3(origin.getX() + at.x(), origin.getY() + height, origin.getZ() + at.z()), at.yaw());
        if (RaceWorld.isRaceWorld(player.level()) && !bound(player))
            RaceKit.give(player);
        bets.tell(player, betTicks());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            PacketDistributor.sendToPlayer(player, current == null ? RaceMeetingPayload.NONE : current.payload());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (current != null && event.getEntity() instanceof ServerPlayer player)
            current.gone(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (current != null && event.getFrom() == RaceWorld.RACECOURSE && event.getEntity() instanceof ServerPlayer player)
            current.gone(player);
    }

    /** Someone isn't in the race world any more. */
    private void gone(ServerPlayer player) {
        bar.removePlayer(player);
        bound.remove(player.getUUID());
        Slot slot = slot(player.getUUID());
        if (slot == null || slot.scratched)
            return;
        if (stage == Stage.OPEN) {
            player.sendSystemMessage(Component.translatable("umapyoi.meeting.keeps_entry", minutes(closeTicks())).withStyle(ChatFormatting.YELLOW));
        } else if (stage == Stage.CLOSED || stage == Stage.RUNNING) {
            if (!session.giveUp(player)) {
                if (session.finished(player))
                    return;
                scratch(slot);
            }
            tell(Component.translatable("umapyoi.meeting.gave_up", player.getDisplayName()));
            sync();
        }
    }

    @Nullable
    private Slot slot(UUID player) {
        for (Slot slot : slots)
            if (player.equals(slot.player))
                return slot;
        return null;
    }

    // ---- as time goes by

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (current != null)
            current.tick();
    }

    private int closeTicks() {
        return Math.max(0, left - UmapyoiConfig.RACE_MEETING_CLOSE_SECONDS.get() * 20);
    }

    private void tick() {
        age++;
        switch (stage) {
            case PREPARING -> {
                return;
            }
            case OPEN -> {
                session.tick();
                left--;
                takeBets();
                if (closeTicks() <= 0)
                    close();
            }
            case CLOSED -> {
                left--;
                takeBets();
                int gate = UmapyoiConfig.RACE_MEETING_GATE_SECONDS.get();
                if (!called && left <= (gate + WALK_IN_SECONDS) * 20) {
                    called = true;
                    session.gather(true);
                    tell(Component.translatable("umapyoi.meeting.loading"));
                }
                if (!loaded && left <= gate * 20 && !load())
                    return;
                session.tick(); // opens the gates and moves on to RUNNING
            }
            case RUNNING -> {
                bets.close();
                bets.tick(0);
                boolean over = session.tick();
                if (age % LIVE_EVERY == 0)
                    showLive();
                if (over)
                    finish();
                else if (level.players().isEmpty() && finishers.isEmpty() && !bets.any()) {
                    end(Component.translatable("umapyoi.meeting.nobody", name()));
                    return;
                }
            }
            case RESULTS -> {
                bets.tick(0);
                session.coast();
                if (--left <= 0)
                    announceResult();
            }
            case LEAVING -> {
                bets.tick(0);
                if (--left <= 0) {
                    end(null);
                    return;
                }
                int total = UmapyoiConfig.RACE_MEETING_LEAVE_SECONDS.get() * 20;
                if (total - left < FIREWORK_TICKS && age % FIREWORK_EVERY == 0)
                    firework();
            }
        }
        if (age % 10 == 0) {
            for (ServerPlayer player : level.players())
                bar.addPlayer(player);
            keepInPlace();
            updateBar();
        }
    }

    private void takeBets() {
        if (betTicks() <= 0)
            bets.close();
        bets.tick(betTicks());
        List<Integer> now = bets.favourites();
        if (age % 20 == 0 && !now.isEmpty() && !now.equals(favourites)) {
            favourites = now;
            sync();
        }
    }

    /** Entries close: whoever isn't there has withdrawn. */
    private void close() {
        stage = Stage.CLOSED;
        boolean different = refreshSouls();
        for (Slot slot : slots) {
            if (slot.player == null)
                continue;
            ServerPlayer player = server.getPlayerList().getPlayer(slot.player);
            boolean there = player != null && player.level() == level;
            if (there && problem(race, UmapyoiAPI.getUmaSoul(player)) == null)
                continue;
            tell(Component.translatable("umapyoi.meeting.missed", slot.playerName));
            if (player != null && !there)
                player.sendSystemMessage(Component.translatable("umapyoi.meeting.missed_self").withStyle(ChatFormatting.RED));
            free(slot);
            different = true;
            if (there)
                sendToStand(player);
        }
        if (different)
            bets.field(entries());
        tell(Component.translatable("umapyoi.meeting.entries_closed", minutes(left)));
        sync();
    }

    /** Into the stalls. @return false if there's nobody and it's off */
    private boolean load() {
        loaded = true;
        if (level.players().isEmpty()) {
            end(Component.translatable("umapyoi.meeting.nobody", name()));
            return false;
        }
        for (Slot slot : slots) {
            if (slot.player == null || slot.scratched)
                continue;
            ServerPlayer player = server.getPlayerList().getPlayer(slot.player);
            if (player == null || player.level() != level)
                scratch(slot);
            else {
                session.add(player, slot.lane);
                bound.add(slot.player);
                RaceKit.take(player);
            }
        }
        session.begin(left);
        sync();
        return true;
    }

    /** Everyone is in, or too late. */
    private void finish() {
        stage = Stage.RESULTS;
        left = UmapyoiConfig.RACE_MEETING_RESULT_SECONDS.get() * 20;
        sync();
    }

    private void announceResult() {
        stage = Stage.LEAVING;
        left = UmapyoiConfig.RACE_MEETING_LEAVE_SECONDS.get() * 20;
        List<RaceSession.Place> places = session.places();
        result = places;
        MutableComponent top = Component.empty();
        for (int i = 0; i < Math.min(3, places.size()) && places.get(i).time() >= 0; i++)
            top.append(i == 0 ? "" : "  ").append(Component.translatable("umapyoi.meeting.place", i + 1)).append(" ").append(places.get(i).name());
        server.getPlayerList().broadcastSystemMessage(Component.translatable("umapyoi.meeting.result", name(), top).withStyle(ChatFormatting.GOLD), false);
        for (ServerPlayer player : level.players()) {
            TimeTrial.sendResult(player, session);
            if (!places.isEmpty() && places.getFirst().time() >= 0) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
                player.connection.send(new ClientboundSetSubtitleTextPacket(name()));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("umapyoi.meeting.winner", places.getFirst().name()).withStyle(ChatFormatting.GOLD)));
            }
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        session.cleanUp();
        award();
        bets.settle(places);
        learn(places);
        for (ServerPlayer player : level.players())
            bets.tell(player, 0);
        bound.clear();
        for (ServerPlayer player : level.players())
            if (visitors.contains(player.getUUID()))
                RaceKit.give(player);
        sync();
    }

    // ---- what it's worth

    /** The crowd has seen how the players run. */
    private void learn(List<RaceSession.Place> places) {
        for (RaceSession.Place place : places) {
            double expected = bets.expected(place.lane());
            if (place.player() != null && place.time() >= 0 && expected > 0)
                RaceAccounts.get(server).ran(place.player().getUUID(), (place.time() - expected) / (setup.distanceMeters() / 1000));
        }
    }

    /** On the soul: the race was run, and perhaps won. */
    private void record(ServerPlayer player, boolean won) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (UmaSoulUtils.hasSoulData(soul))
            race.followUp(soul, level, won);
    }

    /** Prizes for everyone who came in. */
    private void award() {
        if (awarded)
            return;
        awarded = true;
        List<? extends Double> shares = UmapyoiConfig.RACE_MEETING_REWARDS.get();
        if (shares.isEmpty())
            return;
        for (Finisher finisher : finishers.values()) {
            double share = shares.get(Math.min(finisher.place(), shares.size()) - 1);
            List<ItemStack> prizes = RaceLoot.prizes(level, race, finisher.soul(), share);
            if (!prizes.isEmpty())
                RaceMail.send(server, finisher.player(), prizes, Component.translatable("umapyoi.meeting.prize", name(), finisher.place()));
        }
    }

    private void refund() {
        RaceMail.send(server, applicant, List.of(ticket.copy()), Component.translatable("umapyoi.meeting.refund", name()));
    }

    // ---- the end

    /** Skips to the next stage (for testing). */
    public static boolean skip() {
        if (current == null)
            return false;
        switch (current.stage) {
            case OPEN -> current.left = Math.min(current.left, UmapyoiConfig.RACE_MEETING_CLOSE_SECONDS.get() * 20);
            case CLOSED -> {
                if (!current.loaded)
                    current.left = Math.min(current.left, UmapyoiConfig.RACE_MEETING_GATE_SECONDS.get() * 20);
            }
            case RESULTS, LEAVING -> current.left = 1;
            default -> {
                return false;
            }
        }
        current.bets.touch();
        current.sync();
        return true;
    }

    /** Calls it off; the ticket goes back unless the race has started. */
    public static boolean stop() {
        if (current == null)
            return false;
        RaceMeeting meeting = current;
        if (meeting.stage.ordinal() < Stage.RUNNING.ordinal())
            meeting.refund();
        else
            meeting.award();
        meeting.end(Component.translatable("umapyoi.meeting.stopped", meeting.name()));
        return true;
    }

    private void end(@Nullable Component announcement) {
        current = null;
        if (bets != null)
            bets.cancel();
        if (session != null)
            session.cleanUp();
        bar.removeAllPlayers();
        for (ServerPlayer player : List.copyOf(level.players()))
            if (visitors.contains(player.getUUID()))
                RaceWorld.leave(player);
        if (announcement != null)
            server.getPlayerList().broadcastSystemMessage(announcement, false);
        PacketDistributor.sendToAllPlayers(RaceMeetingPayload.NONE);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (current == null)
            return;
        RaceMeeting meeting = current;
        current = null;
        if (meeting.stage.ordinal() < Stage.RUNNING.ordinal())
            meeting.refund();
        else
            meeting.award();
        if (meeting.bets != null)
            meeting.bets.cancel();
        if (meeting.session != null)
            meeting.session.cleanUp();
        meeting.bar.removeAllPlayers();
    }

    // ---- showing it

    /** Those who came to watch stay off the course while it's in use, and nobody strays from the grounds: there's only open country out there. */
    private void keepInPlace() {
        RacecourseData data = RacecourseData.get(level);
        TrackGeometry track = data.track();
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (!visitors.contains(player.getUUID()))
                continue;
            if (session.running(player))
                continue;
            Slot slot = slot(player.getUUID());
            boolean runner = slot != null && !slot.scratched;
            double x = player.getX() - data.origin().getX();
            double z = player.getZ() - data.origin().getZ();
            if (!TrackBuilder.onGrounds(track, x, z)) {
                player.displayClientMessage(Component.translatable("umapyoi.meeting.off_the_grounds").withStyle(ChatFormatting.RED), true);
                if (runner && stage.ordinal() < Stage.RUNNING.ordinal())
                    sendToTrack(player);
                else
                    sendToStand(player);
            } else if (!runner && stage.ordinal() <= Stage.RUNNING.ordinal() && track.locate(x, z).offset() < track.outer() + 1) {
                player.displayClientMessage(Component.translatable("umapyoi.meeting.off_the_track").withStyle(ChatFormatting.RED), true);
                sendToStand(player);
            }
        }
    }

    private void updateBar() {
        switch (stage) {
            case OPEN, CLOSED -> {
                // Once the clock is out the runners are told to keep still; when the gates open nobody knows.
                if (stage == Stage.CLOSED && left <= 0)
                    bar.setName(Component.translatable("umapyoi.meeting.bar.gate", name()));
                else
                    bar.setName(Component.translatable(stage == Stage.OPEN ? "umapyoi.meeting.bar.open" : "umapyoi.meeting.bar.closed", name(), clock(left)));
                bar.setProgress(Mth.clamp(left / (float) (UmapyoiConfig.RACE_MEETING_OPEN_SECONDS.get() * 20), 0, 1));
            }
            case RUNNING -> {
                int late = session.stragglerTicksLeft();
                List<RaceSession.Place> places = session.places();
                if (late >= 0) {
                    bar.setName(Component.translatable("umapyoi.meeting.bar.stragglers", name(), clock(late)));
                    bar.setProgress(Mth.clamp(late / (float) Math.max(1, UmapyoiConfig.RACE_MEETING_STRAGGLER_SECONDS.get() * 20), 0, 1));
                } else if (!places.isEmpty() && places.getFirst().time() < 0) {
                    RaceSession.Place lead = places.getFirst();
                    double run = Mth.clamp(lead.progress() / session.distance(), 0, 1);
                    bar.setName(Component.translatable("umapyoi.meeting.bar.running", name(), lead.name(),
                            String.format("%.0f", (1 - run) * setup.distanceMeters())));
                    bar.setProgress((float) run);
                }
            }
            case RESULTS -> {
                bar.setName(Component.translatable("umapyoi.meeting.bar.results", name(), ".".repeat(1 + age / 10 % 3)));
                bar.setProgress(1);
            }
            case LEAVING -> {
                bar.setName(Component.translatable("umapyoi.meeting.bar.leaving", name(), clock(left)));
                bar.setProgress(Mth.clamp(left / (float) Math.max(1, UmapyoiConfig.RACE_MEETING_LEAVE_SECONDS.get() * 20), 0, 1));
            }
            default -> {
            }
        }
    }

    /** To those who watch; the runners have better things to do. */
    private void showLive() {
        RaceLivePayload live = new RaceLivePayload(session.raceTicks(), session.spots());
        for (ServerPlayer player : level.players())
            if (!bound.contains(player.getUUID()))
                PacketDistributor.sendToPlayer(player, live);
    }

    private void firework() {
        RacecourseData data = RacecourseData.get(level);
        TrackGeometry track = data.track();
        RandomSource random = level.getRandom();
        TrackGeometry.Point at = track.pointAt(track.homeStraightStart() + random.nextDouble() * track.straight(), track.inner() - 5 - random.nextDouble() * 6);
        FireworkExplosion.Shape[] shapes = FireworkExplosion.Shape.values();
        DyeColor[] dyes = DyeColor.values();
        FireworkExplosion explosion = new FireworkExplosion(shapes[random.nextInt(shapes.length - 1)],
                it.unimi.dsi.fastutil.ints.IntList.of(dyes[random.nextInt(dyes.length)].getFireworkColor(), dyes[random.nextInt(dyes.length)].getFireworkColor()),
                it.unimi.dsi.fastutil.ints.IntList.of(dyes[random.nextInt(dyes.length)].getFireworkColor()), random.nextBoolean(), random.nextBoolean());
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        rocket.set(DataComponents.FIREWORKS, new Fireworks(1 + random.nextInt(2), List.of(explosion)));
        level.addFreshEntity(new FireworkRocketEntity(level, data.origin().getX() + at.x(), data.origin().getY() + 1.5, data.origin().getZ() + at.z(), rocket));
    }

    /** To everyone in the race world. */
    private void tell(Component message) {
        for (ServerPlayer player : level.players())
            player.sendSystemMessage(message);
    }

    private Component name() {
        return Component.translatable(Util.makeDescriptionId("race", race.id()) + ".name").withStyle(race.ranking().color);
    }

    private Component description() {
        return Component.translatable("umapyoi.meeting.description",
                Component.translatable("race.umapyoi.tier." + race.ranking().name().toLowerCase(java.util.Locale.ROOT)),
                Component.translatable("umapyoi.race.surface." + setup.surface().name().toLowerCase(java.util.Locale.ROOT)),
                String.format("%.0f", setup.distanceMeters()));
    }

    static String clock(int ticks) {
        int seconds = (Math.max(0, ticks) + 19) / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static String minutes(int ticks) {
        return clock(ticks);
    }

    private void sync() {
        PacketDistributor.sendToAllPlayers(payload());
    }

    private RaceMeetingPayload payload() {
        RaceMeetingPayload.Course course = RaceMeetingPayload.Course.NONE;
        if (session != null) {
            TrackGeometry track = RacecourseData.get(level).track();
            course = new RaceMeetingPayload.Course((float) track.straight(), (float) track.radius(), (float) track.width(),
                    (float) track.finish(), (float) session.distance(), (float) session.layout().finalFrom());
        }
        return new RaceMeetingPayload(true, race.id(), applicantName, stage.ordinal(), stage == Stage.RUNNING ? session.raceTicks() : left,
                UmapyoiConfig.RACE_MEETING_CLOSE_SECONDS.get() * 20, (float) setup.distanceMeters(), setup.surface().ordinal(),
                setup.leftHanded(), setup.weather().ordinal(), runnerLevel, course, rows());
    }

    private List<RaceMeetingPayload.Row> rows() {
        List<Slot> all = slots.stream().sorted((a, b) -> Integer.compare(a.lane, b.lane)).toList();
        if (stage.ordinal() < Stage.RUNNING.ordinal())
            refreshSouls();
        List<Integer> favourites = bets == null ? List.of() : bets.favourites();
        List<Slot> running = all.stream().filter(slot -> !slot.scratched).toList();
        List<RacePopularity.Rating> ratings = RacePopularity.rate(running.stream().map(slot -> UmaSoulUtils.getProperty(slot.soul).toArray()).toList());
        List<RaceMeetingPayload.Row> rows = new ArrayList<>();
        for (Slot slot : all) {
            int rated = running.indexOf(slot);
            int place = 0;
            float time = 0;
            for (int i = 0; i < result.size(); i++)
                if (result.get(i).lane() == slot.lane) {
                    place = result.get(i).time() < 0 ? -1 : i + 1;
                    time = (float) result.get(i).time();
                }
            rows.add(new RaceMeetingPayload.Row(slot.lane + 1, UmaSoulUtils.getName(slot.soul), slot.playerName, slot.scratched, form(slot),
                    rated < 0 ? 0 : favourites.contains(slot.lane) ? favourites.indexOf(slot.lane) + 1 : ratings.get(rated).place(), rated < 0 ? new byte[RacePopularity.COLUMNS] : ratings.get(rated).tips(),
                    !result.isEmpty() && place == 0 ? -1 : place, time));
        }
        return rows;
    }

    private RaceMeetingPayload.Form form(Slot slot) {
        int[] stats = UmaSoulUtils.getProperty(slot.soul).toArray();
        byte[] grades = new byte[RaceMeetingPayload.Form.STATS];
        for (int i = 0; i < grades.length; i++)
            grades[i] = (byte) Mth.clamp(stats[i], 0, 39);
        byte[] aptitudes = new byte[RaceMeetingPayload.Form.APTITUDES];
        Distance distance = setup.category();
        aptitudes[RaceMeetingPayload.Form.SURFACE] = (byte) UmaSoulUtils.getSurfaceAptitude(slot.soul)[setup.surface().ordinal()].ordinal();
        aptitudes[RaceMeetingPayload.Form.DISTANCE] = (byte) (distance == null ? Aptitude.C : UmaSoulUtils.getDistanceAptitude(slot.soul)[distance.ordinal()]).ordinal();
        Aptitude[] styles = RaceStyle.aptitudes(slot.soul, level);
        for (int i = 0; i < styles.length; i++)
            aptitudes[RaceMeetingPayload.Form.STYLES + i] = (byte) styles[i].ordinal();
        return new RaceMeetingPayload.Form(grades, aptitudes, (byte) UmaSoulUtils.getMotivation(slot.soul).ordinal(), slot.player == null ? List.of() : List.copyOf(UmaSoulUtils.getSkills(slot.soul)));
    }
}
