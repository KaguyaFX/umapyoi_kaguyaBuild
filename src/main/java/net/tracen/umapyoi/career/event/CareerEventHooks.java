package net.tracen.umapyoi.career.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerAttachments;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.data.tag.UmapyoiItemTags;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;

/** Watches for what completes (or rules out) an event in progress. */
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
        if ((stack.is(ItemRegistry.HACHIMI_MID.get()) || stack.is(ItemRegistry.HACHIMI_BIG.get()))
                && CareerEventManager.isActive(player, CareerEventType.WANT_HACHIMI))
            CareerEventManager.complete(player, CareerEventType.WANT_HACHIMI);
    }

    /** Once a second, every event in progress that depends on where the player is or what they do. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0)
            return;
        EventProgress progress = player.getData(CareerAttachments.EVENT_PROGRESS);
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        if (!CareerManager.isInCareer(soul) || data == null) {
            progress.sprintSeconds = 0;
            return;
        }

        boolean running = false;
        List<CareerEventType> done = new ArrayList<>();
        List<CareerEventType> missed = new ArrayList<>();
        Level level = player.level();
        for (CareerEventData.Active active : data.active()) {
            CareerEventType type = CareerEventType.byId(active.id()).orElse(null);
            if (type == null)
                continue;
            switch (type) {
                case WANT_TO_RUN -> {
                    running = true;
                    if (!player.isSprinting())
                        progress.sprintSeconds = 0;
                    else if (++progress.sprintSeconds >= UmapyoiConfig.EVENT_RUN_SECONDS.get())
                        done.add(type);
                }
                case CHANGE_OUTFIT -> {
                    String outfit = CareerEventType.outfitOf(player);
                    if (!outfit.isEmpty() && !outfit.equals(active.data()))
                        done.add(type);
                }
                case OTHER_BIOME -> {
                    String biome = CareerEventType.biomeOf(player);
                    if (!biome.isEmpty() && !biome.equals(active.data()))
                        done.add(type);
                }
                case WATCH_SUNRISE, WATCH_SUNSET -> {
                    CareerEventType.SkyWindow window = type == CareerEventType.WATCH_SUNRISE
                            ? CareerEventType.SkyWindow.SUNRISE : CareerEventType.SkyWindow.SUNSET;
                    if (level.getDayTime() >= parseLong(active.data()))
                        missed.add(type);
                    else if (window.isNow(level) && level.canSeeSky(BlockPos.containing(player.getEyePosition())))
                        done.add(type);
                }
                default -> {
                }
            }
        }
        if (!running || done.contains(CareerEventType.WANT_TO_RUN))
            progress.sprintSeconds = 0;
        done.forEach(type -> CareerEventManager.complete(player, type));
        missed.forEach(type -> CareerEventManager.cancel(player, type));
    }

    private static long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE; // no note: treat the window as gone
        }
    }
}
