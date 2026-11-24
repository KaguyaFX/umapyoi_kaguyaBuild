package net.tracen.umapyoi.container;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.tracen.umapyoi.block.BlockRegistry;
import net.tracen.umapyoi.block.entity.RaceRegisterBlockEntity;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.race.RaceMeeting;
import net.tracen.umapyoi.race.RaceMeetingClient;
import net.tracen.umapyoi.utils.Position;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;

import static net.tracen.umapyoi.block.entity.RaceRegisterBlockEntity.DATA_SLOT_SIZE;

public class RaceContainer extends AbstractContainerMenu {
    public final RaceRegisterBlockEntity tileEntity;
    public final ItemStackHandler inventory;
    private final ContainerData containerData;
    private final ContainerLevelAccess canInteractWithCallable;

    private static RaceRegisterBlockEntity getTileEntity(final Inventory playerInventory,
                                                             final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        final BlockEntity tileAtPos = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (tileAtPos instanceof RaceRegisterBlockEntity) {
            return (RaceRegisterBlockEntity) tileAtPos;
        }
        throw new IllegalStateException("Tile entity is not correct! " + tileAtPos);
    }

    public RaceContainer(final int i, final Inventory playerInv, final FriendlyByteBuf data) {
        this(i, playerInv, getTileEntity(playerInv, data), new SimpleContainerData(DATA_SLOT_SIZE));
    }

    // ---------------------------------------------------------------- pages

    /** The quick race in the terminal, and the race meeting. */
    public static final int PAGE_QUICK = 0;
    public static final int PAGE_MEETING = 1;
    /** Button ids (0 and 1 show a page). */
    public static final int BUTTON_CREATE = 10, BUTTON_CREATE_RUN = 11, BUTTON_ENTER = 12, BUTTON_WATCH = 13, BUTTON_WITHDRAW = 14;
    private static final int QUICK_SLOTS = 6;
    private static final int PLAYER_SLOTS = 36;
    /** Where the ticket for a meeting goes; whatever is in it goes back to the player when they close the menu. */
    public static final int MEETING_SLOT = QUICK_SLOTS + PLAYER_SLOTS;

    private final Container meetingTicket = new SimpleContainer(1);
    private int page = PAGE_QUICK;

    public int getPage() {
        return page;
    }

    /** Client side; the server is told through the button packet. */
    public void setPage(int page) {
        this.page = page;
    }

    /** While a race is being held the meeting page shows nothing but that. */
    private boolean showsMeeting() {
        return page == PAGE_MEETING && tileEntity.getLevel() != null && tileEntity.getLevel().isClientSide() && RaceMeetingClient.current() != null;
    }

