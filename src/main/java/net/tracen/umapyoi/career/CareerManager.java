package net.tracen.umapyoi.career;

import java.util.Arrays;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.blessing.BlessingManager;
import net.tracen.umapyoi.blessing.BlessingType;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.registry.training.ExtraStatusSupport;
import net.tracen.umapyoi.registry.training.StatusSupport;
import net.tracen.umapyoi.registry.training.SupportContainer;
import net.tracen.umapyoi.registry.training.SupportStack;
import net.tracen.umapyoi.registry.umadata.Motivations;
import net.tracen.umapyoi.registry.umadata.UmaDataBasicStatus;
import net.tracen.umapyoi.registry.umadata.UmaDataExtraStatus;
import net.tracen.umapyoi.registry.umadata.UmaDataTraining;
import net.tracen.umapyoi.utils.ResultRankingUtils;
import net.tracen.umapyoi.utils.UmaSkillUtils;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import net.tracen.umapyoi.trait.PersonalityEffects;
import net.tracen.umapyoi.career.event.CareerEventManager;
import net.tracen.umapyoi.registry.training.SkillSupport;
import net.tracen.umapyoi.registry.training.BorealisSupport;
import net.tracen.umapyoi.registry.training.RandomStatusSupport;
import java.util.ArrayList;
import java.util.List;
import net.tracen.umapyoi.advancements.trigger.UmapyoiActionTrigger;

/**
 * Three-year career: soul lifecycle, career time, training points, death and the end of the career.
 */
public final class CareerManager {
    /** Junior early June (11) to Senior late December (72). */
    public static final int PERIODS = 62;
    public static final int FIRST_PERIOD = 11;
    public static final long MILLION = 1_000_000L;
    /** Lowest stat a soul can have. A stat of 0 breaks other code (e.g. max action points = wisdom x 200). */
    public static final int MIN_STAT = 1;

    public static final int SPEED = 0, STAMINA = 1, STRENGTH = 2, GUTS = 3, WISDOM = 4;

    private CareerManager() {
    }

    // ---------------------------------------------------------------- state

    @Nullable
    public static CareerState getState(ItemStack soul) {
        return soul.get(DataComponentsTypeRegistry.CAREER_STATE);
    }

    public static boolean isUndecided(ItemStack stack) {
        return stack.is(ItemRegistry.UMA_SOUL.get()) && getState(stack) == CareerState.UNDECIDED;
    }

    public static boolean isInCareer(ItemStack stack) {
        return stack.is(ItemRegistry.UMA_SOUL.get()) && getState(stack) == CareerState.IN_CAREER;
    }

    /** In or after its three-year career: the training terminal must not touch it. */
    public static boolean isCareerSoul(ItemStack stack) {
        CareerState state = getState(stack);
        return stack.is(ItemRegistry.UMA_SOUL.get())
                && (state == CareerState.IN_CAREER || state == CareerState.FINISHED);
    }

    /** "Junior Early June" style label for a period index from the race calendar. */
    public static Component periodName(int periodIndex) {
        int year = (periodIndex - 1) / 24;
        int half = (periodIndex - 1) % 24 + 1;
        String yearKey = switch (year) {
            case 0 -> "junior";
            case 1 -> "classic";
            default -> "senior";
        };
        return Component.translatable("tooltip.umapyoi.race.time." + yearKey).append(" ")
                .append(Component.translatable("tooltip.umapyoi.race.time." + half));
    }

    // ---------------------------------------------------------------- start

    /**
     * Starts the career of the undecided soul in terminal slot 0. Simulates worst-case auto-training,
     * charges the same card cost, and stores the result on the soul. Cards stay in their slots.
     */
    public static boolean startCareer(ServerLevel level, ItemStackHandler inventory, ServerPlayer player) {
        ItemStack soul = inventory.getStackInSlot(0);
        if (!isUndecided(soul) || !soul.has(DataComponentsTypeRegistry.UMADATA_TRAINING))
            return false;
        for (int i = 1; i < 7; i++) {
            ItemStack card = inventory.getStackInSlot(i);
            if (card.getItem() instanceof SupportContainer support && !support.canSupport(level, card).test(soul))
                return false;
        }

        Simulation simulation = simulateWorstCase(level, inventory, soul);
        ItemStack sim = simulation.result();
        int[] used = simulation.used();
        CareerEventManager.CardInfo cardInfo = cardInfo(level, inventory, used);

        // Same cost as auto-training: a card that would break in the simulation breaks now.
        for (int i = 1; i < 7; i++) {
            if (used[i] == 0)
                continue;
            ItemStack card = inventory.getStackInSlot(i).copy();
            SupportContainer support = (SupportContainer) card.getItem();
            if (support.isConsumable(level, card)) {
                card.shrink(used[i]);
            } else if (card.isDamageableItem() && card.getDamageValue() + used[i] >= card.getMaxDamage()) {
                card = ItemStack.EMPTY;
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1F, 1F);
            } else if (card.isDamageableItem()) {
                card.setDamageValue(card.getDamageValue() + used[i]);
            }
            inventory.setStackInSlot(i, card);
        }

