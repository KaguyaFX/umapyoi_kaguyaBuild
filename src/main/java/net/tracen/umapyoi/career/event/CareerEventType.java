package net.tracen.umapyoi.career.event;

import java.util.Arrays;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.UmapyoiConfig;

/**
 * Every event a career can have (design doc ch. 9). A career's event table is built from these when
 * it starts; each turn may draw one. Step 1 has three general whims to test the system with.
 */
public enum CareerEventType {
    /** 想跑一跑: sprint for a while without stopping. */
    WANT_TO_RUN("want_to_run", Kind.OPTIONAL, 3, 10),
    /** 肚子餓了: eat anything. Only appears when not full, so it can always be done. */
    HUNGRY("hungry", Kind.MANDATORY, 3, 10),
    /** 想吃胡蘿蔔: eat a carrot or something made from one. */
    WANT_CARROT("want_carrot", Kind.OPTIONAL, 2, 10);

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
            default -> true;
        };
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