    public ItemStack getMeetingTicket() {
        return meetingTicket.getItem(0);
    }

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (id == PAGE_QUICK || id == PAGE_MEETING) {
            this.page = id;
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer))
            return false;
        Component problem = null;
        switch (id) {
            case BUTTON_CREATE, BUTTON_CREATE_RUN -> {
                ItemStack ticket = getMeetingTicket();
                if (!ticket.is(ItemRegistry.UMA_RACE_TICKET.get()))
                    return false;
                problem = RaceMeeting.apply(serverPlayer, ticket, id == BUTTON_CREATE_RUN);
                if (problem == null) {
                    ticket.shrink(1);
                    meetingTicket.setChanged();
                }
            }
            case BUTTON_ENTER -> problem = RaceMeeting.enter(serverPlayer);
            case BUTTON_WATCH -> problem = RaceMeeting.watch(serverPlayer);
            case BUTTON_WITHDRAW -> problem = RaceMeeting.withdraw(serverPlayer);
            default -> {
                return false;
            }
        }
        if (problem != null)
            serverPlayer.sendSystemMessage(problem.copy().withStyle(ChatFormatting.RED));
        return true;
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
        if (!player.level().isClientSide())
            this.clearContainer(player, meetingTicket);
    }

    public static class UmaSoulSlot extends SlotItemHandler {
        public UmaSoulSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return stack.is(ItemRegistry.UMA_SOUL.get());
        }

        @Override
        public int getMaxStackSize(@Nonnull ItemStack stack) {
            return 1;
        }
    }

    public RaceContainer(final int i, final Inventory playerInv, final RaceRegisterBlockEntity te, ContainerData containerData) {
        super(ContainerRegistry.RACE_REGISTER.get(), i);
        this.tileEntity = te;
        this.inventory = te.getInventory();
        this.containerData = containerData;
        assert tileEntity.getLevel() != null;
        this.canInteractWithCallable = ContainerLevelAccess.create(tileEntity.getLevel(), tileEntity.getBlockPos());

        this.addSlot(new UmaSoulSlot(this.inventory, 0, 26, 83) {
            @Override
            public boolean isActive() {
                return page == PAGE_QUICK;
            }
        });
        this.addSlot(new SlotItemHandler(this.inventory, 1, 44, 83){
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ItemRegistry.UMA_RACE_TICKET.get());
            }

            @Override
            public boolean isActive() {
                return page == PAGE_QUICK;
            }
        });

        for (int j = 0; j < 4; j++) {
            // actually, I am not so sure that if bit ops optimization still works in java
            this.addSlot(new SlotItemHandler(this.inventory, j + 2, (j & 1) * 18 + 98, 75 + (j >> 1) * 18) {
                @Override
                public boolean isActive() {
                    return page == PAGE_QUICK;
                }
            });
        }

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new PlayerSlot(playerInv, 9 + (row * 9) + column, 8 + (column * 18),
                        120 + (row * 18))); // inline playerInventoryY=120 for opt.
            }
        }

        for (int column = 0; column < 9; ++column) {
            this.addSlot(new PlayerSlot(playerInv, column, 8 + (column * 18), 178));
        }

        // Added last so the other slots keep their numbers.
        this.addSlot(new Slot(meetingTicket, 0, 11, 27) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return stack.is(ItemRegistry.UMA_RACE_TICKET.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return page == PAGE_MEETING && !showsMeeting();
            }
        });

        this.addDataSlots(containerData);
    }

    /** The player's own slots, out of the way while a race being held fills the page. */
    private final class PlayerSlot extends Slot {
        private PlayerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return !showsMeeting();
        }
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player pPlayer, int pIndex) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot.hasItem()) {
            ItemStack stack1 = slot.getItem();
            stack = stack1.copy();

            if (pIndex >= 0 && pIndex < 6 || pIndex == MEETING_SLOT) {
                // from container to player inventory
                if (!this.moveItemStackTo(stack1, 6, 6 + 36, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(stack1, stack);
            } else if (pIndex >= 6) {
                // from player inventory to container
                if (pIndex < 6 + 36) {
                    // only move from 0 to 1
                    if (!(page == PAGE_MEETING ? this.moveItemStackTo(stack1, MEETING_SLOT, MEETING_SLOT + 1, false)
                            : this.moveItemStackTo(stack1, 0, 2, false))) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack1.getCount() == 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack1.getCount() == stack.getCount()) return ItemStack.EMPTY;

            slot.onTake(pPlayer, stack1);
        }

        return stack;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(canInteractWithCallable, playerIn, BlockRegistry.RACE_REGISTER_BLOCK.get());
    }

    @OnlyIn(Dist.CLIENT)
    public int getProgressInTick() {
        return this.containerData.get(0);
    }

    @OnlyIn(Dist.CLIENT)
    public float getProgression() {
        return this.getProgressInTick() / (float) this.containerData.get(1);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean shallSoulWin() {
        return this.containerData.get(2) != 0;
    }

    @OnlyIn(Dist.CLIENT)
    public long getWinnerReplaceSeed() {
        return ((long) this.containerData.get(3)) | ((long) this.containerData.get(4) << 32);
    }

    @OnlyIn(Dist.CLIENT)
    public int getAnimationTickMod(int modVal) {
        return Math.floorMod(this.containerData.get(0), modVal);
    }

    @OnlyIn(Dist.CLIENT)
    public int getGoalType() {
        int indices = this.containerData.get(5);
        if (indices == -1) indices = 0;
        return (indices < 0 ? -indices : Math.min(5 - indices, 3));
    }

    @OnlyIn(Dist.CLIENT)
    public double getBaseScaleFactor() {
        int valueInInt = this.containerData.get(6);
        long valueInLong = valueInInt & 0xffffffffL;
        return valueInLong / (double) ((1L << 32) - 1);
    }

    @OnlyIn(Dist.CLIENT)
    public Position getSoulTactic() {
        return Position.values()[this.containerData.get(8)];
    }
}
