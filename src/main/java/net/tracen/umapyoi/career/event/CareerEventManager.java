package net.tracen.umapyoi.career.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.effect.MobEffectRegistry;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.registry.umadata.UmaDataBasicStatus;
import net.tracen.umapyoi.trait.PersonalityEffects;
import net.tracen.umapyoi.trait.PersonalityTrait;
import net.tracen.umapyoi.utils.UmaSkillUtils;
import net.tracen.umapyoi.utils.UmaStatusUtils;
import net.tracen.umapyoi.advancements.trigger.UmapyoiActionTrigger;

/**
 * The per-turn event system (design doc ch. 9). When a career starts it gets an event table. At the
 * start of every turn, events past their deadline are settled, then one event may come up: the one
 * fixed to this turn, otherwise a roll draws one from the table. Several can be in progress at once.
 * Support card skills are rolled every turn on their own, apart from that one event.
 */
public final class CareerEventManager {
    /** Turns of the two New Years: Classic and Senior year, first half of January. */
    public static final int NEW_YEAR_FIRST_TURN = 14;
    public static final int NEW_YEAR_LAST_TURN = 38;

    private CareerEventManager() {
    }

    /** What the support cards bring to a career, noted when it starts (cards go back after). */
    public record CardInfo(List<String> skills, int acupuncture, int randomStat) {
        public static final CardInfo NONE = new CardInfo(List.of(), 0, 0);
    }

    /** For a career started before events existed: no card events. */
    public static CareerEventData buildTable(ItemStack soul) {
        return buildTable(soul, CardInfo.NONE);
    }

    /** Everything this career can meet: general and trait events, card events, the New Years. */
    public static CareerEventData buildTable(ItemStack soul, CardInfo cards) {
        List<CareerEventData.Entry> table = new ArrayList<>();
        for (CareerEventType type : CareerEventType.values()) {
            if (type.belongsIn(soul))
                table.add(new CareerEventData.Entry(type.id(), type.maxCount()));
        }
        // card events are one-time: one per card
        if (cards.acupuncture() > 0)
            table.add(new CareerEventData.Entry(CareerEventType.ACUPUNCTURE.id(), cards.acupuncture()));
        if (cards.randomStat() > 0)
            table.add(new CareerEventData.Entry(CareerEventType.RANDOM_STAT.id(), cards.randomStat()));
        List<CareerEventData.Scheduled> scheduled = List.of(
                new CareerEventData.Scheduled(NEW_YEAR_FIRST_TURN, CareerEventType.NEW_YEAR_FIRST.id()),
                new CareerEventData.Scheduled(NEW_YEAR_LAST_TURN, CareerEventType.NEW_YEAR_LAST.id()));
        List<String> skills = cards.skills().stream().filter(id -> {
            ResourceLocation skill = ResourceLocation.tryParse(id);
            return skill != null && !UmaSkillUtils.hasLearnedSkill(soul, skill);
        }).distinct().toList();
        return new CareerEventData(table, scheduled, List.of(), skills);
    }

