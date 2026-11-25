package net.tracen.umapyoi.race;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.item.ItemRegistry;

/**
 * Everyone's horse tickets (HT): what they bet with. They are a number on an account, not an item, so they
 * come along into the race world, where nothing else does. Minerals buy them at the race terminal, jewels are
 * what they can be turned back into.
 * <p>
 * What is staked on a race is off the balance but still noted here: if the server stops in the middle of it
 * (or crashes), it goes back.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class RaceAccounts extends SavedData {
    private static final String NAME = "umapyoi_race_accounts";
    private static final double EDGE_LIMIT = 40;
    /** How much the latest race counts for what is known of a runner. */
    private static final double EDGE_LATEST = 0.5;

    static final class Account {
        private long balance;
        private long staked;
        /** What the last race with bets on it brought (below zero: cost). */
        private long last;
        /** What the crowd has seen: how many seconds per kilometre slower than worked out this player runs. */
        private double edge;
        private int races;

        long balance() {
            return balance;
        }
    }

    private final Map<UUID, Account> accounts = new HashMap<>();

    static RaceAccounts get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(RaceAccounts::new, RaceAccounts::load, null), NAME);
    }

    Account of(UUID player) {
        return accounts.computeIfAbsent(player, key -> new Account());
    }

    // ---- money

    /** @return false if there isn't that much on the account */
    boolean stake(MinecraftServer server, UUID player, long amount) {
        Account account = of(player);
        if (amount <= 0 || account.balance < amount)
            return false;
        account.balance -= amount;
        account.staked += amount;
        changed(server, player);
        return true;
    }

    /** A stake is settled. @param back what the player gets for it */
    void settle(MinecraftServer server, UUID player, long stake, long back) {
        Account account = of(player);
        account.staked = Math.max(0, account.staked - stake);
        account.balance += back;
        changed(server, player);
    }

    void result(MinecraftServer server, UUID player, long gain) {
        of(player).last = gain;
        changed(server, player);
    }

    /** Puts tickets on an account, or takes them off (never below zero). @return what is on it then */
    public static long add(MinecraftServer server, UUID player, long amount) {
        RaceAccounts accounts = get(server);
        Account account = accounts.of(player);
        account.balance = Math.max(0, account.balance + amount);
        accounts.changed(server, player);
        return account.balance;
    }

    public static long balance(MinecraftServer server, UUID player) {
        return get(server).of(player).balance;
    }

    // ---- the counter

    @Nullable
    private static List<? extends String> parsed;
    private static Map<Item, Integer> values = Map.of();

    /** Tickets one item of this kind buys; 0 if it isn't taken. */
    public static int value(ItemStack stack) {
        return stack.isEmpty() ? 0 : values().getOrDefault(stack.getItem(), 0);
    }

    public static Map<Item, Integer> values() {
        List<? extends String> config = UmapyoiConfig.RACE_TICKET_VALUES.get();
        if (config != parsed) {
            Map<Item, Integer> map = new java.util.LinkedHashMap<>();
            for (String entry : config) {
                String[] halves = entry.split("=");
                ResourceLocation id = halves.length == 2 ? ResourceLocation.tryParse(halves[0].trim()) : null;
                Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
                try {
                    if (item != Items.AIR)
                        map.put(item, Math.max(0, Integer.parseInt(halves[1].trim())));
                } catch (NumberFormatException e) {
                    Umapyoi.getLogger().warn("race_ticket_values: {} isn't an item=number", entry);
                }
            }
            values = map;
            parsed = config;
        }
        return values;
    }

    /** Buys tickets for everything in the stack. @return how many */
    public static long payIn(ServerPlayer player, ItemStack stack) {
        long amount = (long) value(stack) * stack.getCount();
        if (amount <= 0)
            return 0;
        stack.setCount(0);
        add(player.server, player.getUUID(), amount);
        return amount;
    }

    /** Turns tickets into jewels. @return why not, or null */
    @Nullable
    public static Component takeOut(ServerPlayer player, int jewels) {
        RaceAccounts accounts = get(player.server);
        Account account = accounts.of(player.getUUID());
        long price = (long) jewels * UmapyoiConfig.RACE_TICKET_JEWEL_PRICE.get();
        if (jewels <= 0 || account.balance < price)
            return Component.translatable("umapyoi.ticket.problem.balance");
        account.balance -= price;
        accounts.changed(player.server, player.getUUID());
        player.getInventory().placeItemBackInInventory(new ItemStack(ItemRegistry.JEWEL.get(), jewels));
        return null;
    }

    // ---- what is known of the runners

    /** Seconds per kilometre to add to what is worked out for this player. */
    double edge(UUID player) {
        Account account = accounts.get(player);
        return account == null || account.races == 0 ? 0 : account.edge;
    }

    /** They ran: the crowd has seen it. */
    void ran(UUID player, double secondsPerKilometre) {
        Account account = of(player);
        double seen = Mth.clamp(secondsPerKilometre, -EDGE_LIMIT, EDGE_LIMIT);
        account.edge = account.races == 0 ? seen : Mth.lerp(EDGE_LATEST, account.edge, seen);
        account.races++;
        setDirty();
    }

    // ---- telling and keeping

    private void changed(MinecraftServer server, UUID id) {
        setDirty();
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null)
            send(player);
    }

    private void send(ServerPlayer player) {
        Account account = of(player.getUUID());
        PacketDistributor.sendToPlayer(player, new RaceAccountPayload(account.balance, account.staked, account.last, UmapyoiConfig.RACE_TICKET_JEWEL_PRICE.get()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            get(player.server).send(player);
    }

    /** Whatever is still staked belongs to a race that never got its result. */
    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        RaceAccounts accounts = get(event.getServer());
        for (Account account : accounts.accounts.values())
            if (account.staked > 0) {
                account.balance += account.staked;
                account.staked = 0;
                accounts.setDirty();
            }
    }

    private static RaceAccounts load(CompoundTag tag, HolderLookup.Provider registries) {
        RaceAccounts accounts = new RaceAccounts();
        for (Tag entry : tag.getList("accounts", Tag.TAG_COMPOUND)) {
            CompoundTag data = (CompoundTag) entry;
            if (!data.hasUUID("player"))
                continue;
            Account account = accounts.of(data.getUUID("player"));
            account.balance = data.getLong("balance");
            account.staked = data.getLong("staked");
            account.last = data.getLong("last");
            account.edge = data.getDouble("edge");
            account.races = data.getInt("races");
        }
        return accounts;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        accounts.forEach((player, account) -> {
            CompoundTag data = new CompoundTag();
            data.putUUID("player", player);
            data.putLong("balance", account.balance);
            data.putLong("staked", account.staked);
            data.putLong("last", account.last);
            data.putDouble("edge", account.edge);
            data.putInt("races", account.races);
            list.add(data);
        });
        tag.put("accounts", list);
        return tag;
    }
}
