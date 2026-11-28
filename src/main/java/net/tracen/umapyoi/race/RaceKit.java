package net.tracen.umapyoi.race;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.item.ItemRegistry;

/**
 * What everyone who comes to a race is lent: a phone, a spyglass, and the way out ({@link RaceExitItem}).
 * Runners hand it in at the stalls and get it again when the result is out.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
final class RaceKit {
    private static final String TAG = "umapyoi_race_kit";
    private static final int PHONE_SLOT = 0;
    private static final int SPYGLASS_SLOT = 1;
    /** The clock that skips a meeting ahead, for whoever may (RaceMeeting#mayHurry). */
    private static final String HURRY_TAG = "umapyoi_race_hurry";
    private static final int HURRY_SLOT = 7;

    private RaceKit() {
    }

    static void give(ServerPlayer player) {
        RaceExitItem.give(player);
        // Whoever still has their things on them (they couldn't be put in keeping) is sent back anyway.
        if (!RaceStash.marked(player))
            return;
        lend(player, new ItemStack(ItemRegistry.RACE_PHONE.get()), PHONE_SLOT);
        lend(player, new ItemStack(Items.SPYGLASS), SPYGLASS_SLOT);
        giveHurry(player);
    }

    /** The clock that skips the meeting ahead, while it can be and for whoever may. */
    static void giveHurry(ServerPlayer player) {
        if (!RaceMeeting.hurryable() || !RaceMeeting.mayHurry(player))
            return;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++)
            if (isHurry(inventory.getItem(i)))
                return;
        ItemStack stack = new ItemStack(Items.CLOCK);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG, true);
        tag.putBoolean(HURRY_TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("umapyoi.meeting.hurry").withStyle(style -> style.withItalic(false).withColor(ChatFormatting.YELLOW)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("umapyoi.meeting.hurry.hint", RaceMeeting.hurrySeconds())
                .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GRAY)))));
        if (inventory.getItem(HURRY_SLOT).isEmpty())
            inventory.setItem(HURRY_SLOT, stack);
        else
            inventory.add(stack);
    }

    /** Takes the clock off a player: the meeting was skipped ahead, or it's too late for that. */
    static void takeHurry(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (isHurry(player.getInventory().getItem(slot)))
                player.getInventory().setItem(slot, ItemStack.EMPTY);
        if (isHurry(player.containerMenu.getCarried()))
            player.containerMenu.setCarried(ItemStack.EMPTY);
    }

    static boolean isHurry(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(HURRY_TAG);
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        useHurry(event, event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        useHurry(event, event);
    }

    private static void useHurry(PlayerInteractEvent event, ICancellableEvent cancel) {
        if (!RaceWorld.isRaceWorld(event.getLevel()) || !isHurry(event.getItemStack()))
            return;
        cancel.setCanceled(true);
        if (event instanceof PlayerInteractEvent.RightClickItem item)
            item.setCancellationResult(InteractionResult.SUCCESS);
        else if (event instanceof PlayerInteractEvent.RightClickBlock block)
            block.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        Component problem = RaceMeeting.hurry(player);
        if (problem != null)
            player.displayClientMessage(problem.copy().withStyle(ChatFormatting.RED), true);
    }

    private static void lend(ServerPlayer player, ItemStack stack, int slot) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++)
            if (inventory.getItem(i).is(stack.getItem()))
                return;
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (inventory.getItem(slot).isEmpty())
            inventory.setItem(slot, stack);
        else
            inventory.add(stack);
    }

    /** Takes it all back. */
    static void take(ServerPlayer player) {
        RaceExitItem.take(player);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (lent(player.getInventory().getItem(slot)))
                player.getInventory().setItem(slot, ItemStack.EMPTY);
        if (lent(player.containerMenu.getCarried()))
            player.containerMenu.setCarried(ItemStack.EMPTY);
    }

    private static boolean lent(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(TAG);
    }

    /** It isn't thrown away: back into the pocket. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!(event.getPlayer() instanceof ServerPlayer player) || !RaceWorld.isRaceWorld(player.level())
                || !lent(stack) && !RaceExitItem.isExit(stack))
            return;
        event.setCanceled(true);
        player.getInventory().add(stack.copy());
    }
}
