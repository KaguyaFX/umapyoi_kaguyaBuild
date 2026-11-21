package net.tracen.umapyoi.career;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.trait.PersonalityEffects;

/**
 * Everyday actions that give career training points,
 * plus death and logout. Movement, harmful effects and spent levels are counted in
 * {@link CareerManager#tick}. Points are passed in thousandths.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public class CareerEvents {

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack stack = event.getItem();
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null)
            return;
        CareerManager.addPoints(player, CareerManager.STAMINA,
                food.nutrition() * (long) UmapyoiConfig.CAREER_STAMINA_PER_FOOD.get() * 1000L);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player))
            return;
        // Approximates "natural regeneration": healing under Regeneration doesn't count.
        if (player.hasEffect(MobEffects.REGENERATION))
            return;
        float healed = Math.min(event.getAmount(), player.getMaxHealth() - player.getHealth());
        if (healed > 0)
            CareerManager.addPoints(player, CareerManager.STAMINA,
                    (long) (healed * UmapyoiConfig.CAREER_STAMINA_PER_HEAL.get() * 1000));
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        float damage = event.getNewDamage();
        if (damage <= 0)
            return;
        if (event.getEntity() instanceof ServerPlayer victim)
            CareerManager.addPoints(victim, CareerManager.GUTS,
                    (long) (damage * UmapyoiConfig.CAREER_GUTS_PER_DAMAGE_TAKEN.get() * PersonalityEffects.combatPermille(victim)));
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
                && attacker != event.getEntity())
            CareerManager.addPoints(attacker, CareerManager.STRENGTH,
                    (long) (damage * UmapyoiConfig.CAREER_STRENGTH_PER_DAMAGE.get() * PersonalityEffects.combatPermille(attacker)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof ServerPlayer player))
            return;
        CareerManager.addPoints(player, CareerManager.GUTS,
                (long) (event.getBlockedDamage() * UmapyoiConfig.CAREER_GUTS_PER_DAMAGE_BLOCKED.get() * PersonalityEffects.combatPermille(player)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player))
            return;
        float hardness = event.getState().getDestroySpeed(event.getLevel(), event.getPos());
        if (hardness > 0)
            CareerManager.addPoints(player, CareerManager.STRENGTH,
                    (long) (hardness * UmapyoiConfig.CAREER_STRENGTH_PER_HARDNESS.get() * 1000));
    }

    /** Wisdom from experience orbs: something every player collects (mobs, ores, smelting). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player))
            return;
        CareerManager.addPoints(player, CareerManager.WISDOM,
                event.getOrb().getValue() * (long) UmapyoiConfig.CAREER_WISDOM_PER_XP.get() * 1000L);
    }

    @SubscribeEvent
    public static void onBrew(PlayerBrewedPotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            CareerManager.addPoints(player, CareerManager.WISDOM,
                    event.getStack().getCount() * (long) UmapyoiConfig.CAREER_WISDOM_PER_POTION.get() * 1000L);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player)
            CareerManager.onDeath(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            CareerManager.onLogout(player);
    }
}