    /** Called by CareerManager when the worn soul's career reaches a new turn. */
    public static void onNewTurn(ServerPlayer player, ItemStack soul, int turn) {
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        if (data == null)
            data = buildTable(soul);
        RandomSource random = player.getRandom();

        // Settle what ran out. Failing a mandatory one costs motivation and stats; the rest just end.
        List<CareerEventData.Active> active = new ArrayList<>();
        List<CareerEventData.Active> failed = new ArrayList<>();
        for (CareerEventData.Active event : data.active()) {
            if (event.deadline() > turn)
                active.add(event);
            else if (CareerEventType.byId(event.id()).map(t -> t.kind() == CareerEventType.Kind.MANDATORY).orElse(false))
                failed.add(event);
        }

        // At most one new event per turn: the one fixed to it, or a draw. 呆滯 souls draw less often.
        List<CareerEventData.Entry> table = new ArrayList<>(data.table());
        CareerEventType next = scheduledFor(data, turn);
        double chance = UmapyoiConfig.EVENT_CHANCE_PER_TURN.get();
        if (PersonalityEffects.has(soul, PersonalityTrait.DULL))
            chance *= UmapyoiConfig.DULL_EVENT_CHANCE_MULTIPLIER.get();
        if (next == null && random.nextDouble() < chance)
            next = draw(player, soul, table, active);
        CareerEventData.Active nextActive = null;
        if (next != null) {
            String note = next.kind() == CareerEventType.Kind.NOTICE ? rollNotice(next, random)
                    : next.noteWhenAppearing(player, soul);
            nextActive = new CareerEventData.Active(next.id(), turn, turn + next.turns(soul), note);
            active.add(nextActive);
            for (int i = 0; i < table.size(); i++) {
                if (table.get(i).id().equals(next.id()))
                    table.set(i, new CareerEventData.Entry(next.id(), Math.max(0, table.get(i).left() - 1)));
            }
        }

        // Support card skills: each still to learn gets its own roll; together about 90% per career.
        List<String> skills = new ArrayList<>();
        List<String> learned = new ArrayList<>();
        double skillChance = 1.0 - Math.pow(1.0 - UmapyoiConfig.EVENT_SKILL_CHANCE.get(), 1.0 / CareerManager.PERIODS);
        for (String id : data.skills()) {
            ResourceLocation skill = ResourceLocation.tryParse(id);
            if (skill == null || !UmaSkillRegistry.REGISTRY.containsKey(skill) || UmaSkillUtils.hasLearnedSkill(soul, skill))
                continue;
            if (random.nextDouble() < skillChance) {
                UmaSkillUtils.learnSkill(soul, skill);
                if (UmaSkillUtils.hasLearnedSkill(soul, skill)) {
                    learned.add(id);
                    active.add(new CareerEventData.Active(CareerEventType.SKILL_LEARNED.id(), turn, turn + 1, id));
                    continue;
                }
            }
            skills.add(id); // not this time (or no free skill slot): roll again next turn
        }
        soul.set(DataComponentsTypeRegistry.CAREER_EVENTS, new CareerEventData(table, data.scheduled(), active, skills));

        for (CareerEventData.Active event : failed)
            fail(player, soul, event);
        if (nextActive != null) {
            if (next.kind() == CareerEventType.Kind.NOTICE)
                applyNotice(player, soul, next, nextActive.data());
            else
                player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        for (String id : learned) {
            UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.LEARN_SKILL);
            player.displayClientMessage(Component.translatable("umapyoi.career.event.skill_learned",
                    CareerEventType.skillName(id)).withStyle(ChatFormatting.AQUA), true);
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.2F);
        }
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
        int pick = player.getRandom().nextInt(total);
        for (CareerEventType type : candidates) {
            pick -= type.weight(soul);
            if (pick < 0)
                return type;
        }
        return null;
    }

    // ---------------------------------------------------------------- notices

    /** Decides how a notice turns out, before anything happens: noted on the event for the list. */
    private static String rollNotice(CareerEventType type, RandomSource random) {
        return switch (type) {
            case ACUPUNCTURE -> random.nextDouble() < UmapyoiConfig.EVENT_ACUPUNCTURE_CHANCE.get() ? "success" : "fail";
            case RANDOM_STAT -> String.valueOf(random.nextInt(5));
            default -> "";
        };
    }

    private static void applyNotice(ServerPlayer player, ItemStack soul, CareerEventType type, String note) {
        switch (type) {
            case NEW_YEAR_FIRST, NEW_YEAR_LAST -> {
                // 超絕好調 for a while, whatever the motivation; a stronger one already running is kept.
                MobEffectInstance current = player.getEffect(MobEffectRegistry.MOOD_BONUS);
                int ticks = UmapyoiConfig.EVENT_NEW_YEAR_BUFF_SECONDS.get() * 20;
                int amplifier = current == null ? 0 : current.getAmplifier();
                if (current == null || current.getDuration() < ticks)
                    player.addEffect(new MobEffectInstance(MobEffectRegistry.MOOD_BONUS, ticks, amplifier));
                player.sendSystemMessage(Component.translatable("umapyoi.career.event.notice_message",
                        type.displayName(note), type.hint(note)).withStyle(ChatFormatting.GOLD));
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.0F);
                return;
            }
            case ACUPUNCTURE -> {
                if ("success".equals(note)) {
                    soul.update(DataComponentsTypeRegistry.UMADATA_MAX_BASIC_STATUS, UmaDataBasicStatus.DEFAULT_MAX_STATUS,
                            max -> new UmaDataBasicStatus(Math.min(39, max.speed() + 1), Math.min(39, max.stamina() + 1),
                                    Math.min(39, max.strength() + 1), Math.min(39, max.guts() + 1),
                                    Math.min(39, max.wisdom() + 1)));
                } else {
                    UmaStatusUtils.changeMotivation(player, -1);
                }
            }
            case RANDOM_STAT -> {
                int stat;
                try {
                    stat = Integer.parseInt(note);
                } catch (NumberFormatException e) {
                    stat = 0;
                }
                CareerManager.addStatLevels(player, soul, Math.floorMod(stat, 5), 1);
            }
            default -> {
            }
        }
        player.displayClientMessage(Component.translatable("umapyoi.career.event.notice_message",
                type.displayName(note), type.hint(note)), true);
        player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    // ---------------------------------------------------------------- queries and results

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
        soul.set(DataComponentsTypeRegistry.CAREER_EVENTS, data.withActive(active));
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
