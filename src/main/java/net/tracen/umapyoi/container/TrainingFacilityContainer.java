package net.tracen.umapyoi.container;

import java.util.Objects;
import java.util.function.Predicate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.career.CareerManager;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.tracen.umapyoi.block.BlockRegistry;
import net.tracen.umapyoi.block.entity.TrainingFacilityBlockEntity;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.registry.training.SupportContainer;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TrainingFacilityContainer extends AbstractContainerMenu {

    public final TrainingFacilityBlockEntity tileEntity;
    public final ItemStackHandler inventory;
    private final ContainerData containerData;
    private final ContainerLevelAccess canInteractWithCallable;

    public TrainingFacilityContainer(final int windowId, final Inventory playerInventory,
            final TrainingFacilityBlockEntity tileEntity, ContainerData cookingPotDataIn) {
        super(ContainerRegistry.TRAINING_FACILITY.get(), windowId);
        this.tileEntity = tileEntity;
        this.inventory = tileEntity.getInventory();
        this.containerData = cookingPotDataIn;
        this.canInteractWithCallable = ContainerLevelAccess.create(tileEntity.getLevel(), tileEntity.getBlockPos());
        int startX = 8;

        this.addSlot(new TrainingUmaSlot(tileEntity, inventory, 0, 80, 98));
        for (int i = 1; i < 4; i++) {
            this.addSlot(new TrainingSupportSlot(tileEntity, inventory, i, (i - 1) * 27 + 12, 19));
        }

        for (int i = 4; i < 7; i++) {
            this.addSlot(new TrainingSupportSlot(tileEntity, inventory, i, (i - 4) * 27 + 94, 19));
        }

        // Main Player Inventory
        int startPlayerInvY = 120;
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, 9 + (row * 9) + column, startX + (column * 18),
                        startPlayerInvY + (row * 18)));
            }
        }

        // Hotbar
        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(playerInventory, column, startX + (column * 18), 178));
        }

        this.addDataSlots(cookingPotDataIn);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            if (index >= 0 && index < 7) {
                if (!this.moveItemStackTo(itemStack1, 7, 43, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack1, itemStack);
            } else if (index >= 7) {
                if (index < 43) {
                    if (!this.moveItemStackTo(itemStack1, 0, 7, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemStack1.getCount() == 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack1.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemStack1);
        }

        return itemStack;
    }

    private static TrainingFacilityBlockEntity getTileEntity(final Inventory playerInventory,
            final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        final BlockEntity tileAtPos = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (tileAtPos instanceof TrainingFacilityBlockEntity) {
            return (TrainingFacilityBlockEntity) tileAtPos;
        }
        throw new IllegalStateException("Tile entity is not correct! " + tileAtPos);
    }

    public TrainingFacilityContainer(final int windowId, final Inventory playerInventory, final FriendlyByteBuf data) {
        this(windowId, playerInventory, getTileEntity(playerInventory, data), new SimpleContainerData(4));
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(canInteractWithCallable, playerIn, BlockRegistry.TRAINING_FACILITY.get());
    }

    public static final int BUTTON_AUTO_TRAINING = 0;
    public static final int BUTTON_THREE_YEAR = 1;
    public static final int BUTTON_AUTO_ACCEPT = 2;

    /** Route buttons (three-year career), sent with the vanilla menu button packet. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return false;
        switch (id) {
            case BUTTON_AUTO_TRAINING -> this.tileEntity.chooseAutoTraining();
            case BUTTON_THREE_YEAR -> this.tileEntity.startThreeYearCareer(serverPlayer);
            case BUTTON_AUTO_ACCEPT -> this.tileEntity.toggleAutoAccept();
            default -> {
                return false;
            }
        }
        return true;
    }

    public boolean isAutoAccept() {
        return this.containerData.get(1) != 0;
    }

    @OnlyIn(Dist.CLIENT)
    public int getProgressionScaled() {
        int i = this.containerData.get(0);
        return i != 0 ? i * 130 / TrainingFacilityBlockEntity.MAX_PROCESS_TIME : 0;
    }

    @OnlyIn(Dist.CLIENT)
    public int getAnimation() {
        int i = this.containerData.get(0);
        return i != 0 ? i % 4 : 0;
    }

    public static class TrainingSupportSlot extends SlotItemHandler {
        private final TrainingFacilityBlockEntity tileEntity;

        public TrainingSupportSlot(TrainingFacilityBlockEntity tileEntity, IItemHandler itemHandler, int index,
                int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            this.tileEntity = tileEntity;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getPlaceProblem(this.tileEntity.getLevel(), this.getItemHandler(), this.getSlotIndex(), stack) == null;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }

    public static class TrainingUmaSlot extends SlotItemHandler
    {
        private final TrainingFacilityBlockEntity tileEntity;

        public TrainingUmaSlot(TrainingFacilityBlockEntity tileEntity, IItemHandler itemHandler, int index,
                               int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            this.tileEntity = tileEntity;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getPlaceProblem(this.tileEntity.getLevel(), this.getItemHandler(), this.getSlotIndex(), stack) == null;
        }
    }

    /**
     * Explains why {@code stack} cannot be put into terminal slot {@code slot}
     * (0 = Uma soul, 1-6 = support cards). Returns null when it can.
     * The slots' mayPlace() is defined by this method, so the rule and the
     * message shown to the player always agree.
     */
    @Nullable
    public static Component getPlaceProblem(@Nullable Level level, IItemHandler handler, int slot, ItemStack stack) {
        if (slot == 0) {
            if (!stack.is(ItemRegistry.UMA_SOUL.get()))
                return Component.translatable("tooltip.umapyoi.training.need_uma_soul");
            if (CareerManager.isCareerSoul(stack))
                return Component.translatable("tooltip.umapyoi.training.career_soul");
            for (int i = 1; i < 7; i++) {
                ItemStack other = handler.getStackInSlot(i);
                if (other.isEmpty())
                    continue;
                if (!(other.getItem() instanceof SupportContainer support))
                    return Component.translatable("tooltip.umapyoi.training.invalid_support", other.getHoverName());
                if (!support.canSupport(level, other).test(stack))
                    return Component.translatable("tooltip.umapyoi.training.soul_is_supporter", other.getHoverName());
            }
            return null;
        }

        if (stack.getItem() instanceof SupportContainer support) {
            Predicate<ItemStack> check = support.canSupport(level, stack);
            ItemStack soul = handler.getStackInSlot(0);
            if (!soul.isEmpty() && !check.test(soul))
                return Component.translatable("tooltip.umapyoi.training.card_has_soul", soul.getHoverName());
            for (int i = 1; i < 7; i++) {
                ItemStack other = handler.getStackInSlot(i);
                if (i == slot || other.isEmpty())
                    continue;
                if (!check.test(other))
                    return Component.translatable("tooltip.umapyoi.training.card_conflict", other.getHoverName());
            }
        }
        return null;
    }
}
