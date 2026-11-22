package net.tracen.umapyoi.trait;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerAttachments;
import net.tracen.umapyoi.events.MotivationEvent;
import net.tracen.umapyoi.events.ResumeActionPointEvent;
import net.tracen.umapyoi.utils.UmaStatusUtils;

/**
 * Personality trait effects that react to what happens. The ones other
 * systems ask for are in PersonalityEffects. Not done yet, because they need whims: 懶散,
 * 急躁, 呆滯's fewer whims; 挑食's disliked food waits for the food system.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public class PersonalityEvents {

    // ---------------------------------------------------------------- motivation

    /** 開朗: drops happen half as often. 呆滯: gains often don't happen. */
    @SubscribeEvent
    public static void onMotivation(MotivationEvent event) {
        LivingEntity target = event.getTarget();
        if (target == null || target.level().isClientSide())
            return;
        int before = event.previous.ordinal();
        int after = event.getAfter().ordinal();
        ItemStack soul = event.getTargetSoul();
        if (after > before && PersonalityEffects.has(soul, PersonalityTrait.CHEERFUL)
                && target.getRandom().nextDouble() >= UmapyoiConfig.CHEERFUL_MOOD_DROP_MULTIPLIER.get())
            event.setCanceled(true);
        else if (after < before && PersonalityEffects.has(soul, PersonalityTrait.DULL)
                && target.getRandom().nextDouble() < UmapyoiConfig.DULL_MOOD_GAIN_FAIL_CHANCE.get())
            event.setCanceled(true);
    }

    // ---------------------------------------------------------------- getting hurt, fighting

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0)
            return;
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim)
            attacker.getData(CareerAttachments.TRAIT_STATE).lastCombatTick = attacker.level().getGameTime();
        if (!(victim instanceof ServerPlayer player) || UmapyoiAPI.getUmaSoul(player).isEmpty())
            return;
        player.getData(CareerAttachments.TRAIT_STATE).lastCombatTick = player.level().getGameTime();

        // 膽小: sometimes slowed
        if (PersonalityEffects.has(player, PersonalityTrait.TIMID)
                && player.getRandom().nextDouble() < UmapyoiConfig.TIMID_CHANCE.get())
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    UmapyoiConfig.TIMID_SLOWNESS_SECONDS.get() * 20, 0));
        // 懦弱: weak when nobody is around
        if (PersonalityEffects.has(player, PersonalityTrait.COWARDLY) && !PersonalityEffects.hasCompanion(player))
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    UmapyoiConfig.COWARDLY_WEAKNESS_SECONDS.get() * 20, 0));
    }

    /** 興奮: no action point recovery for a while after a fight. */
    @SubscribeEvent
    public static void onResumeActionPoint(ResumeActionPointEvent event) {
        if (!(event.getLivingEntity() instanceof ServerPlayer player)
                || !PersonalityEffects.has(event.getUmaSoul(), PersonalityTrait.EXCITABLE))
            return;
        long since = player.level().getGameTime() - player.getData(CareerAttachments.TRAIT_STATE).lastCombatTick;
        if (since >= 0 && since < UmapyoiConfig.EXCITABLE_AP_PAUSE_SECONDS.get() * 20L)
            event.setCanceled(true);
    }

    // ---------------------------------------------------------------- experience, food

    /** 好學: more experience from orbs; fractions add up over time. */
    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !PersonalityEffects.has(player, PersonalityTrait.STUDIOUS))
            return;
        TraitState state = player.getData(CareerAttachments.TRAIT_STATE);
        state.xpFraction += event.getOrb().getValue() * UmapyoiConfig.STUDIOUS_XP_BONUS.get();
        int extra = (int) state.xpFraction;
        if (extra > 0) {
            state.xpFraction -= extra;
            player.giveExperiencePoints(extra);
        }
    }

    /** 挑食: the same food too many times in a row lowers motivation. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack stack = event.getItem();
        if (stack.getItem().getFoodProperties(stack, player) == null
                || !PersonalityEffects.has(player, PersonalityTrait.PICKY_EATER))
            return;
        TraitState state = player.getData(CareerAttachments.TRAIT_STATE);
        Item food = stack.getItem();
        state.sameFoodCount = food == state.lastFood ? state.sameFoodCount + 1 : 1;
        state.lastFood = food;
        if (state.sameFoodCount >= UmapyoiConfig.PICKY_EATER_SAME_FOOD.get()) {
            state.sameFoodCount = 0;
            UmaStatusUtils.changeMotivation(player, -1);
        }
    }

    // ---------------------------------------------------------------- once per second

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0)
            return;
        TraitState state = player.getData(CareerAttachments.TRAIT_STATE);
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (soul.isEmpty() || !player.isAlive()) {
            state.rainSeconds = 0;
            return;
        }
        Level level = player.level();
        BlockPos eyes = BlockPos.containing(player.getEyePosition());

        // 怕黑: Darkness in dark places
        if (PersonalityEffects.has(soul, PersonalityTrait.AFRAID_OF_DARK)
                && level.getMaxLocalRawBrightness(eyes) <= UmapyoiConfig.AFRAID_OF_DARK_LIGHT.get())
            refresh(player, MobEffects.DARKNESS, 0, 60);

        // 怕雨: out in the rain lowers motivation bit by bit
        if (PersonalityEffects.has(soul, PersonalityTrait.HATES_RAIN) && level.isRainingAt(eyes)) {
            if (++state.rainSeconds >= UmapyoiConfig.HATES_RAIN_SECONDS.get()) {
                state.rainSeconds = 0;
                UmaStatusUtils.changeMotivation(player, -1);
            }
        } else {
            state.rainSeconds = 0;
        }

        // 堅韌: Resistance at low health
        if (PersonalityEffects.has(soul, PersonalityTrait.TENACIOUS)
                && player.getHealth() <= player.getMaxHealth() * UmapyoiConfig.TENACIOUS_HEALTH.get())
            refresh(player, MobEffects.DAMAGE_RESISTANCE, 0, 40);
    }

    /** Keeps a short effect going without resetting a longer one the player already has. */
    private static void refresh(ServerPlayer player, Holder<MobEffect> effect, int amplifier, int ticks) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && (current.isInfiniteDuration() || current.getAmplifier() > amplifier
                || current.getDuration() > ticks / 2))
            return;
        player.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false, true));
    }
}
