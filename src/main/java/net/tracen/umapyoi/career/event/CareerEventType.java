package net.tracen.umapyoi.career.event;

import java.util.Arrays;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;

/**
 * Every event a career can have (design doc ch. 9). A career's event table is built from these when
 * it starts; each turn may draw one.
 */
public enum CareerEventType {
    /** 想跑一跑: sprint for a while without stopping. */
    WANT_TO_RUN("want_to_run", Kind.OPTIONAL, 3, 10),
    /** 肚子餓了: eat anything. Only appears when not full, so it can always be done. */
    HUNGRY("hungry", Kind.MANDATORY, 3, 10),
    /** 想吃胡蘿蔔: eat a carrot or something made from one. */
    WANT_CARROT("want_carrot", Kind.OPTIONAL, 2, 10),
    /** 想喝蜂蜜特飲: needs the item, so it can only be optional. */
    WANT_HACHIMI("want_hachimi", Kind.OPTIONAL, 2, 10),
    /** 想換套衣服: wear a different outfit in the outfit slot than when it came up. */
    CHANGE_OUTFIT("change_outfit", Kind.OPTIONAL, 2, 10),
    /** 想去別的地方看看: be in another biome than where it came up. */
    OTHER_BIOME("other_biome", Kind.OPTIONAL, 2, 10),
    /** 想看日出: only comes up a little before dawn; gone if dawn passes. */
    WATCH_SUNRISE("watch_sunrise", Kind.OPTIONAL, 1, 5),
    /** 想看夕陽: only comes up a little before dusk; gone if dusk passes. */
    WATCH_SUNSET("watch_sunset", Kind.OPTIONAL, 1, 5);

    public enum Kind {
        OPTIONAL("optional"), MANDATORY("mandatory"), STATE("state"), NOTICE("notice");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public Component label() {
            return Component.translatable("umapyoi.career.event.kind." + id);
        }
    }

    /**
     * Dawn and dusk in day time ticks (0-23999), and how long before them the event may come up. The
     * whole wait plus the window fits in an optional event's time.
     */
    public enum SkyWindow {
        SUNRISE(22500, 24000), SUNSET(12000, 13500);

        static final int LEAD = 3000;
        final int start;
        final int end;

        SkyWindow(int start, int end) {
            this.start = start;
            this.end = end;
        }

        static boolean hasDayCycle(Level level) {
            return !level.dimensionType().hasFixedTime();
        }

        boolean comingSoon(Level level) {
            long time = level.getDayTime() % 24000L;
            return hasDayCycle(level) && time >= start - LEAD && time < start;
        }

        /** Absolute day time when the window that is coming up ends. */
        long endAfter(Level level) {
            long day = level.getDayTime() - level.getDayTime() % 24000L;
            return day + end;
        }

        boolean isNow(Level level) {
            long time = level.getDayTime() % 24000L;
            return hasDayCycle(level) && time >= start && time < end;
        }
    }

    private final String id;
    private final Kind kind;
    private final int maxCount;
    private final int weight;

    CareerEventType(String id, Kind kind, int maxCount, int weight) {
        this.id = id;
        this.kind = kind;
        this.maxCount = maxCount;
        this.weight = weight;
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    /** How many times it may come up in one career. */
    public int maxCount() {
        return maxCount;
    }

    /** Draw weight against the other events in the table. */
    public int weight() {
        return weight;
    }

    /** Turns the player has to do it. */
    public int turns() {
        return kind == Kind.MANDATORY ? UmapyoiConfig.EVENT_MANDATORY_TURNS.get() : UmapyoiConfig.EVENT_OPTIONAL_TURNS.get();
    }

    /** Whether it goes into this soul's event table at the start of the career. */
    public boolean belongsIn(ItemStack soul) {
        return true; // general events: every career
    }

    /** Checked when drawn: only events that can be done right now come up. */
    public boolean canAppear(ServerPlayer player) {
        return switch (this) {
            case HUNGRY -> player.getFoodData().getFoodLevel() <= UmapyoiConfig.EVENT_HUNGRY_MAX_FOOD.get();
            case WATCH_SUNRISE -> SkyWindow.SUNRISE.comingSoon(player.level());
            case WATCH_SUNSET -> SkyWindow.SUNSET.comingSoon(player.level());
            default -> true;
        };
    }

    /** What the event notes when it comes up, to compare with later. */
    public String noteWhenAppearing(ServerPlayer player) {
        return switch (this) {
            case CHANGE_OUTFIT -> outfitOf(player);
            case OTHER_BIOME -> biomeOf(player);
            case WATCH_SUNRISE -> String.valueOf(SkyWindow.SUNRISE.endAfter(player.level()));
            case WATCH_SUNSET -> String.valueOf(SkyWindow.SUNSET.endAfter(player.level()));
            default -> "";
        };
    }

    /** The outfit slot's content as a comparable key; "" when empty. */
    static String outfitOf(ServerPlayer player) {
        ItemStack suit = UmapyoiAPI.getUmaSuit(player);
        return suit.isEmpty() ? "" : String.valueOf(ItemStack.hashItemAndComponents(suit));
    }

    static String biomeOf(ServerPlayer player) {
        return player.level().getBiome(BlockPos.containing(player.position())).unwrapKey()
                .map(key -> key.location().toString()).orElse("");
    }

    public Component displayName() {
        return Component.translatable("umapyoi.career.event." + id);
    }

    public Component hint() {
        return Component.translatable("umapyoi.career.event." + id + ".hint");
    }

    public static Optional<CareerEventType> byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id.equals(id)).findFirst();
    }
}
