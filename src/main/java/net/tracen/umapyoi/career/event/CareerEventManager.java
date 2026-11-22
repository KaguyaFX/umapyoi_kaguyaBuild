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
import net.tracen.umapyoi.trait.PersonalityEffects;
import net.tracen.umapyoi.trait.PersonalityTrait;
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

        // Settle what ran out. Failing a mandatory one costs motivation and stats; the rest just end.
        List<CareerEventData.Active> active = new ArrayList<>();
        List<CareerEventData.Active> failed = new ArrayList<>();
        for (CareerEventData.Active event : data.active()) {
            if (event.deadline() > turn)
                active.add(event);
            else if (CareerEventType.byId(event.id()).map(t -> t.kind() == CareerEventType.Kind.MANDATORY).orElse(false))
                failed.add(event);
        }

        // At most one new event per turn. 呆滯 souls get them less often.
        List<CareerEventData.Entry> table = new ArrayList<>(data.table());
        CareerEventType next = scheduledFor(data, turn);
        double chance = UmapyoiConfig.EVENT_CHANCE_PER_TURN.get();
        if (PersonalityEffects.has(soul, PersonalityTrait.DULL))
            chance *= UmapyoiConfig.DULL_EVENT_CHANCE_MULTIPLIER.get();
        if (next == null && player.getRandom().nextDouble() < chance)
            next = draw(player, soul, table, active);
        if (next != null) {
            active.add(new CareerEventData.Active(next.id(), turn, turn + next.turns(soul),
                    next.noteWhenAppearing(player, soul)));
            for (int i = 0; i < table.size(); i++) {
                if (table.get(i).id().equals(next.id()))
                    table.set(i, new CareerEventData.Entry(next.id(), Math.max(0, table.get(i).left() - 1)));
            }
        }
        soul.set(DataComponentsTypeRegistry.CAREER_EVENTS, new CareerEventData(table, data.scheduled(), active));

        for (CareerEventData.Active event : failed)
            fail(player, soul, event);
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
    private static CareerEventType draw(ServerPlayer player, ItemStack soul, List<CareerEventData.Entry> table,
            List<CareerEventData.Active> active) {
        List<CareerEventType> candidates = new ArrayList<>();
        int total = 0;
        for (CareerEventData.Entry entry : table) {
            Optional<CareerEventType> type = CareerEventType.byId(entry.id());
            if (entry.left() <= 0 || type.isEmpty() || type.get().weight(soul) <= 0)
                continue;
            if (active.stream().anyMatch(a -> a.id().equals(entry.id())) || !type.get().canAppear(player, soul))
                continue;
            candidates.add(type.get());
            total += type.get().weight(soul);
        }
        if (total <= 0)
            return null;
        RandomSource random = player.getRandom();
        int pick = random.nextInt(total);
        for (CareerEventType type : candidates) {
            pick -= type.weight(soul);
            if (pick < 0)
                return type;
        }
        return null;
    }

    /** The worn soul's event of this type in progress, if any. */
    public static Optional<CareerEventData.Active> active(ServerPlayer player, CareerEventType type) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (!CareerManager.isInCareer(soul))
            return Optional.empty();
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        if (data == null)
            return Optional.empty();
        return data.active().stream().filter(a -> a.id().equals(type.id())).findFirst();
    }

    public static boolean isActive(ServerPlayer player, CareerEventType type) {
        return active(player, type).isPresent();
    }

    /** Career points multiplier from events in progress, per mille (懶散期 halves them). */
    public static long pointsPermille(ItemStack soul) {
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        if (data != null && data.active().stream().anyMatch(a -> a.id().equals(CareerEventType.LAZY_PERIOD.id())))
            return UmapyoiConfig.EVENT_LAZY_POINTS_PERMILLE.get();
        return 1000;
    }

    /**
     * The player did it: the event goes away and motivation rises one step. A state (the lazy
     * period) just ends, with no reward.
     */
    public static void complete(ServerPlayer player, CareerEventType type) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (!CareerManager.isInCareer(soul))
            return;
        Optional<CareerEventData.Active> removed = remove(soul, type);
        if (removed.isEmpty())
            return;
        Component name = type.displayName(removed.get().data());
        if (type.kind() == CareerEventType.Kind.STATE) {
            player.displayClientMessage(Component.translatable("umapyoi.career.event.ended", name), true);
            return;
        }
        UmaStatusUtils.changeMotivation(player, 1);
        player.displayClientMessage(Component.translatable("umapyoi.career.event.completed", name), true);
        player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    /** It can't be done any more (dawn has passed and the like): it goes away with no penalty. */
    public static void cancel(ServerPlayer player, CareerEventType type) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (!CareerManager.isInCareer(soul))
            return;
        remove(soul, type).ifPresent(event -> player.displayClientMessage(
                Component.translatable("umapyoi.career.event.cancelled", type.displayName(event.data())), true));
    }

    private static Optional<CareerEventData.Active> remove(ItemStack soul, CareerEventType type) {
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        if (data == null)
            return Optional.empty();
        List<CareerEventData.Active> active = new ArrayList<>(data.active());
        Optional<CareerEventData.Active> found = active.stream().filter(a -> a.id().equals(type.id())).findFirst();
        if (found.isEmpty())
            return found;
        active.remove(found.get());
        soul.set(DataComponentsTypeRegistry.CAREER_EVENTS, new CareerEventData(data.table(), data.scheduled(), active));
        return found;
    }

    /** A mandatory event ran out: motivation down (two steps for 急躁) and every stat loses a little. */
    private static void fail(ServerPlayer player, ItemStack soul, CareerEventData.Active event) {
        CareerEventType type = CareerEventType.byId(event.id()).orElseThrow();
        int steps = PersonalityEffects.has(soul, PersonalityTrait.IMPATIENT) ? UmapyoiConfig.IMPATIENT_FAIL_MOOD_STEPS.get() : 1;
        UmaStatusUtils.changeMotivation(player, -steps);
        CareerManager.penalize(player, soul, UmapyoiConfig.EVENT_FAIL_KEEP_PERMILLE.get());
        player.displayClientMessage(Component.translatable("umapyoi.career.event.failed", type.displayName(event.data())), true);
        player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.8F, 0.8F);
    }
}
