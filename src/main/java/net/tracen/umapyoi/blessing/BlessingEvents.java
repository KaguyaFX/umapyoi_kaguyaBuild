package net.tracen.umapyoi.blessing;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.tracen.umapyoi.effect.MobEffectRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaStatusUtils;
import net.tracen.umapyoi.advancements.trigger.UmapyoiActionTrigger;

/**
 * Behaviour of the blessings that isn't a plain attribute: removal without a soul, 幸福, 悲傷, 生存.
 * 平靜, 安眠 and 專注 are checked where the matching system lives (CommonEvents, CareerManager),
 * 疾風's hunger cut in PlayerFoodExhaustionMixin.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public class BlessingEvents {

    /** The mod's own Umamusume states, which make no sense on a player without a soul. */
    private static final List<Holder<MobEffect>> UMA_ONLY_EFFECTS = List.of(MobEffectRegistry.MOOD_BONUS,
            MobEffectRegistry.PANICKING, MobEffectRegistry.NIGHT_OWL, MobEffectRegistry.SLOW_METABOLISM,
            MobEffectRegistry.FRIGHT);

    /**
     * Uma-only rule (design principle 6): with no soul worn, blessings and the other Umamusume
     * states are removed right away and don't come back.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        boolean blessed = BlessingManager.hasAny(player);
        boolean umaState = UMA_ONLY_EFFECTS.stream().anyMatch(player::hasEffect);
        if ((!blessed && !umaState) || !UmapyoiAPI.getUmaSoul(player).isEmpty())
            return;
        BlessingManager.removeAll(player);
        UMA_ONLY_EFFECTS.forEach(player::removeEffect);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getNewDamage() <= 0)
            return;
        // 幸福: any damage cancels it
        entity.removeEffect(BlessingType.HAPPINESS.effect());
        // 悲傷: each hit takes 60 seconds off (not the hit that kills, so it can still protect)
        MobEffectInstance sorrow = entity.getEffect(BlessingType.SORROW.effect());
        if (sorrow != null && entity.getHealth() > 0) {
            int left = sorrow.getDuration() - 60 * 20;
            entity.removeEffect(BlessingType.SORROW.effect());
            if (left > 0)
                entity.addEffect(new MobEffectInstance(BlessingType.SORROW.effect(), left, sorrow.getAmplifier(),
                        false, false, true));
        }
    }

    /** 幸福: running out (not being cancelled) raises motivation by one step. */
    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && instance.is(BlessingType.HAPPINESS.effect())
                && !event.getEntity().level().isClientSide())
            UmaStatusUtils.changeMotivation(event.getEntity(), 1);
    }

    /** 悲傷 III: stop this death, 1 health left, totem particles (no totem animation, other effects stay). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled())
            return;
        if (BlessingManager.level(player, BlessingType.SORROW) < 2)
            return;
        event.setCanceled(true);
        player.setHealth(1.0F);
        UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.SORROW_SAVE);
        player.removeEffect(BlessingType.SORROW.effect());
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(),
                player.getY() + 1.0, player.getZ(), 60, 0.4, 0.8, 0.4, 0.5);
    }

    /** 生存: +3 saturation from any food. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || BlessingManager.level(player, BlessingType.SURVIVAL) < 0)
            return;
        ItemStack stack = event.getItem();
        if (stack.getItem().getFoodProperties(stack, player) == null)
            return;
        FoodData food = player.getFoodData();
        food.setSaturation(Math.min(food.getFoodLevel(), food.getSaturationLevel() + 3.0F));
    }
}
