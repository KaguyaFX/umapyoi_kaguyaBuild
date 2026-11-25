package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Whom the crowd fancies, by the original game's three values: speed with guts, stamina with guts and
 * wisdom, power with wisdom, each as a deviation value (50 is average, 10 a standard deviation) within the
 * field. The best five in each get a tip. How the original adds the three up isn't known; here it's just
 * their sum.
 */
final class RacePopularity {
    /** A runner's tip in one of the three columns. */
    static final byte NO_TIP = 0;
    private static final byte[] TIPS = { 1, 2, 3, 4, 4 };
    static final int COLUMNS = 3;

    /**
     * @param place 1 = the favourite
     * @param tips  one per column: 1 the best, 2, 3, 4 (the fourth and the fifth), or {@link #NO_TIP}
     */
    record Rating(int place, byte[] tips) {
    }

    private RacePopularity() {
    }

    /** @param stats speed, stamina, power, guts and wisdom of each runner */
    static List<Rating> rate(List<int[]> stats) {
        int field = stats.size();
        double[] guts = deviation(column(stats, 3));
        double[] wisdom = deviation(column(stats, 4));
        double[][] values = {
                deviation(IntStream.range(0, field).mapToDouble(i -> stats.get(i)[0] * Math.sqrt(guts[i])).toArray()),
                deviation(IntStream.range(0, field).mapToDouble(i -> stats.get(i)[1] * Math.pow(guts[i], 0.8) * Math.pow(wisdom[i], 0.6)).toArray()),
                deviation(IntStream.range(0, field).mapToDouble(i -> stats.get(i)[2] * Math.sqrt(wisdom[i])).toArray()) };
        byte[][] tips = new byte[field][COLUMNS];
        double[] total = new double[field];
        for (int column = 0; column < COLUMNS; column++) {
            double[] value = values[column];
            List<Integer> order = order(value);
            for (int i = 0; i < Math.min(TIPS.length, field); i++)
                tips[order.get(i)][column] = TIPS[i];
            for (int i = 0; i < field; i++)
                total[i] += value[i];
        }
        int[] place = new int[field];
        List<Integer> order = order(total);
        for (int i = 0; i < field; i++)
            place[order.get(i)] = i + 1;
        List<Rating> ratings = new ArrayList<>();
        for (int i = 0; i < field; i++)
            ratings.add(new Rating(place[i], tips[i]));
        return ratings;
    }

    /** The runners from the highest to the lowest. */
    private static List<Integer> order(double[] value) {
        return IntStream.range(0, value.length).boxed().sorted(Comparator.comparingDouble((Integer i) -> -value[i])).toList();
    }

    private static double[] column(List<int[]> stats, int column) {
        return stats.stream().mapToDouble(row -> row[column]).toArray();
    }

    private static double[] deviation(double[] values) {
        double mean = IntStream.range(0, values.length).mapToDouble(i -> values[i]).average().orElse(0);
        double variance = IntStream.range(0, values.length).mapToDouble(i -> (values[i] - mean) * (values[i] - mean)).average().orElse(0);
        double sd = Math.sqrt(variance);
        return IntStream.range(0, values.length).mapToDouble(i -> sd < 1e-9 ? 50 : Math.max(1, 50 + (values[i] - mean) / sd * 10)).toArray();
    }
}