        int[] start = UmaSoulUtils.getProperty(soul).toArray();
        int[] growth = simulation.growth();
        int[] precise = new int[5];
        int[] cards = new int[5];
        for (int i = 0; i < 5; i++) {
            precise[i] = (int) (start[i] * MILLION);
            cards[i] = 1000; // placeholder: cards don't change career growth yet
        }

        ItemStack result = soul.copy();
        // Fixed non-stat gains are given now, as much as auto-training would give.
        UmaSoulUtils.setSkillSlots(result, UmaSoulUtils.getSkillSlots(sim));
        UmaSoulUtils.setExtraActionPoint(result, UmaSoulUtils.getExtraActionPoint(sim));
        long total = UmapyoiConfig.CAREER_LENGTH_MINUTES.get() * 60L * 20L;
        result.set(DataComponentsTypeRegistry.CAREER_DATA, new UmaCareerData(UUID.randomUUID(), 0L, total,
                UmaCareerData.toList(precise), UmaCareerData.toList(start), UmaCareerData.toList(growth),
                UmaCareerData.toList(cards), 0));
        result.set(DataComponentsTypeRegistry.CAREER_STATE, CareerState.IN_CAREER);
        result.set(DataComponentsTypeRegistry.CAREER_EVENTS, CareerEventManager.buildTable(result, cardInfo));
        result.update(DataComponentsTypeRegistry.UMADATA_TRAINING, new UmaDataTraining(1, 6, false),
                data -> new UmaDataTraining(0, data.talent(), true));
        UmaSkillUtils.syncActionPoint(result);
        inventory.setStackInSlot(0, result);

        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1F, 1F);
        player.sendSystemMessage(Component.translatable("umapyoi.career.started"));
        return true;
    }

    /** Worst-case auto-training of a soul with the cards in slots 1-6 of {@code inventory}. */
    public record Simulation(ItemStack result, int[] used, int[] growth) {
    }

    /**
     * Worst case: fixed stat supports and fixed extra supports count; random stats, acupuncturist and
     * skills count as failed. Training count = physique; a card stops once its uses run out.
     * Used to start a career and, on the client, to preview the guaranteed grades.
     */
    public static Simulation simulateWorstCase(Level level, IItemHandler inventory, ItemStack soul) {
        int cycles = soul.has(DataComponentsTypeRegistry.UMADATA_TRAINING) ? UmaSoulUtils.getPhysique(soul) : 0;
        ItemStack sim = soul.copy();
        int[] used = new int[7];
        for (int cycle = 0; cycle < cycles; cycle++) {
            for (int i = 1; i < 7; i++) {
                ItemStack card = inventory.getStackInSlot(i);
                if (!(card.getItem() instanceof SupportContainer support) || used[i] >= usesLeft(level, card))
                    continue;
                used[i]++;
                for (SupportStack stack : support.getSupports(level, card)) {
                    if (stack.getFactor() instanceof StatusSupport || stack.getFactor() instanceof ExtraStatusSupport)
                        stack.applySupport(sim, level.getRandom());
                }
            }
        }
        int[] start = UmaSoulUtils.getProperty(soul).toArray();
        int[] simulated = UmaSoulUtils.getProperty(sim).toArray();
        int[] growth = new int[5];
        for (int i = 0; i < 5; i++)
            growth[i] = Math.max(0, simulated[i] - start[i]);
        return new Simulation(sim, used, growth);
    }

    /** Skills, acupuncturists and random-stat supports on the cards that take part in the career. */
    private static CareerEventManager.CardInfo cardInfo(Level level, IItemHandler inventory, int[] used) {
        List<String> skills = new ArrayList<>();
        int acupuncture = 0;
        int randomStat = 0;
        for (int i = 1; i < 7; i++) {
            ItemStack card = inventory.getStackInSlot(i);
            if (used[i] == 0 || !(card.getItem() instanceof SupportContainer support))
                continue;
            for (SupportStack stack : support.getSupports(level, card)) {
                if (stack.getFactor() instanceof SkillSupport) {
                    String skill = stack.getOrCreateTag().getString("skill");
                    if (!skill.isEmpty())
                        skills.add(skill);
                } else if (stack.getFactor() instanceof BorealisSupport) {
                    acupuncture++;
                } else if (stack.getFactor() instanceof RandomStatusSupport) {
                    randomStat++;
                }
            }
        }
        return new CareerEventManager.CardInfo(skills, acupuncture, randomStat);
    }

    /** Base reward for one stat: ceil(growth x percent), in integers. */
    public static int baseReward(int growth) {
        return (growth * UmapyoiConfig.CAREER_BASE_REWARD_PERCENT.get() + 99) / 100;
    }

    private static int usesLeft(Level level, ItemStack card) {
        if (((SupportContainer) card.getItem()).isConsumable(level, card))
            return card.getCount();
        if (!card.isDamageableItem())
            return Integer.MAX_VALUE;
        return card.getMaxDamage() - card.getDamageValue();
    }

    // ---------------------------------------------------------------- runtime

    /** Called every server tick from the worn soul's curioTick. */
    public static void tick(ServerPlayer player, ItemStack soul) {
        CareerCache cache = player.getData(CareerAttachments.CAREER_CACHE);
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (getState(soul) != CareerState.IN_CAREER || data == null) {
            cache.clear();
            return;
        }
        // Read-only check every tick: a different soul (or a copy of this one), or this one taken off and put back.
        if (!belongsTo(cache, data) || !cache.isActive(player))
            load(player, cache, soul, data);
        cache.lastActiveTick = player.tickCount;

        trackActivity(player, cache, soul);

        cache.elapsed++;
        if (cache.elapsed >= cache.total) {
            finish(player, soul, cache);
            return;
        }
        int period = (int) (cache.elapsed * PERIODS / cache.total);
        if (period != cache.period || flooredChanged(cache) || cache.elapsed % 20 == 0) {
            boolean newTurn = period != cache.period;
            cache.period = period;
            flush(soul, cache);
            if (newTurn)
                CareerEventManager.onNewTurn(player, soul, period);
        }
    }

    /** Adds whole levels to one stat of the worn soul's career (a random-stat card event). */
    public static void addStatLevels(ServerPlayer player, ItemStack soul, int stat, int levels) {
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (getState(soul) != CareerState.IN_CAREER || data == null)
            return;
        CareerCache cache = player.getData(CareerAttachments.CAREER_CACHE);
        if (!belongsTo(cache, data) || !cache.isActive(player))
            load(player, cache, soul, data);
        flush(soul, cache);
        cache.precise[stat] += levels * (long) MILLION;
        flush(soul, cache); // clamps to the stat's cap
    }

    /**
     * Keeps {@code keepPermille} of every precise stat (980 = lose 2%), e.g. for a failed mandatory
     * event. Works on the worn soul's career.
     */
    public static void penalize(ServerPlayer player, ItemStack soul, long keepPermille) {
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (getState(soul) != CareerState.IN_CAREER || data == null)
            return;
        CareerCache cache = player.getData(CareerAttachments.CAREER_CACHE);
        if (!belongsTo(cache, data) || !cache.isActive(player))
            load(player, cache, soul, data);
        flush(soul, cache);
        for (int i = 0; i < 5; i++)
            cache.precise[i] = cache.precise[i] * keepPermille / 1000;
        flush(soul, cache);
    }

    /** The cache holds this soul's career: same career id and the progress last read or written. */
    private static boolean belongsTo(CareerCache cache, UmaCareerData data) {
        return data.careerId().equals(cache.careerId) && data.elapsedTicks() == cache.writtenElapsed;
    }

    /** Adds training points (in thousandths of a point) to the worn soul's career, if it has one. */
    public static void addPoints(ServerPlayer player, int stat, long milliPoints) {
        if (milliPoints <= 0)
            return;
        CareerCache cache = player.getData(CareerAttachments.CAREER_CACHE);
        if (!cache.isActive(player))
            return;
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (data == null || !belongsTo(cache, data))
            return;
        addPoints(player, cache, soul, stat, milliPoints);
    }

    private static void addPoints(ServerPlayer player, CareerCache cache, ItemStack soul, int stat, long milliPoints) {
        long mood = Math.round(UmaSoulUtils.getMotivation(soul).getMultiplier() * 1000);
        // 專注 blessing: x1.05 / 1.10 / 1.15
        long focus = 1000 + 50L * (BlessingManager.level(player, BlessingType.FOCUS) + 1);
        // A single gain is capped so absurd amounts (e.g. /kill dealing near-infinite damage) can't overflow.
        milliPoints = Math.min(milliPoints, 1_000_000_000L);
        // 厭光 (day/night), 踏實 (motivation Normal or better), and the lazy period event
        long personality = PersonalityEffects.pointsPermille(player, soul) * CareerEventManager.pointsPermille(soul) / 1000;
        long delta = milliPoints * mood * cache.cardMultiplier[stat] / 1000L * focus * personality
                / (1000L * 1000L * UmapyoiConfig.CAREER_POINTS_PER_LEVEL.get());
        cache.precise[stat] = Math.min(cache.max[stat] * MILLION, cache.precise[stat] + delta);
    }

    private static void trackActivity(ServerPlayer player, CareerCache cache, ItemStack soul) {
        // Speed: time moving on your own feet; sprinting counts double.
        double x = player.getX();
        double z = player.getZ();
        boolean onFoot = !player.isPassenger() && !player.isFallFlying() && !player.getAbilities().flying
                && !player.isSpectator();
        if (cache.hasLastPos && onFoot) {
            double dx = x - cache.lastX;
            double dz = z - cache.lastZ;
            double moved = dx * dx + dz * dz;
            if (moved > 0.0009 && moved < 4.0) {
                int perSecond = player.isSprinting() ? UmapyoiConfig.CAREER_SPEED_SPRINT_PER_SECOND.get()
                        : UmapyoiConfig.CAREER_SPEED_WALK_PER_SECOND.get();
                addPoints(player, cache, soul, SPEED, perSecond * 1000L / 20);
            }
        }
        cache.lastX = x;
        cache.lastZ = z;
        cache.hasLastPos = true;

        // Guts: time under any harmful effect.
        boolean harmful = player.getActiveEffects().stream()
                .anyMatch(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
        if (harmful)
            addPoints(player, cache, soul, GUTS, UmapyoiConfig.CAREER_GUTS_PER_SECOND_HARMFUL.get() * 1000L / 20);

        // Wisdom: experience levels spent (enchanting table, anvil).
        int level = player.experienceLevel;
        if (cache.lastXpLevel >= 0 && level < cache.lastXpLevel)
            addPoints(player, cache, soul, WISDOM,
                    (cache.lastXpLevel - level) * (long) UmapyoiConfig.CAREER_WISDOM_PER_LEVEL_SPENT.get() * 1000L);
        cache.lastXpLevel = level;
    }

    private static void load(ServerPlayer player, CareerCache cache, ItemStack soul, UmaCareerData data) {
        cache.careerId = data.careerId();
        cache.elapsed = data.elapsedTicks();
        cache.writtenElapsed = data.elapsedTicks();
        cache.total = Math.max(1, data.totalTicks());
        cache.period = (int) (Math.min(cache.elapsed, cache.total) * PERIODS / cache.total);
        int[] precise = UmaCareerData.toArray(data.precise());
        int[] lastWritten = UmaCareerData.toArray(data.lastWritten());
        int[] cards = UmaCareerData.toArray(data.cardMultiplier());
        int[] current = UmaSoulUtils.getProperty(soul).toArray();
        int[] max = UmaSoulUtils.getMaxProperty(soul).toArray();
        for (int i = 0; i < 5; i++) {
            // Stats changed outside the career (race rewards, training books) are merged in.
            cache.precise[i] = Math.max(MIN_STAT * MILLION, precise[i] + (long) (current[i] - lastWritten[i]) * MILLION);
            cache.baseline[i] = current[i];
            cache.max[i] = max[i];
            cache.cardMultiplier[i] = cards[i];
        }
        cache.hasLastPos = false;
        cache.lastXpLevel = player.experienceLevel;
    }

    private static boolean flooredChanged(CareerCache cache) {
        for (int i = 0; i < 5; i++) {
            if (cache.precise[i] / MILLION != cache.baseline[i])
                return true;
        }
        return false;
    }

    /** Writes the cache back to the soul, merging any outside change to its integer stats first. */
    private static void flush(ItemStack soul, CareerCache cache) {
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (data == null)
            return;
        int[] current = UmaSoulUtils.getProperty(soul).toArray();
        int[] max = UmaSoulUtils.getMaxProperty(soul).toArray();
        int[] ints = new int[5];
        int[] precise = new int[5];
        for (int i = 0; i < 5; i++) {
            cache.precise[i] += (long) (current[i] - cache.baseline[i]) * MILLION;
            cache.precise[i] = Math.max(MIN_STAT * MILLION, Math.min(max[i] * MILLION, cache.precise[i]));
            cache.max[i] = max[i];
            ints[i] = (int) (cache.precise[i] / MILLION);
            precise[i] = (int) cache.precise[i];
            cache.baseline[i] = ints[i];
        }
        if (!Arrays.equals(ints, current))
            soul.set(DataComponentsTypeRegistry.UMADATA_BASIC_STATUS, UmaDataBasicStatus.init(ints));
        soul.set(DataComponentsTypeRegistry.CAREER_DATA, data.withProgress(cache.elapsed, precise, ints));
        cache.writtenElapsed = cache.elapsed;
    }

    /** Death during the career: all stats x0.9 (inherited values included), mood to the lowest. */
    public static void onDeath(ServerPlayer player) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (getState(soul) != CareerState.IN_CAREER || data == null)
            return;
        CareerCache cache = player.getData(CareerAttachments.CAREER_CACHE);
        if (!belongsTo(cache, data) || !cache.isActive(player))
            load(player, cache, soul, data);
        flush(soul, cache);
        // 悲傷 blessing: level I or higher keeps the stats, level II or higher also the motivation
        int sorrow = BlessingManager.level(player, BlessingType.SORROW);
        if (sorrow < 0) {
            long keep = UmapyoiConfig.CAREER_DEATH_KEEP_PERMILLE.get();
            for (int i = 0; i < 5; i++)
                cache.precise[i] = cache.precise[i] * keep / 1000;
            flush(soul, cache);
        }
        soul.set(DataComponentsTypeRegistry.CAREER_DATA,
                soul.get(DataComponentsTypeRegistry.CAREER_DATA).withDeaths(data.deaths() + 1));
        if (sorrow < 1)
            UmaSoulUtils.setMotivation(soul, Motivations.BAD);
        player.sendSystemMessage(Component.translatable(sorrow < 0 ? "umapyoi.career.death" : "umapyoi.career.death_blessed"));
        cache.clear();
    }

    /** Logout: write back what the cache holds while the soul is still in the slot. */
    public static void onLogout(ServerPlayer player) {
        CareerCache cache = player.getData(CareerAttachments.CAREER_CACHE);
        if (!cache.isActive(player))
            return;
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (data != null && belongsTo(cache, data) && getState(soul) == CareerState.IN_CAREER)
            flush(soul, cache);
        cache.clear();
    }

    /** End of the three years: add the base reward and recompute the ranking. */
    private static void finish(ServerPlayer player, ItemStack soul, CareerCache cache) {
        flush(soul, cache);
        UmaCareerData data = soul.get(DataComponentsTypeRegistry.CAREER_DATA);
        int[] current = UmaSoulUtils.getProperty(soul).toArray();
        int[] max = UmaSoulUtils.getMaxProperty(soul).toArray();
        int[] growth = UmaCareerData.toArray(data.worstGrowth());
        int percent = UmapyoiConfig.CAREER_BASE_REWARD_PERCENT.get();
        int[] result = new int[5];
        int[] precise = new int[5];
        for (int i = 0; i < 5; i++) {
            int base = baseReward(growth[i]); // ceil(g x 110%) in integers
            result[i] = Math.min(max[i], current[i] + base);
            precise[i] = (int) (result[i] * MILLION);
        }
        soul.set(DataComponentsTypeRegistry.UMADATA_BASIC_STATUS, UmaDataBasicStatus.init(result));
        soul.set(DataComponentsTypeRegistry.CAREER_DATA, data.withProgress(data.totalTicks(), precise, result));
        soul.set(DataComponentsTypeRegistry.CAREER_STATE, CareerState.FINISHED);
        soul.remove(DataComponentsTypeRegistry.CAREER_EVENTS);
        UmaSkillUtils.syncActionPoint(soul);
        soul.update(DataComponentsTypeRegistry.UMADATA_EXTRA_STATUS, UmaDataExtraStatus.DEFAULT,
                extra -> new UmaDataExtraStatus(extra.actionPoint(), extra.extraActionPoint(),
                        ResultRankingUtils.generateRanking(soul), extra.motivation()));
        cache.clear();
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 1F, 1F);
        player.sendSystemMessage(Component.translatable("umapyoi.career.finished"));
        UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.FINISH_CAREER);
    }
}
