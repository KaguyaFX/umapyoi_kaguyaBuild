package net.tracen.umapyoi.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.item.data.DataLocation;
import net.tracen.umapyoi.registry.umadata.UmaData;

/**
 * The career keepsake on the ground: remembers which Umamusume it shows. The name comes from the
 * item's data_location when placed and goes back onto the item when broken.
 */
public class CareerKeepsakeBlockEntity extends BlockEntity {
    private ResourceLocation umaName = UmaData.DEFAULT_UMA_ID;

    public CareerKeepsakeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.CAREER_KEEPSAKE.get(), pos, state);
    }

    public ResourceLocation getUmaName() {
        return umaName;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation name = ResourceLocation.tryParse(tag.getString("Uma"));
        if (name != null)
            this.umaName = name;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Uma", umaName.toString());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        DataLocation location = input.get(DataComponentsTypeRegistry.DATA_LOCATION);
        if (location != null)
            this.umaName = location.name();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponentsTypeRegistry.DATA_LOCATION, new DataLocation(umaName));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Uma");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
