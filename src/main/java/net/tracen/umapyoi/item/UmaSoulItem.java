package net.tracen.umapyoi.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.career.CareerState;
import net.tracen.umapyoi.career.UmaCareerData;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.registry.umadata.UmaData;
import net.tracen.umapyoi.registry.umadata.UmaDataBasicStatus;
import net.tracen.umapyoi.trait.PersonalityTrait;
import net.tracen.umapyoi.trait.UmaTraits;
import net.tracen.umapyoi.utils.*;

public class UmaSoulItem extends Item{
    private static final Comparator<Reference<UmaData>> COMPARATOR = new UmaDataComparator();
    
    public UmaSoulItem() {
        super(Umapyoi.defaultItemProperties().stacksTo(1));
    }
    
    public static Stream<Reference<UmaData>> sortedUmaDataList(HolderLookup.Provider provider) {
        return provider.lookupOrThrow(UmaData.REGISTRY_KEY).listElements().sorted(UmaSoulItem.COMPARATOR);
    }

    @Override
    public String getCreatorModId(ItemStack itemStack) {
        return UmaSoulUtils.getName(itemStack).getNamespace();
    }
    
    @Override
    public Component getName(ItemStack pStack) {
        GachaRanking ranking = GachaRanking.getGachaRanking(pStack);
        if(ranking == GachaRanking.EASTER_EGG) return super.getName(pStack).copy().withStyle(ChatFormatting.GREEN);
        return super.getName(pStack);
    }
    
    @Override
    public String getDescriptionId(ItemStack pStack) {
        return Util.makeDescriptionId("umadata", UmaSoulUtils.getName(pStack));
    }
    
    @Override
    public boolean isFoil(ItemStack pStack) {
        return !pStack.has(DataComponentsTypeRegistry.UMADATA_TRAINING);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
    		TooltipFlag tooltipFlag) {
    	super.appendHoverText(stack, context, tooltip, tooltipFlag);
        CareerState careerState = CareerManager.getState(stack);
        UmaCareerData careerData = stack.get(DataComponentsTypeRegistry.CAREER_DATA);
        if (careerState == CareerState.UNDECIDED)
            tooltip.add(Component.translatable("tooltip.umapyoi.career.undecided").withStyle(ChatFormatting.YELLOW));
        else if (careerState == CareerState.IN_CAREER && careerData != null)
            tooltip.add(Component.translatable("tooltip.umapyoi.career.in_career",
                    CareerManager.periodName(careerData.periodIndex())).withStyle(ChatFormatting.LIGHT_PURPLE));
        else if (careerState == CareerState.FINISHED && stack.has(DataComponentsTypeRegistry.UMADATA_TRAINING))
            // Only until it is registered; registering removes UMADATA_TRAINING.
            tooltip.add(Component.translatable("tooltip.umapyoi.career.finished").withStyle(ChatFormatting.LIGHT_PURPLE));
        int ranking = ResultRankingUtils.getRanking(stack);
        if(!stack.has(DataComponentsTypeRegistry.UMADATA_TRAINING))
            tooltip.add(Component.translatable("tooltip.umapyoi.uma_soul.ranking", UmaStatusUtils.getStatusLevel(ranking))
                            .withStyle(ChatFormatting.GOLD));
        if (Screen.hasShiftDown() || !UmapyoiConfig.TOOLTIP_SWITCH.get()) {
            tooltip.add(
                    Component.translatable("tooltip.umapyoi.uma_soul.soul_details").withStyle(ChatFormatting.AQUA));
            UmaDataBasicStatus property = UmaSoulUtils.getProperty(stack);
            UmaDataBasicStatus maxProperty = UmaSoulUtils.getMaxProperty(stack);

            tooltip.add(Component.translatable("tooltip.umapyoi.uma_soul.speed_details",
                    UmaStatusUtils.getStatusLevel(property.speed()),
                    UmaStatusUtils.getStatusLevel(maxProperty.speed()))
                            .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("tooltip.umapyoi.uma_soul.stamina_details",
                    UmaStatusUtils.getStatusLevel(property.stamina()),
                    UmaStatusUtils.getStatusLevel(maxProperty.stamina()))
                            .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("tooltip.umapyoi.uma_soul.strength_details",
                    UmaStatusUtils.getStatusLevel(property.strength()),
                    UmaStatusUtils.getStatusLevel(maxProperty.strength()))
                            .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("tooltip.umapyoi.uma_soul.guts_details",
                    UmaStatusUtils.getStatusLevel(property.guts()),
                    UmaStatusUtils.getStatusLevel(maxProperty.guts()))
                            .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("tooltip.umapyoi.uma_soul.wisdom_details",
                    UmaStatusUtils.getStatusLevel(property.wisdom()),
                    UmaStatusUtils.getStatusLevel(maxProperty.wisdom()))
                            .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.literal(""));

            tooltip.add(Component.translatable("tooltip.umapyoi.uma_soul.aptitude.details").withStyle(ChatFormatting.AQUA));
            Aptitude[] surfaceAptitudes = UmaSoulUtils.getSurfaceAptitude(stack);
            tooltip.add(
                    Component.translatable("tooltip.umapyoi.uma_soul.aptitude.turf", surfaceAptitudes[0].styledComponent()).withStyle(ChatFormatting.GREEN)
                            .append(" / ").withStyle(ChatFormatting.RESET)
                            .append(Component.translatable("tooltip.umapyoi.uma_soul.aptitude.dirt", surfaceAptitudes[1].styledComponent()).withStyle(ChatFormatting.GOLD))
                            .append(" / ").withStyle(ChatFormatting.RESET)
                            .append(Component.translatable("tooltip.umapyoi.uma_soul.aptitude.synthetic", surfaceAptitudes[2].styledComponent()).withStyle(ChatFormatting.GOLD))
            );
            Aptitude[] distanceAptitudes = UmaSoulUtils.getDistanceAptitude(stack);
            tooltip.add(
                    Component.translatable("tooltip.umapyoi.uma_soul.aptitude.short", distanceAptitudes[0].styledComponent())
                            .append(" / ")
                            .append(Component.translatable("tooltip.umapyoi.uma_soul.aptitude.miles", distanceAptitudes[1].styledComponent())
                                    .append(" / ")
                                    .append(Component.translatable("tooltip.umapyoi.uma_soul.aptitude.medium", distanceAptitudes[2].styledComponent()))
                                    .append(" / ")
                                    .append(Component.translatable("tooltip.umapyoi.uma_soul.aptitude.long", distanceAptitudes[3].styledComponent())))
            );
            tooltip.add(
                    Component.translatable("tooltip.umapyoi.uma_soul.aptitude.strategy", Component.translatable("tooltip.umapyoi.uma_soul.aptitude.strategy." + UmaSoulUtils.getPosition(stack).name().toLowerCase()))
            );
            appendTraits(UmaTraits.of(stack), tooltip);
        } else {
            tooltip.add(Component.translatable("tooltip.umapyoi.press_shift_for_details")
                    .withStyle(ChatFormatting.AQUA));
        }
      }

