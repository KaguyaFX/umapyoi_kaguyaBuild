package net.tracen.umapyoi.race;

import java.util.List;

import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.race.track.TrackGeometry.Section;

/**
 * One race laid on the course: it starts at the finish line (course progress 0) and runs
 * {@code distance} blocks, possibly over several laps. It answers "which part of the race is
 * this?" for a distance run, the way the original game names them:
 * <ul>
 * <li>Phases by distance: {@link RacePhase}.</li>
 * <li>Corners are numbered counting back from the finish: the last corner of the race is always 4,
 * the ones before it 3, 2, 1, then 4 again. Each half of a curve is one corner.</li>
 * <li>The final straight is the last straight of the race, whatever comes after it. If the race
 * ends on a straight, it runs from that straight's start to the finish.</li>
 * </ul>
 * Distances here are race progress: blocks run along the course from the start.
 */
public final class RaceLayout {
    private static final List<Section> CORNERS = List.of(Section.TURN_1, Section.TURN_2, Section.TURN_3, Section.TURN_4);
    private static final List<Section> STRAIGHTS = List.of(Section.BACK_STRAIGHT, Section.HOME_STRAIGHT);

    private final TrackGeometry track;
    private final double distance;
    private final int lastCorner;
    private final double finalStraightFrom;
    private final double finalStraightTo;

    public RaceLayout(TrackGeometry track, double distance) {
        this.track = track;
        this.distance = distance;
        this.lastCorner = cornersStartedBy(distance);

        // The last straight that starts before the finish (the start itself is on the home straight).
        double lap = track.lap();
        double best = Double.NEGATIVE_INFINITY;
        for (Section straight : STRAIGHTS) {
            double start = track.sectionStart(straight);
            for (int laps = -1; start + laps * lap <= distance; laps++)
                best = Math.max(best, start + laps * lap);
        }
        this.finalStraightFrom = best;
        this.finalStraightTo = Math.min(best + track.straight(), distance);
    }

    /** Where the final leg starts. */
    public double finalFrom() {
        return Math.max(1, distance * UmapyoiConfig.RACE_PHASE_FINAL_FROM.get());
    }

    public RacePhase phase(double progress) {
        return RacePhase.of(progress, distance);
    }

    /** The translation key naming where on the course this part of the race is. */
    public String sectionKey(double progress) {
        if (progress >= finalStraightFrom && progress < finalStraightTo)
            return "umapyoi.race.section.final_straight";
        Section section = track.section(track.wrap(progress));
        if (section.straight())
            return section.key();
        int number = 4 - Math.floorMod(lastCorner - cornersStartedBy(progress), 4);
        return "umapyoi.race.section.turn_" + number;
    }

    /** How many corners the race has entered by this point. */
    private int cornersStartedBy(double progress) {
        double lap = track.lap();
        int count = 0;
        for (Section corner : CORNERS) {
            double start = track.sectionStart(corner);
            if (progress >= start)
                count += (int) Math.floor((progress - start) / lap) + 1;
        }
        return count;
    }
}
