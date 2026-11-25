package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * How likely everyone is to win: found out by running the race many times over, off to the side, with what can
 * be known of the runners. Players are run as if a computer ran them, with what the crowd has seen of them added
 * ({@link RaceAccounts#edge}).
 */
final class RaceOdds {
    /** Long enough for anything that is still running. */
    private static final int TIME_LIMIT_TICKS = 20 * 600;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Umapyoi race odds");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    /**
     * A runner.
     *
     * @param edge seconds per kilometre she takes longer than worked out
     */
    record Entry(int lane, ItemStack soul, double trim, boolean player, double edge) {
    }

    /**
     * By entry.
     *
     * @param win    the chance to win
     * @param rest   the chance to be first of all but the likeliest winner
     * @param placed the chance to be placed
     * @param time   the time without {@link Entry#edge}, on average
     */
    record Chances(double[] win, double[] rest, double[] placed, double[] time) {
    }

    private RaceOdds() {
    }

    /**
     * Works them out on a thread of its own.
     *
     * @param places how many are placed
     * @param first  a first idea is ready after this many runs
     * @param stop   asked now and then, with how many runs are done, whether nobody wants to know any more
     * @param found  told twice, on that thread
     */
    static void workOut(ServerLevel level, TrackGeometry track, RaceSetup setup, RaceLayout layout, double distance, List<Entry> entries,
            int places, int first, int runs, long seed, IntPredicate stop, Consumer<Chances> found) {
        WORKER.execute(() -> {
            try {
                int field = entries.size();
                int[] wins = new int[field];
                int[] placed = new int[field];
                int[][] leaders = new int[runs][];
                double[] times = new double[field];
                double kilometres = setup.distanceMeters() / 1000;
                for (int run = 0; run < runs; run++) {
                    if (stop.test(run))
                        return;
                    double[] time = run(level, track, setup, layout, distance, entries, RandomSource.create(seed + run * 7919L));
                    List<Integer> order = new ArrayList<>(IntStream.range(0, field).boxed().toList());
                    for (int i = 0; i < field; i++)
                        times[i] += time[i];
                    order.sort(Comparator.comparingDouble(i -> time[i] + entries.get(i).edge() * kilometres));
                    for (int i = 0; i < field; i++) {
                        if (i == 0)
                            wins[order.get(i)]++;
                        if (i < places)
                            placed[order.get(i)]++;
                    }
                    leaders[run] = new int[] { order.get(0), order.get(1) };
                    if (run + 1 == first || run + 1 == runs)
                        found.accept(chances(wins, leaders, placed, times, places, run + 1));
                }
            } catch (RuntimeException e) {
                Umapyoi.getLogger().error("Couldn't work out the odds of a race", e);
            }
        });
    }

    /** Nobody is without any chance. */
    private static Chances chances(int[] wins, int[][] leaders, int[] placed, double[] times, int places, int runs) {
        int field = wins.length;
        int likeliest = 0;
        for (int i = 1; i < field; i++)
            if (wins[i] > wins[likeliest])
                likeliest = i;
        int[] others = new int[field];
        for (int run = 0; run < runs; run++)
            others[leaders[run][0] == likeliest ? leaders[run][1] : leaders[run][0]]++;
        double[] win = new double[field], rest = new double[field], place = new double[field], time = new double[field];
        for (int i = 0; i < field; i++) {
            win[i] = (wins[i] + 0.5) / (runs + 0.5 * field);
            rest[i] = i == likeliest ? 0 : (others[i] + 0.5) / (runs + 0.5 * (field - 1));
            place[i] = places == 0 ? 0 : (placed[i] + 0.5 * places) / (runs + 0.5 * field);
            time[i] = times[i] / runs;
        }
        return new Chances(win, rest, place, time);
    }

    /** One race. @return everyone's time */
    private static double[] run(ServerLevel level, TrackGeometry track, RaceSetup setup, RaceLayout layout, double distance, List<Entry> entries, RandomSource random) {
        List<RaceOpponent> field = new ArrayList<>();
        for (Entry entry : entries) {
            double stall = RaceSession.stallOffset(track, entry.lane());
            field.add(entry.player() ? RaceOpponent.of(level, track, setup, layout, distance, RaceSession.PLAIN_SPRINT, entry.soul(), 1, 1.0, stall, random)
                    : RaceOpponent.of(level, track, setup, layout, distance, RaceSession.PLAIN_SPRINT, entry.soul(), entry.trim(), stall, random));
        }
        for (RaceOpponent runner : field)
            runner.plan(setup, RaceSession.PLAIN_SPRINT, track);
        for (int tick = 1; tick < TIME_LIMIT_TICKS && !field.stream().allMatch(RaceOpponent::finished); tick++) {
            List<RaceTraffic.Body> all = new ArrayList<>();
            List<RaceRunner> running = new ArrayList<>();
            for (RaceOpponent runner : field)
                if (!runner.finished()) {
                    all.add(runner.body());
                    if (runner.runner().started())
                        running.add(runner.runner());
                }
            RaceSession.chargeUp(running);
            for (RaceOpponent runner : field)
                runner.tick(track, distance, tick, RaceSession.START, all);
        }
        return field.stream().mapToDouble(runner -> runner.finished() ? runner.time() : TIME_LIMIT_TICKS / 20.0).toArray();
    }
}
