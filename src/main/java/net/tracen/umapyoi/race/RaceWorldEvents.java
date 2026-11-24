package net.tracen.umapyoi.race;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.events.ResumeActionPointEvent;

/**
 * Rules that hold for everyone in the race world. Hunger stays full: vanilla stops sprinting when
 * hunger runs low, which would fight the race's own stamina. Skill action points come back slower
 * ({@code race_action_point_regen}), and not at all while exhausted in a race. Nobody is hurt or dies
 * there: dying would drop the soul and skip the way out ({@link RaceStash}). (The career clock and
 * events pause there too; see CareerManager and CareerEventHooks.)
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class RaceWorldEvents {
    private RaceWorldEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !RaceWorld.isRaceWorld(player.level()))
            return;
        FoodData food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(5.0F);
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RaceWorld.isRaceWorld(player.level())
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            event.setCanceled(true);
    }

    /** What still kills (a command, the void) puts them back at the course instead, out of any race. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !RaceWorld.isRaceWorld(player.level()))
            return;
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        TimeTrial.cancel(player);
        ServerLevel race = player.serverLevel();
        RaceWorld.sendToCourse(player, race, RacecourseData.get(race));
    }

    /**
     * Action points normally come back one per tick; here only on the ticks the rate allows. The
     * rate grows with wisdom, from race_action_point_regen up to race_action_point_regen_max.
     */
    @SubscribeEvent
    public static void onActionPointRegen(ResumeActionPointEvent event) {
        LivingEntity entity = event.getLivingEntity();
        if (entity == null || !RaceWorld.isRaceWorld(entity.level()))
            return;
        if (entity instanceof ServerPlayer player && TimeTrial.exhausted(player)) {
            event.setCanceled(true);
            return;
        }
        double rate = RaceStats.apRegen(event.getUmaSoul());
        int tick = entity.tickCount;
        if (Math.floor(tick * rate) == Math.floor((tick - 1) * rate))
            event.setCanceled(true);
    }
}
