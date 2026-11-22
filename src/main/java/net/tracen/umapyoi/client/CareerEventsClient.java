package net.tracen.umapyoi.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.career.event.CareerEventData;

/**
 * Client-side state of the career event list: open or closed (with a short fade), and which events
 * the player has already seen. "Seen" is only kept in memory; after a restart every event shows as
 * new once more, which is harmless.
 */
@OnlyIn(Dist.CLIENT)
public final class CareerEventsClient {
    static final long FADE_MS = 250;

    private static boolean open;
    private static long changedAt;
    private static final Set<String> seen = new HashSet<>();
    /** Events that were new when the list was opened, marked with a red dot. */
    private static final Set<String> newOnOpen = new HashSet<>();

    private CareerEventsClient() {
    }

    static String key(UUID careerId, CareerEventData.Active event) {
        return careerId + "|" + event.id() + "|" + event.start();
    }

    /** The "show events" key: open or close the list. */
    public static void toggle() {
        open = !open;
        changedAt = Util.getMillis();
        if (open)
            newOnOpen.clear();
    }

    /** How visible the list is right now, 0 to 1, fading in or out. */
    static float visibility() {
        float t = Mth.clamp((Util.getMillis() - changedAt) / (float) FADE_MS, 0F, 1F);
        return open ? t : 1F - t;
    }

    /** While the list is open, everything in it counts as seen; what wasn't seen before gets the dot. */
    static void markSeenIfOpen(UUID careerId, List<CareerEventData.Active> active) {
        if (!open)
            return;
        for (CareerEventData.Active event : active) {
            String key = key(careerId, event);
            if (seen.add(key))
                newOnOpen.add(key);
        }
    }

    static boolean hasUnseen(UUID careerId, List<CareerEventData.Active> active) {
        return active.stream().anyMatch(event -> !seen.contains(key(careerId, event)));
    }

    static boolean isNew(UUID careerId, CareerEventData.Active event) {
        return newOnOpen.contains(key(careerId, event));
    }

    /** True when the worn soul is in a career, so the key does something. */
    public static boolean hasCareer() {
        var player = Minecraft.getInstance().player;
        if (player == null)
            return false;
        return CareerManager.isInCareer(UmapyoiAPI.getUmaSoul(player));
    }
}
