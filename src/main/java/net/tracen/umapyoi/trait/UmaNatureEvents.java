package net.tracen.umapyoi.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerAttachments;
import net.tracen.umapyoi.data.tag.UmapyoiItemTags;
import net.tracen.umapyoi.utils.UmaStatusUtils;
import net.tracen.umapyoi.advancements.trigger.UmapyoiActionTrigger;
import net.tracen.umapyoi.registry.umadata.Motivations;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * What any player wearing a soul is like, trained or not. The base health and
 * speed are soul modifiers (UmaSoulCuriosWrapper) and sprint hunger is in PlayerFoodExhaustionMixin.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public class UmaNatureEvents {

    private static boolean isUma(Player player) {
        return !UmapyoiAPI.getUmaSoul(player).isEmpty();
    }

    /** Legs are precious: falls count as a little higher. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && event.getDistance() > 0.5F && isUma(player))
            event.setDistance(event.getDistance() + UmapyoiConfig.UMA_EXTRA_FALL_DISTANCE.get().floatValue());
    }

    /** An Umamusume doesn't ride a horse (llamas and camels are fine). */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.isMounting() || !(event.getEntityMounting() instanceof Player player))
            return;
        if (!(event.getEntityBeingMounted() instanceof AbstractHorse horse) || horse instanceof Llama || horse instanceof Camel)
            return;
        if (!isUma(player))
            return;
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable("umapyoi.uma.no_riding"), true);
            UmapyoiActionTrigger.fire(serverPlayer, UmapyoiActionTrigger.TRY_RIDE_HORSE);
        }
    }

    /** Carrots and sweets fill an Umamusume more. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack stack = event.getItem();
        int extra = UmapyoiConfig.UMA_TREAT_EXTRA_FOOD.get();
        if (extra > 0 && stack.is(UmapyoiItemTags.UMA_TREATS) && isUma(player))
            player.getFoodData().eat(extra, 0.25F);
    }

    // ---------------------------------------------------------------- sensitive ears

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        Vec3 center = event.getExplosion().center();
        startle(level, center, UmapyoiConfig.EAR_EXPLOSION_RANGE.get(), UmapyoiConfig.EAR_EXPLOSION_CHANCE.get());
    }

    @SubscribeEvent
    public static void onLightning(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level && !event.loadedFromDisk())
            startle(level, bolt.position(), UmapyoiConfig.EAR_THUNDER_RANGE.get(), UmapyoiConfig.EAR_THUNDER_CHANCE.get());
    }

    /** A loud noise near an Umamusume may lower motivation, at most once per cooldown. */
    private static void startle(ServerLevel level, Vec3 center, int range, double chance) {
        double rangeSqr = (double) range * range;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.position().distanceToSqr(center) > rangeSqr || !isUma(player))
                continue;
            TraitState state = player.getData(CareerAttachments.TRAIT_STATE);
            long now = level.getGameTime();
            if (now < state.earCooldownUntil || player.getRandom().nextDouble() >= chance)
                continue;
            state.earCooldownUntil = now + UmapyoiConfig.EAR_COOLDOWN_SECONDS.get() * 20L;
            UmaStatusUtils.changeMotivation(player, -1);
        }
    }

    // ---------------------------------------------------------------- a helmet on the ears

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0)
            return;
        TraitState state = player.getData(CareerAttachments.TRAIT_STATE);
        achievements(player);
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty() || head.is(UmapyoiItemTags.EAR_FRIENDLY_HEADWEAR) || !isUma(player) || player.isSleeping()) {
            state.helmetSeconds = 0;
            return;
        }
        if (++state.helmetSeconds >= UmapyoiConfig.HELMET_MOOD_SECONDS.get()) {
            state.helmetSeconds = 0;
            UmaStatusUtils.changeMotivation(player, -1);
            UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.HELMET_EARS);
        }
    }

    /** Once a second: achievements about what an Umamusume is wearing, sitting on, and feeling. */
    private static void achievements(ServerPlayer player) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (soul.isEmpty())
            return;
        UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.WEAR_SOUL);
        if (!UmapyoiAPI.getUmaSuit(player).isEmpty())
            UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.WEAR_COSTUME);
        if (player.getVehicle() instanceof AbstractHorse horse && !(horse instanceof Llama) && !(horse instanceof Camel))
            UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.RIDE_HORSE_AS_UMA);
        Motivations motivation = UmaSoulUtils.getMotivation(soul);
        if (motivation == Motivations.BAD)
            UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.MOOD_BAD);
        else if (motivation == Motivations.PERFECT)
            UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.MOOD_PERFECT);
    }
}
