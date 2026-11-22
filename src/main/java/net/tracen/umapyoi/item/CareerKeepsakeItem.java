package net.tracen.umapyoi.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.item.data.DataLocation;

/**
 * "{Umamusume}'s memories": given when a three-year career ends, never in the creative menu or the
 * recipe viewers. One item for every Umamusume; the data_location picks whose figure it is.
 */
public class CareerKeepsakeItem extends BlockItem {
    public CareerKeepsakeItem(Block block) {
        super(block, new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public Component getName(ItemStack stack) {
        DataLocation location = stack.get(DataComponentsTypeRegistry.DATA_LOCATION);
        if (location == null)
            return super.getName(stack);
        return Component.translatable("item.umapyoi.career_keepsake.named",
                Component.translatable(Util.makeDescriptionId("umadata", location.name())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.umapyoi.career_keepsake.desc").withStyle(ChatFormatting.GRAY,
                ChatFormatting.ITALIC));
    }
}
