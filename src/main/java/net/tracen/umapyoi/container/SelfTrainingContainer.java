package net.tracen.umapyoi.container;

import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.Nullable;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.tracen.umapyoi.block.BlockRegistry;
import net.tracen.umapyoi.block.entity.SelfTrainingTerminalBlockEntity;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.career.SkillOffer;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.trait.PersonalityTrait;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SelfTrainingContainer extends AbstractContainerMenu {
    public static final int PAGE_MAIN = 0, PAGE_PERSONALITY = 1, PAGE_SKILL = 2;
    /** Button ids: 0 = start, 100 + n = positive trait n, 200 + n = offered skill n. */
    public static final int BUTTON_START = 0, BUTTON_POSITIVE = 100, BUTTON_SKILL = 200;

    public final SelfTrainingTerminalBlockEntity tileEntity;
    public final ItemStackHandler inventory;
    private final ContainerLevelAccess access;
    /** Client only: the terminal slots are shown on the main page. */
    public int page = PAGE_MAIN;

    public SelfTrainingContainer(int windowId, Inventory playerInventory, SelfTrainingTerminalBlockEntity tileEntity) {
        super(ContainerRegistry.SELF_TRAINING_TERMINAL.get(), windowId);
        this.tileEntity = tileEntity;
        this.inventory = tileEntity.getInventory();
        this.access = ContainerLevelAccess.create(tileEntity.getLevel(), tileEntity.getBlockPos());

        this.addSlot(new TerminalSlot(inventory, 0, 41, 15));
        for (int i = 1; i < 7; i++)
            this.addSlot(new TerminalSlot(inventory, i, 12, 19 + (i - 1) * 26));

        for (int row = 0; row < 3; ++row)
            for (int column = 0; column < 9; ++column)
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + column, 8 + column * 18, 188 + row * 18));
        for (int column = 0; column < 9; ++column)
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 246));
    }

    public SelfTrainingContainer(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        this(windowId, playerInventory, getTileEntity(playerInventory, data));
    }

    private static SelfTrainingTerminalBlockEntity getTileEntity(Inventory playerInventory, FriendlyByteBuf data) {
        Objects.requireNonNull(data, "data cannot be null");
        BlockEntity tile = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (tile instanceof SelfTrainingTerminalBlockEntity terminal)
            return terminal;
        throw new IllegalStateException("Tile entity is not correct! " + tile);
    }

    private class TerminalSlot extends SlotItemHandler {
        TerminalSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getPlaceProblem(tileEntity.getLevel(), this.getItemHandler(), this.getSlotIndex(), stack) == null;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        @Override
        public boolean isActive() {
            return page == PAGE_MAIN;
        }
    }

    /** Why {@code stack} can't go into terminal slot {@code slot} (0 = soul, 1-6 = cards), or null. */
    @Nullable
    public static Component getPlaceProblem(@Nullable Level level, IItemHandler handler, int slot, ItemStack stack) {
        if (slot == 0) {
            if (!stack.is(ItemRegistry.UMA_SOUL.get()))
                return Component.translatable("tooltip.umapyoi.training.need_uma_soul");
            if (!CareerManager.isUndecided(stack))
                return Component.translatable("tooltip.umapyoi.self_training.need_undecided");
        }
        // card rules (and the soul-vs-card supporter rule) are the same as the training terminal's
        return TrainingFacilityContainer.getPlaceProblem(level, handler, slot, stack);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return false;
        if (id == BUTTON_START) {
            tileEntity.start(serverPlayer);
            return true;
        }
        List<PersonalityTrait> positives = PersonalityTrait.positives();
        if (id >= BUTTON_POSITIVE && id < BUTTON_POSITIVE + positives.size()) {
            tileEntity.choosePositive(positives.get(id - BUTTON_POSITIVE));
            return true;
        }
        List<ResourceLocation> skills = SkillOffer.SKILLS;
        if (id >= BUTTON_SKILL && id < BUTTON_SKILL + skills.size()) {
            tileEntity.chooseSkill(skills.get(id - BUTTON_SKILL));
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < 7) {
                if (!this.moveItemStackTo(stack, 7, 43, true))
                    return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 7, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty())
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();
            if (stack.getCount() == moved.getCount())
                return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BlockRegistry.SELF_TRAINING_TERMINAL.get());
    }
}
