package net.tracen.umapyoi.career.event;

/** Short-lived progress toward events, per player, in memory only (a streak just restarts). */
public class EventProgress {
    public int sprintSeconds;
    public int horseSeconds;
    public int companySeconds;
    public int lightSeconds;
    public int indoorSeconds;
    /** Experience level at the last check, to notice levels spent; -1 = not known yet. */
    public int lastXpLevel = -1;
}
