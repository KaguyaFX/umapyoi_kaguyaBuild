package net.tracen.umapyoi.blessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.advancements.trigger.UmapyoiActionTrigger;

/**
 * Rules of the Three Goddesses blessings: offerings are worth points, points buy a random
 * blessing (or a chosen level-I one at 64), held blessings can be extended.
 */
public final class BlessingManager {
    public static final int MAX_HELD = 2;
    public static final int MIN_POINTS = 8;
    public static final int MAX_SPEND = 64;

    private BlessingManager() {
    }

    // ---------------------------------------------------------------- offerings

    private static Map<Item, Integer> values;

    private static Map<Item, Integer> values() {
        if (values == null)
            values = Map.of(Items.IRON_INGOT, 1, Items.GOLD_INGOT, 2, Items.DIAMOND, 8, Items.NETHERITE_INGOT, 32,
                    Items.IRON_BLOCK, 9, Items.GOLD_BLOCK, 18, Items.DIAMOND_BLOCK, 72, Items.NETHERITE_BLOCK, 288,
                    ItemRegistry.JEWEL.get(), 4);
        return values;
    }

    /** Points one item of this kind is worth, 0 if the goddesses don't take it. */
    public static int pointsOf(ItemStack stack) {
        return stack.isEmpty() ? 0 : values().getOrDefault(stack.getItem(), 0);
    }

    // ---------------------------------------------------------------- what a player holds

    /** Level 0-2 of this blessing on the entity, or -1. */
    public static int level(LivingEntity entity, BlessingType type) {
        MobEffectInstance instance = entity.getEffect(type.effect());
        return instance == null ? -1 : Math.min(instance.getAmplifier(), 2);
    }

    public static List<BlessingType> held(LivingEntity entity) {
        List<BlessingType> list = new ArrayList<>();
        for (BlessingType type : BlessingType.values())
            if (entity.hasEffect(type.effect()))
                list.add(type);
        return list;
    }

    public static boolean hasAny(LivingEntity entity) {
        for (BlessingType type : BlessingType.values())
            if (entity.hasEffect(type.effect()))
                return true;
        return false;
    }

    public static void removeAll(LivingEntity entity) {
        for (BlessingType type : BlessingType.values())
            entity.removeEffect(type.effect());
    }

    // ---------------------------------------------------------------- rolling

    /** Level 0-2 for a payment of 8-64 points: I/II/III = 80/18/2 at 8, 50/35/15 at 32, 25/40/35 at 64. */
    public static int rollLevel(int spent, RandomSource random) {
        double[] at8 = { 80, 18, 2 }, at32 = { 50, 35, 15 }, at64 = { 25, 40, 35 };
        double[] a, b;
        double t;
        if (spent <= 32) {
            a = at8;
            b = at32;
            t = (Math.max(spent, 8) - 8) / 24.0;
        } else {
            a = at32;
            b = at64;
            t = (Math.min(spent, 64) - 32) / 32.0;
        }
        double roll = random.nextDouble() * 100;
        double one = a[0] + (b[0] - a[0]) * t;
        double two = a[1] + (b[1] - a[1]) * t;
        return roll < one ? 0 : roll < one + two ? 1 : 2;
    }

    /**
     * Gives (or renews) a blessing. Same blessing again: the timer restarts; a higher level replaces
     * the lower one.
     */
    public static void grant(ServerPlayer player, BlessingType type, int level) {
        int current = level(player, type);
        int newLevel = Math.max(current, level);
        if (current >= 0)
            player.removeEffect(type.effect());
        player.addEffect(new MobEffectInstance(type.effect(), type.rollDuration(newLevel, player.getRandom()),
                newLevel, false, false, true));
        if (type == BlessingType.RENEWAL)
            RenewalBlessingEffect.refill(player, newLevel);
        UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.BLESSING);
    }

    // ---------------------------------------------------------------- statue actions

    public interface PointStore {
        int getPoints();

        void setPoints(int points);
    }

    /** Why this player can't receive a new blessing right now, or null. */
    public static Component newBlessingProblem(ServerPlayer player, int points, int needed) {
        if (UmapyoiAPI.getUmaSoul(player).isEmpty())
            return Component.translatable("umapyoi.blessing.message.need_soul");
        if (held(player).size() >= MAX_HELD)
            return Component.translatable("umapyoi.blessing.message.too_many");
        if (points < needed)
            return Component.translatable("umapyoi.blessing.message.not_enough", needed);
        return null;
    }

    /** "祈求祝福": spend up to 64 points for a random blessing of a random level. */
    public static void pray(ServerPlayer player, PointStore store) {
        Component problem = newBlessingProblem(player, store.getPoints(), MIN_POINTS);
        if (problem != null) {
            player.displayClientMessage(problem, true);
            return;
        }
        int spent = Math.min(store.getPoints(), MAX_SPEND);
        store.setPoints(store.getPoints() - spent);
        BlessingType type = BlessingType.values()[player.getRandom().nextInt(BlessingType.values().length)];
        int level = rollLevel(spent, player.getRandom());
        grant(player, type, level);
        announce(player, type);
    }

    /** "自選": 64 points for a chosen blessing, level I only. */
    public static void choose(ServerPlayer player, PointStore store, BlessingType type) {
        Component problem = newBlessingProblem(player, store.getPoints(), MAX_SPEND);
        if (problem != null) {
            player.displayClientMessage(problem, true);
            return;
        }
        store.setPoints(store.getPoints() - MAX_SPEND);
        grant(player, type, 0);
        announce(player, type);
    }

    /** "延續": pay by level to restart a held blessing's timer. Allowed even with 2 held. */
    public static void extend(ServerPlayer player, PointStore store, BlessingType type) {
        int level = level(player, type);
        if (level < 0 || UmapyoiAPI.getUmaSoul(player).isEmpty())
            return;
        int cost = BlessingType.EXTEND_COST[level];
        if (store.getPoints() < cost) {
            player.displayClientMessage(Component.translatable("umapyoi.blessing.message.not_enough", cost), true);
            return;
        }
        store.setPoints(store.getPoints() - cost);
        grant(player, type, level);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.2F);
    }

    private static void announce(ServerPlayer player, BlessingType type) {
        player.displayClientMessage(Component.translatable("umapyoi.blessing.message.received",
                type.displayName(level(player, type))), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 1F);
    }
}
