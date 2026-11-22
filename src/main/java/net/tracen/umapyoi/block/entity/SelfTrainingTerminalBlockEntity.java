package net.tracen.umapyoi.block.entity;

import org.jetbrains.annotations.NotNull;

import cn.mcmod_mmf.mmlib.block.entity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.career.SkillOffer;
import net.tracen.umapyoi.container.SelfTrainingContainer;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.trait.PersonalityTrait;
import net.tracen.umapyoi.trait.UmaTraits;
import net.tracen.umapyoi.advancements.trigger.UmapyoiActionTrigger;

/**
 * Self-training terminal: slot 0 = undecided soul, 1-6 = support cards. Nothing happens until the
 * player presses start. Choices (positive personality, basic skill) are stored on the soul itself.
 */
public class SelfTrainingTerminalBlockEntity extends SyncedBlockEntity implements MenuProvider {
    private final ItemStackHandler inventory;

    public SelfTrainingTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SELF_TRAINING_TERMINAL.get(), pos, state);
        this.inventory = new ItemStackHandler(7) {
            @Override
            protected void onContentsChanged(int slot) {
                inventoryChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return SelfTrainingContainer.getPlaceProblem(level, this, slot, stack) == null;
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == 0 ? 1 : super.getSlotLimit(slot);
            }
        };
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public NonNullList<ItemStack> getDroppableInventory() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int i = 0; i < 7; ++i)
            drops.add(inventory.getStackInSlot(i));
        return drops;
    }

    private ItemStack soul() {
        return inventory.getStackInSlot(0);
    }

    public void choosePositive(PersonalityTrait trait) {
        ItemStack soul = soul();
        if (!trait.positive() || !CareerManager.isUndecided(soul))
            return;
        UmaTraits traits = UmaTraits.of(soul);
        if (traits.isExcluded(trait))
            return;
        ItemStack result = soul.copy();
        result.set(DataComponentsTypeRegistry.UMA_TRAITS, traits.withPositive(trait));
        inventory.setStackInSlot(0, result);
    }

    public void chooseSkill(ResourceLocation skill) {
        ItemStack soul = soul();
        if (!CareerManager.isUndecided(soul) || !SkillOffer.isAvailable(soul, skill))
            return;
        ItemStack result = soul.copy();
        result.set(DataComponentsTypeRegistry.CAREER_SKILL, skill);
        inventory.setStackInSlot(0, result);
    }

    /** Starts the three-year career: needs a positive personality; learns the picked basic skill. */
    public void start(ServerPlayer player) {
        ItemStack soul = soul();
        if (!(this.level instanceof ServerLevel serverLevel) || !CareerManager.isUndecided(soul))
            return;
        if (UmaTraits.of(soul).positives().isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.umapyoi.self_training.need_positive"), true);
            return;
        }
        ResourceLocation skill = soul.get(DataComponentsTypeRegistry.CAREER_SKILL);
        if (!CareerManager.startCareer(serverLevel, inventory, player))
            return;
        UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.SELF_TRAINING);
        if (skill != null) {
            ItemStack started = inventory.getStackInSlot(0).copy();
            SkillOffer.grant(started, skill);
            started.remove(DataComponentsTypeRegistry.CAREER_SKILL);
            inventory.setStackInSlot(0, started);
        }
    }

    @Override
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.loadAdditional(compound, registries);
        inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.saveAdditional(compound, registries);
        compound.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new SelfTrainingContainer(id, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.umapyoi.self_training_terminal");
    }
}
