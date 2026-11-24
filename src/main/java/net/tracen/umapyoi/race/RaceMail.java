package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Prizes and returned tickets on their way to players. Nothing can be carried out of the race world, and
 * the player may not even be online, so they are handed over once the player is in a normal world.
 */
final class RaceMail extends SavedData {
    private static final String NAME = "umapyoi_race_mail";

    private record Parcel(Component note, List<ItemStack> items) {
    }

    private final Map<UUID, List<Parcel>> parcels = new HashMap<>();

    private static RaceMail get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(RaceMail::new, RaceMail::load, null), NAME);
    }

    /** @param note said to the player when they get it */
    static void send(MinecraftServer server, UUID to, List<ItemStack> items, Component note) {
        RaceMail mail = get(server);
        mail.parcels.computeIfAbsent(to, key -> new ArrayList<>()).add(new Parcel(note, List.copyOf(items)));
        mail.setDirty();
        ServerPlayer player = server.getPlayerList().getPlayer(to);
        if (player != null && !RaceWorld.isRaceWorld(player.level()) && !RaceStash.marked(player))
            deliver(player);
    }

    /** Hands over whatever is waiting for a player. */
    static void deliver(ServerPlayer player) {
        RaceMail mail = get(player.server);
        List<Parcel> waiting = mail.parcels.remove(player.getUUID());
        if (waiting == null)
            return;
        mail.setDirty();
        for (Parcel parcel : waiting) {
            player.sendSystemMessage(parcel.note());
            for (ItemStack stack : parcel.items())
                player.getInventory().placeItemBackInInventory(stack.copy());
        }
    }

    private static RaceMail load(CompoundTag tag, HolderLookup.Provider registries) {
        RaceMail mail = new RaceMail();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (Tag entry : tag.getList("parcels", Tag.TAG_COMPOUND)) {
            CompoundTag parcel = (CompoundTag) entry;
            List<ItemStack> items = new ArrayList<>();
            for (Tag item : parcel.getList("items", Tag.TAG_COMPOUND))
                ItemStack.parse(registries, item).ifPresent(items::add);
            Component note = ComponentSerialization.CODEC.parse(ops, parcel.get("note")).result().orElse(Component.empty());
            mail.parcels.computeIfAbsent(parcel.getUUID("to"), key -> new ArrayList<>()).add(new Parcel(note, items));
        }
        return mail;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        parcels.forEach((to, waiting) -> {
            for (Parcel parcel : waiting) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("to", to);
                ComponentSerialization.CODEC.encodeStart(ops, parcel.note()).result().ifPresent(note -> entry.put("note", note));
                ListTag items = new ListTag();
                for (ItemStack stack : parcel.items())
                    items.add(stack.save(registries));
                entry.put("items", items);
                list.add(entry);
            }
        });
        tag.put("parcels", list);
        return tag;
    }
}
