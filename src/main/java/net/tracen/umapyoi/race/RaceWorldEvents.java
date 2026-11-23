package net.tracen.umapyoi.race;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.events.ResumeActionPointEvent;

/**
 * Rules that hold for everyone in the race world. Hunger stays full: vanilla stops sprinting when
 * hunger runs low, which would fight the race's own stamina. Skill action points come back slower
 * ({@code race_action_point_regen}). (The career clock and events pause there too; see
 * CareerManager and CareerEventHooks.)
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

    /** Action points normally come back one per tick; here only on the ticks the rate allows. */
    @SubscribeEvent
    public static void onActionPointRegen(ResumeActionPointEvent event) {
        LivingEntity entity = event.getLivingEntity();
        if (entity == null || !RaceWorld.isRaceWorld(entity.level()))
            return;
        double rate = UmapyoiConfig.RACE_ACTION_POINT_REGEN.get();
        int tick = entity.tickCount;
        if (Math.floor(tick * rate) == Math.floor((tick - 1) * rate))
            event.setCanceled(true);
    }
}
