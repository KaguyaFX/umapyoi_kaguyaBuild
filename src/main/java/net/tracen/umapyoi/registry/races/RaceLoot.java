package net.tracen.umapyoi.registry.races;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.utils.RaceRanking;

/** What a race pays: its own loot table, or else the one of its racecourse and grade, or of its grade. */
public final class RaceLoot {
    private RaceLoot() {
    }

    private static LootTable table(ServerLevel level, ResourceLocation id) {
        return Objects.requireNonNull(level.getServer()).reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
    }

    /** One roll, before the soul's results count. */
    public static ItemStack roll(ServerLevel level, ResourceLocation raceID, Race race, ItemStack soul) {
        LootTable table = table(level, ResourceLocation.fromNamespaceAndPath(raceID.getNamespace(), "race/id/" + raceID.getPath()));
        if (table == LootTable.EMPTY) {
            Umapyoi.getLogger().debug("There doesn't exist a loot table for {}, falling back to generic rank + field table", raceID);
            if (race == null) {
                Umapyoi.getLogger().error("No such race! {}", raceID);
                return ItemStack.EMPTY;
            }
            RaceRanking rank = race.ranking;
            if (soul.isEmpty()) {
                Umapyoi.getLogger().error("Umasoul is no longer present.");
                table = table(level, ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race/generic/race_" +
                        rank.name().toLowerCase()));
            } else {
                ResourceLocation field = race.field(level, soul).id();
                table = table(level, ResourceLocation.fromNamespaceAndPath(field.getNamespace(), "race/generic/field/race_"
                        + field.getPath() + "_" + rank.name().toLowerCase()));
                if (table == LootTable.EMPTY) {
                    Umapyoi.getLogger().debug("There doesn't exist a loot table for {} {}, falling back to generic table", field, rank);
                    table = table(level, ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race/generic/race_" +
                            rank.name().toLowerCase()));
                }
            }
        }
        ObjectArrayList<ItemStack> returns = table.getRandomItems(new LootParams.Builder(level).create(LootContextParamSets.EMPTY));
        if (returns.isEmpty()) {
            Umapyoi.getLogger().error("Rolls Empty! {}", raceID);
            return ItemStack.EMPTY;
        }
        return returns.get(0);
    }

    /**
     * What a runner gets.
     *
     * @param share of what the terminal's quick race pays for the same soul; at least one item
     */
    public static List<ItemStack> prizes(ServerLevel level, Race race, ItemStack soul, double share) {
        ItemStack prize = roll(level, race.id(), race, soul);
        List<ItemStack> stacks = new ArrayList<>();
        if (prize.isEmpty() || share <= 0)
            return stacks;
        long count = Math.max(1, Math.round(prize.getCount() * race.getUmaFactorCorrection(soul, level) * share));
        // The terminal has four output slots.
        while (count > 0 && stacks.size() < 4) {
            int size = (int) Math.min(prize.getMaxStackSize(), count);
            stacks.add(prize.copyWithCount(size));
            count -= size;
        }
        return stacks;
    }
}
