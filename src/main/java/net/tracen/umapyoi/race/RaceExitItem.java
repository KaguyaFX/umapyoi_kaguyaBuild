package net.tracen.umapyoi.race;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.tracen.umapyoi.Umapyoi;

/**
 * The way out, in the last hotbar slot of everyone who came to a race: right-clicking with it goes home.
 * For a runner whose entry is settled that is giving up, so it takes sneaking too; and from the stalls until
 * the result is out, runners don't have it at all.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
final class RaceExitItem {
    private static final String TAG = "umapyoi_race_exit";
    private static final int SLOT = 8;

    private RaceExitItem() {
    }

    static void give(ServerPlayer player) {
        // Whoever still has their things on them (they couldn't be put in keeping) is sent back anyway.
        if (!RaceStash.marked(player) || !player.getInventory().getItem(SLOT).isEmpty())
            return;
        ItemStack stack = new ItemStack(Items.OAK_DOOR);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("umapyoi.meeting.exit").withStyle(style -> style.withItalic(false).withColor(ChatFormatting.YELLOW)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("umapyoi.meeting.exit.hint").withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GRAY)))));
        player.getInventory().setItem(SLOT, stack);
    }

    /** Takes it off a player. */
    static void take(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (isExit(player.getInventory().getItem(slot)))
                player.getInventory().setItem(slot, ItemStack.EMPTY);
        if (isExit(player.containerMenu.getCarried()))
            player.containerMenu.setCarried(ItemStack.EMPTY);
    }

    private static boolean isExit(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(TAG);
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        use(event, event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        use(event, event);
    }

    private static void use(PlayerInteractEvent event, ICancellableEvent cancel) {
        if (!RaceWorld.isRaceWorld(event.getLevel()) || !isExit(event.getItemStack()))
            return;
        cancel.setCanceled(true);
        if (event instanceof PlayerInteractEvent.RightClickItem item)
            item.setCancellationResult(InteractionResult.SUCCESS);
        else if (event instanceof PlayerInteractEvent.RightClickBlock block)
            block.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (RaceMeeting.bound(player)) {
            player.displayClientMessage(Component.translatable("umapyoi.meeting.exit.racing").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (RaceMeeting.committed(player) && !player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("umapyoi.meeting.exit.sneak").withStyle(ChatFormatting.RED), true);
            return;
        }
        // Not in the middle of handling the click.
        player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
            if (!player.hasDisconnected())
                RaceMeeting.leave(player);
        }));
    }
}
