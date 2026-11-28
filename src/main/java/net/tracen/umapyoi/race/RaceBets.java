package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.advancements.trigger.UmapyoiActionTrigger;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * Betting on one race: all the money on it is pooled, a share of it is paid out to those who were right. A crowd
 * that isn't there puts money on everyone by how good their chances are ({@link RaceOdds}), so there is always
 * something to win, and a big stake makes its own odds worse: what one can win is limited by the pool, whatever
 * one does. The crowd never puts more than so much of its money on one runner, or losing on purpose would pay.
 * <p>
 * Win bets pay if the runner is first, place bets if she is one of the first three (two in a small field). Bets
 * on a runner who is replaced or doesn't run go back.
 */
final class RaceBets {
    private static final int FIRST_RUNS = 30;
    /** Tells everyone at most this often. */
    private static final int TELL_EVERY = 10;
    /** For an achievement: more than this on one race, and all of it lost. */
    private static final long ALL_IN = 1000;

    private static final class Bet {
        private final UUID player;
        private final int lane;
        private final boolean place;
        private long amount;
        private byte state = RaceOddsPayload.OPEN;
        private long paid;

        private Bet(UUID player, int lane, boolean place) {
            this.player = player;
            this.lane = lane;
            this.place = place;
        }
    }

    private final MinecraftServer server;
    private final ServerLevel level;
    private final TrackGeometry track;
    private final RaceSetup setup;
    private final RaceSession session;
    private final long pool;
    /** By stall: the crowd's share of the pool on her; not there = she doesn't run. */
    private final Map<Integer, Double> win = new HashMap<>();
    private final Map<Integer, Double> placed = new HashMap<>();
    /** By stall: how long she should take. */
    private final Map<Integer, Double> times = new HashMap<>();
    private final List<Bet> bets = new ArrayList<>();
    /** Counts the fields: what is worked out for an old one is of no use. */
    private final AtomicInteger version = new AtomicInteger();
    private int places;
    private boolean ready;
    private boolean closed;
    private boolean settled;
    private boolean tell;
    private int age;

    RaceBets(MinecraftServer server, ServerLevel level, TrackGeometry track, RaceSetup setup, RaceSession session, long pool) {
        this.server = server;
        this.level = level;
        this.track = track;
        this.setup = setup;
        this.session = session;
        this.pool = pool;
    }

    // ---- the odds

    /** Who runs: works the odds out anew. */
    void field(List<RaceOdds.Entry> entries) {
        int now = version.incrementAndGet();
        ready = false;
        tell = true;
        places = entries.size() >= 8 ? 3 : entries.size() >= 5 ? 2 : 0;
        if (entries.size() < 2)
            return;
        RaceOdds.workOut(level, track, setup, session.layout(), session.distance(), entries, places, FIRST_RUNS, Math.max(FIRST_RUNS, UmapyoiConfig.RACE_BET_RUNS.get()),
                level.getRandom().nextLong(), run -> version.get() != now, chances -> server.execute(() -> {
                    if (version.get() == now && !closed)
                        know(entries, chances);
                }));
    }

    private void know(List<RaceOdds.Entry> entries, RaceOdds.Chances chances) {
        win.clear();
        placed.clear();
        times.clear();
        double most = UmapyoiConfig.RACE_BET_CROWD_MAX.get();
        double[] wins = capped(chances.win(), chances.rest(), most);
        double[] seconds = capped(chances.placed(), chances.placed(), places == 0 ? 1 : Math.max(1.0 / places, most));
        for (int i = 0; i < entries.size(); i++) {
            int lane = entries.get(i).lane();
            win.put(lane, wins[i]);
            placed.put(lane, seconds[i]);
            times.put(lane, chances.time()[i]);
        }
        ready = true;
        tell = true;
    }

