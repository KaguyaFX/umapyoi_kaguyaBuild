package net.tracen.umapyoi.race;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * What players leave at the door of the race world. Whoever arrives there, by whatever means, has
 * everything they carry taken into safe keeping (inventory, armor, every Curios slot but the soul's,
 * status effects, experience, health and food) and gets full health; whoever leaves gets it back as it
 * was, and what they picked up inside is gone. So nothing from survival counts in a race.
 * <p>
 * It is kept in a file of its own. Nothing may be lost or doubled if the game stops at any moment, and
 * the player's own save is what decides: it carries a mark exactly as long as their things are in
 * keeping. The order is always
 * <ol>
 * <li>taking: write the file, empty the player and set the mark, save;</li>
 * <li>giving back: note in the file that it's being given back, fill the player and take the mark off,
 * save, delete the file.</li>
 * </ol>
 * "Save" is the whole world, not just the player's file: in singleplayer the host's data is in
 * level.dat, and chests and the like should be from the same moment as the player. It happens a moment
 * later, once for everyone who came or went in the meantime; until then the save is the one from before,
 * and a file is only deleted after it.
 * When someone logs in with the mark, their save is from while their things were in keeping: they get
 * them back. Without it, they have their things already and a leftover file is put aside. Someone who
 * logs in inside the race world is sent back out first.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class RaceStash {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MARK = "umapyoi_race_stash";
    private static final String SOUL_SLOT = "uma_soul";
    /** Equipment changes reach max health a tick or two later, so health is set once more after this long. */
    private static final int SETTLE_TICKS = 3;

    /** Ticks at least between two saves. */
    private static final int SAVE_EVERY = 60;

    private static final List<Later> LATER = new ArrayList<>();
    private static final List<Path> DELETE_AFTER_SAVE = new ArrayList<>();
    private static boolean saveDue;
    private static int lastSave = -SAVE_EVERY;

    private record Later(ServerPlayer player, int tick, Runnable action) {
    }

    private RaceStash() {
    }

    // ---- when

    /** Close whatever they have open while still in the normal world, so what's in it returns (or drops) there. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getDimension() == RaceWorld.RACECOURSE && event.getEntity() instanceof ServerPlayer player)
            player.closeContainer();
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (event.getTo() == RaceWorld.RACECOURSE) {
            if (!take(player))
                later(player, 1, () -> RaceWorld.leave(player));
        } else if (event.getFrom() == RaceWorld.RACECOURSE && marked(player)) {
            giveBack(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // A tick later, when the player is fully in.
        if (event.getEntity() instanceof ServerPlayer player)
            later(player, 1, () -> loggedIn(player));
    }

    /** Out of the race world first (which gives everything back), then whatever is left to settle. */
    static void loggedIn(ServerPlayer player) {
        if (RaceWorld.isRaceWorld(player.level())) {
            RaceWorldEvents.used();
            RaceWorld.leave(player);
        }
        if (RaceWorld.isRaceWorld(player.level()))
            return;
        if (marked(player)) {
            giveBack(player);
        } else {
            if (!DELETE_AFTER_SAVE.contains(file(player)))
                putAside(file(player));
            RaceMail.deliver(player);
        }
    }

    private static void later(ServerPlayer player, int ticks, Runnable action) {
        LATER.add(new Later(player, player.server.getTickCount() + ticks, action));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        save(event.getServer());
        if (LATER.isEmpty())
            return;
        List<Later> due = new ArrayList<>();
        for (Iterator<Later> it = LATER.iterator(); it.hasNext();) {
            Later next = it.next();
            if (next.tick() > event.getServer().getTickCount())
                continue;
            it.remove();
            due.add(next);
        }
        for (Later next : due)
            if (!next.player().hasDisconnected() && !next.player().isRemoved())
                next.action().run();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LATER.clear();
        saveDue = false; // it saves itself now
        DELETE_AFTER_SAVE.clear(); // what is left over is put aside at the next login
        lastSave = -SAVE_EVERY;
    }

    private static void save(MinecraftServer server) {
        if (!saveDue || server.getTickCount() - lastSave < SAVE_EVERY)
            return;
        saveDue = false;
        lastSave = server.getTickCount();
        server.saveEverything(true, false, false);
        for (Path file : DELETE_AFTER_SAVE)
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                LOGGER.warn("Couldn't delete {}; it is ignored, since the player has everything", file, e);
            }
        DELETE_AFTER_SAVE.clear();
    }

    // ---- taking and giving back

    /** @return false if nothing could be put in keeping; the player still has everything then */
    static boolean take(ServerPlayer player) {
        if (marked(player)) // something from before that was never given back
            giveBack(player);
        if (marked(player))
            return false;
        try {
            write(file(player), snapshot(player));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Couldn't put {}'s things in keeping for the race world; they keep them", player.getScoreboardName(), e);
            player.sendSystemMessage(Component.translatable("umapyoi.race.stash.take_failed"));
            return false;
        }
        DELETE_AFTER_SAVE.remove(file(player));
        strip(player);
        mark(player, true);
        saveDue = true;
        player.setHealth(player.getMaxHealth());
        later(player, SETTLE_TICKS, () -> {
            if (RaceWorld.isRaceWorld(player.level()))
                player.setHealth(player.getMaxHealth());
        });
        player.sendSystemMessage(Component.translatable("umapyoi.race.stash.taken"));
        return true;
    }

    static void giveBack(ServerPlayer player) {
        Path file = file(player);
        CompoundTag stash;
        try {
            if (!Files.exists(file)) {
                LOGGER.error("{} should get things back from the race world, but {} is missing", player.getScoreboardName(), file);
                mark(player, false);
                player.sendSystemMessage(Component.translatable("umapyoi.race.stash.give_failed"));
                return;
            }
            stash = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            stash.putBoolean("returning", true);
            write(file, stash);
        } catch (IOException | RuntimeException e) {
            // Keep the mark and the file: the next login tries again.
            LOGGER.error("Couldn't give {} their things back from {}; they stay in keeping", player.getScoreboardName(), file, e);
            player.sendSystemMessage(Component.translatable("umapyoi.race.stash.give_failed"));
            return;
        }
        List<ItemStack> souls = looseSouls(player);
        strip(player);
        fill(player, stash);
        rescue(player, souls);
        mark(player, false);
        saveDue = true;
        DELETE_AFTER_SAVE.add(file);
        player.sendSystemMessage(Component.translatable("umapyoi.race.stash.given"));
        if (!RaceWorld.isRaceWorld(player.level()))
            RaceMail.deliver(player);
    }

    // ---- the player

    static boolean marked(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(MARK);
    }

    private static void mark(ServerPlayer player, boolean stashed) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (stashed)
            persisted.putBoolean(MARK, true);
        else
            persisted.remove(MARK);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Everything on the player, without touching it. */
    private static CompoundTag snapshot(ServerPlayer player) {
        CompoundTag stash = new CompoundTag();
        stash.putString("player", player.getScoreboardName());
        stash.put("inventory", player.getInventory().save(new ListTag()));
        stash.putInt("selected", player.getInventory().selected);

        // On the cursor and in the crafting grid. Normally empty: anything open was closed on the way here.
        ListTag loose = new ListTag();
        addItem(player, loose, player.containerMenu.getCarried());
        CraftingContainer grid = player.inventoryMenu.getCraftSlots();
        for (int slot = 0; slot < grid.getContainerSize(); slot++)
            addItem(player, loose, grid.getItem(slot));
        stash.put("loose", loose);

        ListTag curios = new ListTag();
        eachCurio(player, (id, index, cosmetic, slots) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("slot", id);
            entry.putInt("index", index);
            entry.putBoolean("cosmetic", cosmetic);
            entry.put("item", slots.getStackInSlot(index).save(player.registryAccess()));
            curios.add(entry);
        });
        stash.put("curios", curios);

        ListTag effects = new ListTag();
        for (MobEffectInstance effect : player.getActiveEffects())
            effects.add(effect.save());
        stash.put("effects", effects);

        stash.putInt("level", player.experienceLevel);
        stash.putFloat("progress", player.experienceProgress);
        stash.putInt("total", player.totalExperience);
        stash.putFloat("health", player.getHealth());
        stash.putFloat("absorption", player.getAbsorptionAmount());
        FoodData food = player.getFoodData();
        stash.putInt("food", food.getFoodLevel());
        stash.putFloat("saturation", food.getSaturationLevel());
        stash.putFloat("exhaustion", food.getExhaustionLevel());
        return stash;
    }

    private static void addItem(ServerPlayer player, ListTag into, ItemStack stack) {
        if (!stack.isEmpty())
            into.add(stack.save(player.registryAccess()));
    }

    private interface CurioSlot {
        void accept(String id, int index, boolean cosmetic, IDynamicStackHandler slots);
    }

    /** Every filled Curios slot except the soul's. */
    private static void eachCurio(ServerPlayer player, CurioSlot action) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getCurios().forEach((id, kind) -> {
            if (id.equals(SOUL_SLOT))
                return;
            for (boolean cosmetic : new boolean[] { false, true }) {
                IDynamicStackHandler slots = cosmetic ? kind.getCosmeticStacks() : kind.getStacks();
                for (int index = 0; index < slots.getSlots(); index++)
                    if (!slots.getStackInSlot(index).isEmpty())
                        action.accept(id, index, cosmetic, slots);
            }
        }));
    }

    /** Souls that aren't where they belong. What is picked up inside doesn't come along, but a soul is too much to lose. */
    private static List<ItemStack> looseSouls(ServerPlayer player) {
        List<ItemStack> souls = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            if (UmaSoulUtils.hasSoulData(inventory.getItem(slot)))
                souls.add(inventory.getItem(slot).copy());
        return souls;
    }

    /** Puts them back on, or else into the inventory. */
    private static void rescue(ServerPlayer player, List<ItemStack> souls) {
        for (ItemStack soul : souls) {
            IDynamicStackHandler slots = CuriosApi.getCuriosInventory(player).map(handler -> handler.getCurios().get(SOUL_SLOT))
                    .map(ICurioStacksHandler::getStacks).orElse(null);
            if (slots != null && slots.getSlots() > 0 && slots.getStackInSlot(0).isEmpty())
                slots.setStackInSlot(0, soul);
            else
                player.getInventory().placeItemBackInInventory(soul);
        }
    }

    /** Takes everything off the player but the soul. */
    private static void strip(ServerPlayer player) {
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.getCraftSlots().clearContent();
        if (player.containerMenu != player.inventoryMenu)
            player.closeContainer();
        player.getInventory().clearContent();
        eachCurio(player, (id, index, cosmetic, slots) -> slots.setStackInSlot(index, ItemStack.EMPTY));
        player.removeAllEffects();
        player.setAbsorptionAmount(0);
        experience(player, 0, 0, 0);
        player.clearFire();
    }

    /** Puts back what is in a snapshot; the player has nothing on them. */
    private static void fill(ServerPlayer player, CompoundTag stash) {
        Inventory inventory = player.getInventory();
        inventory.load(stash.getList("inventory", Tag.TAG_COMPOUND));
        inventory.selected = stash.getInt("selected");

        List<ItemStack> loose = new ArrayList<>();
        for (Tag tag : stash.getList("loose", Tag.TAG_COMPOUND))
            loose.add(ItemStack.parseOptional(player.registryAccess(), (CompoundTag) tag));
        var curios = CuriosApi.getCuriosInventory(player);
        for (Tag tag : stash.getList("curios", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), entry.getCompound("item"));
            ICurioStacksHandler slots = curios.map(handler -> handler.getCurios().get(entry.getString("slot"))).orElse(null);
            IDynamicStackHandler stacks = slots == null ? null : entry.getBoolean("cosmetic") ? slots.getCosmeticStacks() : slots.getStacks();
            int index = entry.getInt("index");
            if (stacks != null && index < stacks.getSlots() && stacks.getStackInSlot(index).isEmpty())
                stacks.setStackInSlot(index, stack);
            else
                loose.add(stack); // the slot isn't there right now
        }
        // Into free slots, or at the player's feet if there are none.
        for (ItemStack stack : loose)
            if (!stack.isEmpty())
                inventory.placeItemBackInInventory(stack);

        for (Tag tag : stash.getList("effects", Tag.TAG_COMPOUND)) {
            MobEffectInstance effect = MobEffectInstance.load((CompoundTag) tag);
            if (effect != null)
                player.addEffect(effect);
        }
        experience(player, stash.getInt("level"), stash.getFloat("progress"), stash.getInt("total"));
        FoodData food = player.getFoodData();
        food.setFoodLevel(stash.getInt("food"));
        food.setSaturation(stash.getFloat("saturation"));
        food.setExhaustion(stash.getFloat("exhaustion"));
        player.setAbsorptionAmount(stash.getFloat("absorption"));
        health(player, stash.getFloat("health"));

        player.connection.send(new ClientboundSetCarriedItemPacket(inventory.selected));
        player.inventoryMenu.broadcastFullState();
    }

    private static void experience(ServerPlayer player, int level, float progress, int total) {
        player.setExperienceLevels(level);
        player.experienceProgress = progress;
        player.totalExperience = total;
    }

    /** Health as it was. What their equipment adds to the maximum only counts a moment later, so the rest follows then. */
    private static void health(ServerPlayer player, float health) {
        float now = Math.max(1, Math.min(health, player.getMaxHealth()));
        player.setHealth(now);
        if (health > now)
            later(player, SETTLE_TICKS, () -> {
                if (player.getHealth() >= now && !player.isDeadOrDying() && !RaceWorld.isRaceWorld(player.level()))
                    player.setHealth(Math.min(health, player.getMaxHealth()));
            });
    }

    // ---- the file

    static Path file(ServerPlayer player) {
        return player.server.getWorldPath(LevelResource.ROOT).resolve("umapyoi").resolve("race_stash")
                .resolve(player.getStringUUID() + ".dat");
    }

    private static void write(Path file, CompoundTag stash) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        NbtIo.writeCompressed(stash, temp);
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** A file left over although its player has everything: out of the way, but not thrown out. */
    private static void putAside(Path file) {
        try {
            if (Files.exists(file)) {
                LOGGER.warn("{} is left over: its player already has everything. Moving it aside", file);
                Files.move(file, file.resolveSibling(file.getFileName() + ".old"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.warn("Couldn't move {} aside", file, e);
        }
    }
}
