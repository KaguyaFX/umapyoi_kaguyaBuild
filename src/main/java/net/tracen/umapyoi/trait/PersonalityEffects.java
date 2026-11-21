package net.tracen.umapyoi.trait;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.registry.umadata.Motivations;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * Personality trait effects that other systems ask for (career points, hunger, Night Owl, the
 * chance to lose motivation when hurt). Both the negative and the positive trait work whenever the
 * soul is worn, during the career and after it. Only career souls have them.
 */
public final class PersonalityEffects {
    private PersonalityEffects() {
    }

    public static boolean has(@Nullable LivingEntity entity, PersonalityTrait trait) {
        return entity != null && has(UmapyoiAPI.getUmaSoul(entity), trait);
    }

    public static boolean has(ItemStack soul, PersonalityTrait trait) {
        if (soul.isEmpty())
            return false;
        UmaTraits traits = soul.get(DataComponentsTypeRegistry.UMA_TRAITS);
        if (traits == null)
            return false;
        return (trait.positive() ? traits.positive() : traits.negative()).contains(trait.id());
    }

    /** 厭光 and 踏實: multiplier on every career training point, per mille. */
    public static long pointsPermille(ServerPlayer player, ItemStack soul) {
        long permille = 1000;
        if (has(soul, PersonalityTrait.LIGHT_AVERSE)) {
            Level level = player.level();
            if (!level.dimensionType().hasFixedTime()) {
                if (!level.isDay())
                    permille = permille * UmapyoiConfig.LIGHT_AVERSE_NIGHT_PERMILLE.get() / 1000;
                else if (!level.isRaining() && level.canSeeSky(BlockPos.containing(player.getEyePosition())))
                    permille = permille * UmapyoiConfig.LIGHT_AVERSE_DAY_PERMILLE.get() / 1000;
            }
        }
        if (has(soul, PersonalityTrait.STEADY)
                && UmaSoulUtils.getMotivation(soul).ordinal() <= Motivations.NORMAL.ordinal())
            permille = permille * UmapyoiConfig.STEADY_PERMILLE.get() / 1000;
        return permille;
    }

    /** 興奮: multiplier on career points from combat, per mille. */
    public static long combatPermille(ServerPlayer player) {
        return has(player, PersonalityTrait.EXCITABLE) ? UmapyoiConfig.EXCITABLE_COMBAT_PERMILLE.get() : 1000;
    }

    /** 玻璃心 doubles and 開朗 halves the chance to lose motivation when hurt. */
    public static double hurtMoodChance(LivingEntity entity, double chance) {
        if (has(entity, PersonalityTrait.FRAGILE))
            chance = Math.min(1.0, chance * UmapyoiConfig.FRAGILE_HURT_MOOD_MULTIPLIER.get());
        if (has(entity, PersonalityTrait.CHEERFUL))
            chance *= UmapyoiConfig.CHEERFUL_MOOD_DROP_MULTIPLIER.get();
        return chance;
    }

    /** 嗜睡: Night Owl comes sooner. */
    public static long nightOwlThreshold(LivingEntity entity, long threshold) {
        if (!has(entity, PersonalityTrait.SLEEPY))
            return threshold;
        return Math.max(1, (long) (threshold * UmapyoiConfig.SLEEPY_NIGHT_OWL_FACTOR.get()));
    }

    /** 貪吃: more hunger. */
    public static double hungerMultiplier(LivingEntity entity) {
        return has(entity, PersonalityTrait.GLUTTON) ? UmapyoiConfig.GLUTTON_HUNGER_MULTIPLIER.get() : 1.0;
    }
}
