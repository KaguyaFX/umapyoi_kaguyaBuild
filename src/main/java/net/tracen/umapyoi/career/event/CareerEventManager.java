package net.tracen.umapyoi.career.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.utils.UmaStatusUtils;

/**
 * The per-turn event system (design doc ch. 9). When a career starts it gets an event table. At the
 * start of every turn, events past their deadline are settled, then one event may come up: the one
 * fixed to this turn, otherwise a roll draws one from the table. Several can be in progress at once.
 */
public final class CareerEventManager {
    private CareerEventManager() {
    }

    /** Everything this career can meet, for a new career (or one started before events existed). */
    public static CareerEventData buildTable(ItemStack soul) {
        List<CareerEventData.Entry> table = new ArrayList<>();
        for (CareerEventType type : CareerEventType.values()) {
            if (type.belongsIn(soul))
                table.add(new CareerEventData.Entry(type.id(), type.maxCount()));
        }
        return new CareerEventData(table, List.of(), List.of());
    }

    /** Called by CareerManager when the worn soul's career reaches a new turn. */
    public static void onNewTurn(ServerPlayer player, ItemStack soul, int turn) {
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        if (data == null)
            data = buildTable(soul);

        // Settle what ran out. Failing a mandatory one costs motivation and stats.
        List<CareerEventData.Active> active = new ArrayList<>();
        List<CareerEventType> failed = new ArrayList<>();
        for (CareerEventData.Active event : data.active()) {
            if (event.deadline() > turn) {
                active.add(event);
                continue;
            }
            CareerEventType.byId(event.id()).filter(t -> t.kind() == CareerEventType.Kind.MANDATORY)
                    .ifPresent(failed::add);
        }

        // At most one new event per turn.
        List<CareerEventData.Entry> table = new ArrayList<>(data.table());
        CareerEventType next = scheduledFor(data, turn);
        if (next == null && player.getRandom().nextDouble() < UmapyoiConfig.EVENT_CHANCE_PER_TURN.get())
            next = draw(player, table, active);
        if (next != null) {
            active.add(new CareerEventData.Active(next.id(), turn, turn + next.turns(), next.noteWhenAppearing(player)));
            for (int i = 0; i < table.size(); i++) {
                if (table.get(i).id().equals(next.id()))
                    table.set(i, new CareerEventData.Entry(next.id(), Math.max(0, table.get(i).left() - 1)));
            }
        }
        soul.set(DataComponentsTypeRegistry.CAREER_EVENTS, new CareerEventData(table, data.scheduled(), active));

        for (CareerEventType type : failed)
            fail(player, soul, type);
        if (next != null)
            player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    @Nullable
    private static CareerEventType scheduledFor(CareerEventData data, int turn) {
        for (CareerEventData.Scheduled scheduled : data.scheduled()) {
            if (scheduled.turn() == turn)
                return CareerEventType.byId(scheduled.id()).orElse(null);
        }
        return null;
    }

    /** Weighted draw among events that are left, not already in progress, and doable right now. */
    @Nullable
    private static CareerEventType draw(ServerPlayer player, List<CareerEventData.Entry> table,
            List<CareerEventData.Active> active) {
        List<CareerEventType> candidates = new ArrayList<>();
        int total = 0;
        for (CareerEventData.Entry entry : table) {
            Optional<CareerEventType> type = CareerEventType.byId(entry.id());
            if (entry.left() <= 0 || type.isEmpty() || type.get().weight() <= 0)
                continue;
            if (active.stream().anyMatch(a -> a.id().equals(entry.id())) || !type.get().canAppear(player))
                continue;
            candidates.add(type.get());
            total += type.get().weight();
        }
        if (total <= 0)
            return null;
        RandomSource random = player.getRandom();
        int pick = random.nextInt(total);
        for (CareerEventType type : candidates) {
            pick -= type.weight();
            if (pick < 0)
                return type;
        }
        return null;
    }

    /** Whether the worn soul has this event in progress right now. */
    public static boolean isActive(ServerPlayer player, CareerEventType type) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (!CareerManager.isInCareer(soul))
            return false;
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        return data != null && data.active().stream().anyMatch(a -> a.id().equals(type.id()));
    }

    /** The player did it: the event goes away and motivation rises one step. */
    public static void complete(ServerPlayer player, CareerEventType type) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (!CareerManager.isInCareer(soul) || !remove(soul, type))
            return;
        UmaStatusUtils.changeMotivation(player, 1);
        player.displayClientMessage(Component.translatable("umapyoi.career.event.completed", type.displayName()), true);
        player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    /** It can't be done any more (dawn has passed and the like): it goes away with no penalty. */
    public static void cancel(ServerPlayer player, CareerEventType type) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (!CareerManager.isInCareer(soul) || !remove(soul, type))
            return;
        player.displayClientMessage(Component.translatable("umapyoi.career.event.cancelled", type.displayName()), true);
    }

    private static boolean remove(ItemStack soul, CareerEventType type) {
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        if (data == null)
            return false;
        List<CareerEventData.Active> active = new ArrayList<>(data.active());
        if (!active.removeIf(a -> a.id().equals(type.id())))
            return false;
        soul.set(DataComponentsTypeRegistry.CAREER_EVENTS, new CareerEventData(data.table(), data.scheduled(), active));
        return true;
    }

    /** A mandatory event ran out: motivation down one step and every stat loses a little. */
    private static void fail(ServerPlayer player, ItemStack soul, CareerEventType type) {
        UmaStatusUtils.changeMotivation(player, -1);
        CareerManager.penalize(player, soul, UmapyoiConfig.EVENT_FAIL_KEEP_PERMILLE.get());
        player.displayClientMessage(Component.translatable("umapyoi.career.event.failed", type.displayName()), true);
        player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.8F, 0.8F);
    }
}