    /**
     * As shares of the whole, none above {@code most}.
     *
     * @param others how the rest is divided up then: by how good they are when the favourite doesn't win
     */
    private static double[] capped(double[] chances, double[] others, double most) {
        double[] shares = chances.clone();
        double sum = 0;
        for (double share : shares)
            sum += share;
        if (sum <= 0)
            return shares;
        int top = 0;
        for (int i = 0; i < shares.length; i++) {
            shares[i] /= sum;
            if (shares[i] > shares[top])
                top = i;
        }
        if (shares[top] <= most)
            return shares;
        double rest = 0;
        for (int i = 0; i < shares.length; i++)
            if (i != top)
                rest += others[i];
        for (int i = 0; i < shares.length; i++)
            shares[i] = i == top ? most : rest <= 0 ? (1 - most) / (shares.length - 1) : (1 - most) * others[i] / rest;
        return shares;
    }

    private double money(int lane, boolean place) {
        Double share = (place ? placed : win).get(lane);
        if (share == null)
            return 0;
        double money = share * pool;
        for (Bet bet : bets)
            if (bet.lane == lane && bet.place == place && bet.state != RaceOddsPayload.BACK)
                money += bet.amount;
        return money;
    }

    private double total(boolean place) {
        double total = 0;
        for (int lane : win.keySet())
            total += money(lane, place);
        return total;
    }

    /** What is paid per ticket on a runner who is right; never less than the stake. */
    private double odds(int lane, boolean place) {
        double money = money(lane, place);
        return money <= 0 || place && places == 0 ? 0
                : Math.max(1, UmapyoiConfig.RACE_BET_KEEP.get() * total(place) / money / (place ? places : 1));
    }

    /** The stalls by how much is on them to win; empty while that isn't known. */
    List<Integer> favourites() {
        return !ready ? List.of() : win.keySet().stream().sorted((a, b) -> {
            int order = Double.compare(money(b, false), money(a, false));
            return order != 0 ? order : Integer.compare(a, b);
        }).toList();
    }

    // ---- bets

    boolean open() {
        return ready && !closed;
    }

