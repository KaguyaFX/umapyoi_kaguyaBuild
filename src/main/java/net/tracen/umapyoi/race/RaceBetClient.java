package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.tracen.umapyoi.race.RaceOddsPayload.Line;

/** What this client was last told about its player's horse tickets and the money on the race being held. */
public final class RaceBetClient {
    private static RaceAccountPayload account = RaceAccountPayload.NONE;
    private static RaceOddsPayload odds = RaceOddsPayload.NONE;
    private static long since;

    private RaceBetClient() {
    }

    static void account(RaceAccountPayload payload) {
        account = payload;
    }

    static void odds(RaceOddsPayload payload) {
        odds = payload;
        since = Util.getMillis();
    }

    static void forgetOdds() {
        odds = RaceOddsPayload.NONE;
    }

    public static void reset() {
        account = RaceAccountPayload.NONE;
        forgetOdds();
    }

    public static RaceAccountPayload account() {
        return account;
    }

    public static RaceOddsPayload odds() {
        return odds;
    }

    /** Minutes and seconds until no more bets are taken. */
    public static String clock() {
        return RaceMeeting.clock((int) Math.max(0, odds.left() - (Util.getMillis() - since) / 50));
    }

    @Nullable
    private static Line line(int gate) {
        for (Line line : odds.lines())
            if (line.gate() == gate)
                return line;
        return null;
    }

    private static float money(Line line, boolean place) {
        return place ? line.place() : line.win();
    }

    /** Whether one can bet on this runner at all. */
    public static boolean running(int gate) {
        return odds.ready() && line(gate) != null;
    }

    /** What one gets per ticket staked, if {@code more} were put on her too; 0 if that isn't known. */
    public static double odds(int gate, boolean place, long more) {
        Line line = line(gate);
        if (!odds.ready() || line == null || place && odds.places() == 0)
            return 0;
        double total = more;
        for (Line other : odds.lines())
            total += money(other, place);
        double hers = money(line, place) + more;
        return hers <= 0 ? 0 : Math.max(1, odds.keep() * total / hers / (place ? odds.places() : 1));
    }

    /** Where she is in the crowd's favour (1 = the favourite); 0 if that isn't known. */
    public static int favourite(int gate) {
        Line line = line(gate);
        if (!odds.ready() || line == null || line.win() <= 0)
            return 0;
        int place = 1;
        for (Line other : odds.lines())
            if (other.win() > line.win() || other.win() == line.win() && other.gate() < gate)
                place++;
        return place;
    }

    /** What this player has on her. */
    public static long staked(int gate) {
        long sum = 0;
        for (RaceOddsPayload.Slip slip : odds.mine())
            if (slip.gate() == gate && slip.state() != RaceOddsPayload.BACK)
                sum += slip.amount();
        return sum;
    }
}