    /** Uma traits, personality and food likes; souls without any (old or command-made) show nothing. */
    @OnlyIn(Dist.CLIENT)
    private static void appendTraits(UmaTraits traits, List<Component> tooltip) {
        if (traits.isEmpty())
            return;
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.umapyoi.uma_soul.traits").withStyle(ChatFormatting.AQUA));

        List<Component> uma = traits.umaEntries().stream()
                .map(e -> (Component) e.displayName().copy()
                        .withStyle(e.trait().positive() ? ChatFormatting.GREEN : ChatFormatting.RED))
                .toList();
        if (!uma.isEmpty())
            tooltip.add(Component.translatable("gui.umapyoi.self_training.uma_traits").withStyle(ChatFormatting.GRAY)
                    .append(UmaTraits.joined(uma)));

        List<PersonalityTrait> personality = new ArrayList<>(traits.negatives());
        personality.addAll(traits.positives());
        if (!personality.isEmpty())
            tooltip.add(Component.translatable("gui.umapyoi.self_training.personality_line")
                    .withStyle(ChatFormatting.GRAY)
                    .append(UmaTraits.joined(personality.stream()
                            .map(t -> (Component) t.displayName().copy()
                                    .withStyle(t.positive() ? ChatFormatting.GREEN : ChatFormatting.RED))
                            .toList())));

        List<Component> likes = traits.likedItems().stream().map(Item::getDescription).toList();
        if (!likes.isEmpty())
            tooltip.add(Component.translatable("gui.umapyoi.self_training.likes").append(UmaTraits.joined(likes))
                    .withStyle(ChatFormatting.GRAY));
        List<Component> dislikes = traits.dislikedItems().stream().map(Item::getDescription).toList();
        if (!dislikes.isEmpty())
            tooltip.add(Component.translatable("gui.umapyoi.self_training.dislikes")
                    .append(UmaTraits.joined(dislikes)).withStyle(ChatFormatting.GRAY));
    }

    private static class UmaDataComparator implements Comparator<Reference<UmaData>> {
        @Override
        public int compare(Reference<UmaData> left, Reference<UmaData> right) {
            var leftRanking = left.value().ranking();
            var rightRanking = right.value().ranking();
            if(leftRanking == rightRanking) {
                String leftName = left.key().location().toString();
                String rightName = right.key().location().toString();
                return leftName.compareToIgnoreCase(rightName);
            }
            return leftRanking.compareTo(rightRanking);
        }
    }
}
