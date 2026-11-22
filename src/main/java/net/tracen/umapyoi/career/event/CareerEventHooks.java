package net.tracen.umapyoi.career.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.career.CareerAttachments;
import net.tracen.umapyoi.data.tag.UmapyoiItemTags;

/** Watches for what completes an event in progress. */
@EventBusSubscriber(modid = Umapyoi.MODID)
public class CareerEventHooks {

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack stack = event.getItem();
        if (stack.getItem().getFoodProperties(stack, player) == null)
            return;
        if (CareerEventManager.isActive(player, CareerEventType.HUNGRY))
            CareerEventManager.complete(player, CareerEventType.HUNGRY);
        if (stack.is(UmapyoiItemTags.CARROT_FOODS) && CareerEventManager.isActive(player, CareerEventType.WANT_CARROT))
            CareerEventManager.complete(player, CareerEventType.WANT_CARROT);
    }

    /** Once a second: 想跑一跑 counts seconds of sprinting in a row. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0)
            return;
        EventProgress progress = player.getData(CareerAttachments.EVENT_PROGRESS);
        if (!player.isSprinting() || !CareerEventManager.isActive(player, CareerEventType.WANT_TO_RUN)) {
            progress.sprintSeconds = 0;
            return;
        }
        if (++progress.sprintSeconds >= UmapyoiConfig.EVENT_RUN_SECONDS.get()) {
            progress.sprintSeconds = 0;
            CareerEventManager.complete(player, CareerEventType.WANT_TO_RUN);
        }
    }
}