    /** @return why not, or null */
    @Nullable
    Component bet(ServerPlayer player, int lane, boolean place, long amount) {
        if (closed)
            return Component.translatable("umapyoi.ticket.problem.closed");
        if (!ready || !win.containsKey(lane) || place && places == 0)
            return Component.translatable("umapyoi.ticket.problem.not_ready");
        int unit = UmapyoiConfig.RACE_BET_UNIT.get();
        if (amount < unit || amount % unit != 0)
            return Component.translatable("umapyoi.ticket.problem.unit", unit);
        if (!RaceAccounts.get(server).stake(server, player.getUUID(), amount))
            return Component.translatable("umapyoi.ticket.problem.balance");
        Bet bet = null;
        for (Bet other : bets)
            if (other.player.equals(player.getUUID()) && other.lane == lane && other.place == place && other.state == RaceOddsPayload.OPEN)
                bet = other;
        if (bet == null) {
            bet = new Bet(player.getUUID(), lane, place);
            bets.add(bet);
        }
        bet.amount += amount;
        tell = true;
        UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.BET);
        return null;
    }

    /** The runner in this stall isn't the one that was bet on any more. */
    void giveBack(int lane) {
        for (Bet bet : bets)
            if (bet.lane == lane)
                giveBack(bet);
    }

    private void giveBack(Bet bet) {
        if (bet.state != RaceOddsPayload.OPEN)
            return;
        bet.state = RaceOddsPayload.BACK;
        bet.paid = bet.amount;
        RaceAccounts.get(server).settle(server, bet.player, bet.amount, bet.amount);
        ServerPlayer player = server.getPlayerList().getPlayer(bet.player);
        if (player != null)
            player.sendSystemMessage(Component.translatable("umapyoi.ticket.back", bet.amount, bet.lane + 1).withStyle(ChatFormatting.YELLOW));
        tell = true;
    }

    /** She doesn't run: nothing is on her any more. */
    void scratch(int lane) {
        giveBack(lane);
        win.remove(lane);
        placed.remove(lane);
        tell = true;
    }

    /** No more bets. */
    void close() {
        if (closed)
            return;
        closed = true;
        version.incrementAndGet();
        tell = true;
    }

    boolean closed() {
        return closed;
    }

    /** Somebody has something on the race. Leaving before one's runner has lost doesn't get it back. */
    boolean any() {
        return bets.stream().anyMatch(bet -> bet.state == RaceOddsPayload.OPEN);
    }

    /** Something about the time left is different. */
    void touch() {
        tell = true;
    }

    /** The race is off. */
    void cancel() {
        if (settled)
            return;
        settled = true;
        closed = true;
        version.incrementAndGet();
        bets.forEach(this::giveBack);
    }

    /** The result is out. */
    void settle(List<RaceSession.Place> result) {
        if (settled)
            return;
        List<Integer> home = result.stream().filter(place -> place.time() >= 0).map(RaceSession.Place::lane).toList();
        if (home.isEmpty() || !ready) {
            cancel();
            return;
        }
        settled = true;
        Map<UUID, long[]> sums = new LinkedHashMap<>();
        for (Bet bet : bets) {
            if (bet.state != RaceOddsPayload.OPEN)
                continue;
            int at = home.indexOf(bet.lane);
            boolean right = at >= 0 && at < (bet.place ? places : 1);
            bet.state = right ? RaceOddsPayload.WON : RaceOddsPayload.LOST;
            bet.paid = right ? (long) Math.floor(bet.amount * odds(bet.lane, bet.place) + 1e-6) : 0;
        }
        for (Bet bet : bets) {
            if (bet.state != RaceOddsPayload.WON && bet.state != RaceOddsPayload.LOST)
                continue;
            RaceAccounts.get(server).settle(server, bet.player, bet.amount, bet.paid);
            long[] sum = sums.computeIfAbsent(bet.player, key -> new long[2]);
            sum[0] += bet.amount;
            sum[1] += bet.paid;
        }
        sums.forEach((id, sum) -> {
            RaceAccounts.get(server).result(server, id, sum[1] - sum[0]);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("umapyoi.ticket.result", sum[0], sum[1]).withStyle(sum[1] > sum[0] ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                if (sum[1] > sum[0])
                    UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.BET_PROFIT);
                if (sum[1] == 0 && sum[0] > ALL_IN)
                    UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.BET_LOST_ALL);
            }
        });
        tell = true;
    }

    /** How long the runner in this stall should have taken, or below zero if that was never worked out. */
    double expected(int lane) {
        return times.getOrDefault(lane, -1.0);
    }

    // ---- telling

    /** @param left ticks until no more bets are taken */
    void tick(int left) {
        if (++age % TELL_EVERY == 0 && tell) {
            tell = false;
            for (ServerPlayer player : level.players())
                tell(player, left);
        }
    }

    /** To someone who came just now, or when it's over. */
    void tell(ServerPlayer player, int left) {
        List<RaceOddsPayload.Line> lines = new ArrayList<>();
        for (int lane : win.keySet().stream().sorted().toList())
            lines.add(new RaceOddsPayload.Line(lane + 1, (float) money(lane, false), (float) money(lane, true)));
        List<RaceOddsPayload.Slip> mine = bets.stream().filter(bet -> bet.player.equals(player.getUUID()))
                .map(bet -> new RaceOddsPayload.Slip(bet.lane + 1, bet.place, bet.amount, bet.state, bet.paid)).toList();
        PacketDistributor.sendToPlayer(player, new RaceOddsPayload(ready, !closed, Math.max(0, left), places, UmapyoiConfig.RACE_BET_KEEP.get().floatValue(),
                UmapyoiConfig.RACE_BET_UNIT.get(), lines, mine));
    }
}
