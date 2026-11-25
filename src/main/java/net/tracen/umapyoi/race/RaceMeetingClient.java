package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.tracen.umapyoi.race.RaceLivePayload.Spot;

/**
 * What this client was last told about the race being held. Plain data, so it can live outside the client
 * package; only the client ever fills it.
 */
public final class RaceMeetingClient {
    /** Where everyone is comes this often; what's shown glides from one to the next. */
    private static final double LIVE_EVERY_MILLIS = RaceMeeting.LIVE_EVERY * 50;

    @Nullable
    private static RaceMeetingPayload current;
    private static long since;
    @Nullable
    private static RaceLivePayload live;
    @Nullable
    private static RaceLivePayload before;
    private static long liveSince;
    /** The gates in the order they lie. It stands still once the first is home: the rest is for the result. */
    private static List<Integer> order = List.of();
    private static boolean home;

    private RaceMeetingClient() {
    }

    static void set(RaceMeetingPayload payload) {
        current = payload.active() ? payload : null;
        since = Util.getMillis();
        if (current == null)
            RaceBetClient.forgetOdds();
        if (current == null || current.stageNow().ordinal() < RaceMeeting.Stage.RUNNING.ordinal())
            forgetLive();
    }

    static void live(RaceLivePayload payload) {
        before = live;
        live = payload;
        liveSince = Util.getMillis();
        home |= payload.spots().stream().anyMatch(spot -> spot.state() == RaceLivePayload.HOME);
        if (!home || order.isEmpty())
            order = payload.spots().stream().filter(spot -> spot.state() != RaceLivePayload.OUT)
                    .sorted(Comparator.comparingDouble(Spot::progress).reversed()).map(Spot::gate).toList();
    }

    private static void forgetLive() {
        live = null;
        before = null;
        order = List.of();
        home = false;
    }

    public static void reset() {
        current = null;
        forgetLive();
    }

    /** The race being held, or null when there's none. */
    @Nullable
    public static RaceMeetingPayload current() {
        return current;
    }

    private static int passed() {
        return (int) ((Util.getMillis() - since) / 50);
    }

    /** Ticks left of what {@link RaceMeetingPayload#left()} counts, by now. */
    public static int left() {
        return current == null ? 0 : Math.max(0, current.left() - passed());
    }

    /** The same as minutes and seconds. */
    public static String clock() {
        return RaceMeeting.clock(left());
    }

    /** Seconds since the gates opened, while the race is run. */
    public static double raceClock() {
        return current == null ? 0 : (current.left() + passed()) / 20.0;
    }

    /** The race clock, as minutes, seconds and tenths. */
    public static String raceClockText() {
        double seconds = raceClock();
        int minutes = (int) (seconds / 60);
        return String.format("%d:%04.1f", minutes, seconds - minutes * 60);
    }

    /** Whether this client is told where everyone is. */
    public static boolean watching() {
        return live != null;
    }

    /** The first has crossed the line. */
    public static boolean home() {
        return home;
    }

    /** The stalls' numbers in the order their runners lie; empty when not watching. */
    public static List<Integer> order() {
        return order;
    }

    /** Where everyone is, gliding between what the server says. */
    public static List<Spot> spots() {
        if (live == null)
            return List.of();
        if (before == null)
            return live.spots();
        float part = (float) Math.min(1, (Util.getMillis() - liveSince) / LIVE_EVERY_MILLIS);
        List<Spot> spots = new ArrayList<>();
        for (Spot now : live.spots()) {
            Spot then = before.spots().stream().filter(spot -> spot.gate() == now.gate()).findFirst().orElse(now);
            spots.add(new Spot(now.gate(), then.progress() + (now.progress() - then.progress()) * part,
                    then.offset() + (now.offset() - then.offset()) * part, now.state()));
        }
        return spots;
    }

    /** Metres the first still has to go. */
    public static int metersLeft() {
        if (current == null || live == null || current.course().distance() <= 0)
            return 0;
        double lead = 0;
        for (Spot spot : spots())
            if (spot.state() != RaceLivePayload.OUT)
                lead = Math.max(lead, spot.progress());
        return (int) Math.max(0, Math.ceil((1 - lead / current.course().distance()) * current.meters()));
    }
}
