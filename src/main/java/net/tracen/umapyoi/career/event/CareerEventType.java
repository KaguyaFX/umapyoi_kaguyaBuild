package net.tracen.umapyoi.career.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.data.tag.UmapyoiItemTags;
import net.tracen.umapyoi.registry.umadata.Motivations;
import net.tracen.umapyoi.trait.PersonalityEffects;
import net.tracen.umapyoi.trait.PersonalityTrait;
import net.tracen.umapyoi.trait.UmaTrait;
import net.tracen.umapyoi.trait.UmaTraits;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * Every event a career can have. A career's event table is built from these when
 * it starts; each turn may draw one. General events go into every table; trait events only into
 * the tables of souls with that trait, and weigh more so a soul's character shows.
 */
public enum CareerEventType {
    // ---------------------------------------------------------------- general
    /** 想跑一跑: sprint for a while without stopping. 坐不住 makes it come up more. */
    WANT_TO_RUN("want_to_run", Kind.OPTIONAL, 3, 10),
    /** 肚子餓了: eat anything. Only appears when not full, so it can always be done. */
    HUNGRY("hungry", Kind.MANDATORY, 3, 10),
    /** 想吃胡蘿蔔: eat a carrot or something made from one. */
    WANT_CARROT("want_carrot", Kind.OPTIONAL, 2, 10),
    /** 想喝蜂蜜特飲: needs the item, so it can only be optional. */
    WANT_HACHIMI("want_hachimi", Kind.OPTIONAL, 2, 10),
    /** 想換套衣服: wear a different outfit in the outfit slot than when it came up. */
    CHANGE_OUTFIT("change_outfit", Kind.OPTIONAL, 2, 10),
    /** 想去別的地方看看: be in another biome than where it came up. 好奇心 makes it come up more. */
    OTHER_BIOME("other_biome", Kind.OPTIONAL, 2, 10),
    /** 想看日出: only comes up a little before dawn; gone if dawn passes. */
    WATCH_SUNRISE("watch_sunrise", Kind.OPTIONAL, 1, 5),
    /** 想看夕陽: only comes up a little before dusk; gone if dusk passes. */
    WATCH_SUNSET("watch_sunset", Kind.OPTIONAL, 1, 5),

    // ---------------------------------------------------------------- from traits
    /** 戀同族: stay near a horse, donkey or mule. */
    WANT_SEE_HORSE("want_see_horse", Kind.OPTIONAL, 2, 20),
    /** 膽小: get back close to the respawn point. Only comes up away from it. */
    WANT_GO_HOME("want_go_home", Kind.OPTIONAL, 2, 20),
    /** 懦弱: stay with a companion. Only comes up when alone. */
    WANT_COMPANY("want_company", Kind.OPTIONAL, 2, 20),
    /** 怕黑: stay somewhere bright. Only comes up somewhere dark. */
    WANT_LIGHT("want_light", Kind.OPTIONAL, 2, 20),
    /** 貪吃: eat one food named when it came up. */
    WANT_FOOD("want_food", Kind.OPTIONAL, 2, 20),
    /** 貪吃, now and then: a golden carrot, and it has to be. The "be prepared" kind. */
    WANT_GOLDEN_CARROT("want_golden_carrot", Kind.MANDATORY, 1, 5),
    /** 挑食: eat a soup or stew (tag c:foods/soup, so other mods' dishes count). */
    WANT_SOUP("want_soup", Kind.OPTIONAL, 2, 20),
    /** 嗜睡: sleep in a bed. Only comes up when night is near; gone if it can't be done in time. */
    WANT_SLEEP("want_sleep", Kind.MANDATORY, 2, 20),
    /** 怕雨: stay under a roof while it rains. Gone when the rain stops. */
    WANT_INDOORS("want_indoors", Kind.OPTIONAL, 2, 20),
    /**
     * 懶散期: can't be done, only waited out; career points are halved meanwhile. A liked food or a
     * mood food ends it early. Comes up for 懶散 souls, or for anyone whose motivation is at its worst.
     */
    LAZY_PERIOD("lazy_period", Kind.STATE, 3, 20),
    /** 興奮: defeat a hostile mob. */
    WANT_FIGHT("want_fight", Kind.OPTIONAL, 2, 20),
    /** 好學: spend experience levels (enchanting, anvil) or learn a new skill. */
    WANT_LEARN("want_learn", Kind.OPTIONAL, 2, 20),

    // ---------------------------------------------------------------- career, card and skill notices
    // Notices take effect the moment they come up and stay in the list for one turn.
    /** Every career: Classic year, first half of January. Hyper Mood for a while. */
    NEW_YEAR_FIRST("new_year_first", Kind.NOTICE, 0, 0),
    /** Every career: Senior year, first half of January. The last one. */
    NEW_YEAR_LAST("new_year_last", Kind.NOTICE, 0, 0),
    /** Acupuncturist card, once per card: may raise every stat cap by 1, or lower motivation. */
    ACUPUNCTURE("acupuncture", Kind.NOTICE, 1, 10),
    /** Random-stat card, once per card: one random stat +1. */
    RANDOM_STAT("random_stat", Kind.NOTICE, 1, 10),
    /** A support card's skill was learned (rolled every turn, not drawn). */
    SKILL_LEARNED("skill_learned", Kind.NOTICE, 0, 0);

    public enum Kind {
        OPTIONAL("optional"), MANDATORY("mandatory"), STATE("state"), NOTICE("notice");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public Component label() {
            return Component.translatable("umapyoi.career.event.kind." + id);
        }
    }

    /**
     * Day time windows (0-23999). An event tied to one may come up {@code LEAD} ticks before it
     * starts; the wait plus the window fits in the event's time.
     */
    public enum SkyWindow {
        SUNRISE(22500, 24000), SUNSET(12000, 13500), NIGHT(12542, 23460);

        static final int LEAD = 3000;
        final int start;
        final int end;

        SkyWindow(int start, int end) {
            this.start = start;
            this.end = end;
        }

        static boolean hasDayCycle(Level level) {
            return !level.dimensionType().hasFixedTime();
        }

        static long timeOfDay(Level level) {
            return level.getDayTime() % 24000L;
        }

        boolean comingSoon(Level level) {
            long time = timeOfDay(level);
            return hasDayCycle(level) && time >= start - LEAD && time < start;
        }

        /** Absolute day time when the window that is coming up ends. */
        long endAfter(Level level) {
            return level.getDayTime() - timeOfDay(level) + end;
        }

        boolean isNow(Level level) {
            long time = timeOfDay(level);
            return hasDayCycle(level) && time >= start && time < end;
        }
    }

    private final String id;
    private final Kind kind;
    private final int maxCount;
    private final int weight;

    CareerEventType(String id, Kind kind, int maxCount, int weight) {
        this.id = id;
        this.kind = kind;
        this.maxCount = maxCount;
        this.weight = weight;
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    /** How many times it may come up in one career. */
    public int maxCount() {
        return maxCount;
    }

    /** Draw weight against the other events in the table, for this soul. */
    public int weight(ItemStack soul) {
        return switch (this) {
            case WANT_TO_RUN -> hasUmaTrait(soul, UmaTrait.RESTLESS) ? weight * 2 : weight;
            case OTHER_BIOME -> hasUmaTrait(soul, UmaTrait.CURIOUS) ? weight * 2 : weight;
            case LAZY_PERIOD -> PersonalityEffects.has(soul, PersonalityTrait.LAZY) ? weight : weight / 2;
            default -> weight;
        };
    }

    /** Turns the player has to do it (急躁 has fewer for mandatory events). */
    public int turns(ItemStack soul) {
        if (kind == Kind.NOTICE)
            return 1;
        if (kind != Kind.MANDATORY)
            return UmapyoiConfig.EVENT_OPTIONAL_TURNS.get();
        int turns = UmapyoiConfig.EVENT_MANDATORY_TURNS.get();
        if (PersonalityEffects.has(soul, PersonalityTrait.IMPATIENT))
            turns -= UmapyoiConfig.IMPATIENT_FEWER_TURNS.get();
        return Math.max(1, turns);
    }

    /** Whether it goes into this soul's event table at the start of the career. */
    public boolean belongsIn(ItemStack soul) {
        return switch (this) {
            case WANT_SEE_HORSE -> hasUmaTrait(soul, UmaTrait.HORSE_LOVER);
            case WANT_GO_HOME -> PersonalityEffects.has(soul, PersonalityTrait.TIMID);
            case WANT_COMPANY -> PersonalityEffects.has(soul, PersonalityTrait.COWARDLY);
            case WANT_LIGHT -> PersonalityEffects.has(soul, PersonalityTrait.AFRAID_OF_DARK);
            case WANT_FOOD, WANT_GOLDEN_CARROT -> PersonalityEffects.has(soul, PersonalityTrait.GLUTTON);
            case WANT_SOUP -> PersonalityEffects.has(soul, PersonalityTrait.PICKY_EATER);
            case WANT_SLEEP -> PersonalityEffects.has(soul, PersonalityTrait.SLEEPY);
            case WANT_INDOORS -> PersonalityEffects.has(soul, PersonalityTrait.HATES_RAIN);
            case WANT_FIGHT -> PersonalityEffects.has(soul, PersonalityTrait.EXCITABLE);
            case WANT_LEARN -> PersonalityEffects.has(soul, PersonalityTrait.STUDIOUS);
            // scheduled, from cards (added by the card list at the start), or never drawn
            case NEW_YEAR_FIRST, NEW_YEAR_LAST, ACUPUNCTURE, RANDOM_STAT, SKILL_LEARNED -> false;
            default -> true; // general events, and the lazy period (bad motivation can bring it to anyone)
        };
    }

    /** Checked when drawn: only events that can be done right now come up. */
    public boolean canAppear(ServerPlayer player, ItemStack soul) {
        Level level = player.level();
        return switch (this) {
            case HUNGRY, WANT_CARROT, WANT_FOOD, WANT_GOLDEN_CARROT, WANT_SOUP -> notFull(player);
            case WATCH_SUNRISE -> SkyWindow.SUNRISE.comingSoon(level);
            case WATCH_SUNSET -> SkyWindow.SUNSET.comingSoon(level);
            case WANT_GO_HOME -> !isHome(player);
            case WANT_COMPANY -> !PersonalityEffects.hasCompanion(player);
            case WANT_LIGHT -> lightAtEyes(player) < UmapyoiConfig.EVENT_BRIGHT_LIGHT.get();
            case WANT_SLEEP -> level.dimensionType().bedWorks() && SkyWindow.hasDayCycle(level)
                    && SkyWindow.timeOfDay(level) >= SkyWindow.NIGHT.start - SkyWindow.LEAD
                    && SkyWindow.timeOfDay(level) < SkyWindow.NIGHT.end - 600;
            case WANT_INDOORS -> rainsHere(player);
            case LAZY_PERIOD -> PersonalityEffects.has(soul, PersonalityTrait.LAZY)
                    || UmaSoulUtils.getMotivation(soul) == Motivations.BAD;
            default -> true;
        };
    }

    /** What the event notes when it comes up, to compare with later. */
    public String noteWhenAppearing(ServerPlayer player, ItemStack soul) {
        return switch (this) {
            case CHANGE_OUTFIT -> outfitOf(player);
            case OTHER_BIOME -> biomeOf(player);
            case WATCH_SUNRISE -> String.valueOf(SkyWindow.SUNRISE.endAfter(player.level()));
            case WATCH_SUNSET -> String.valueOf(SkyWindow.SUNSET.endAfter(player.level()));
            case WANT_FOOD -> pickFood(player, soul);
            case WANT_LEARN -> UmaSoulUtils.getSkills(soul).stream().map(ResourceLocation::toString)
                    .collect(Collectors.joining(";"));
            default -> "";
        };
    }

    // ---------------------------------------------------------------- conditions

    static boolean hasUmaTrait(ItemStack soul, UmaTrait trait) {
        return UmaTraits.of(soul).umaEntries().stream().anyMatch(entry -> entry.trait() == trait);
    }

    static boolean notFull(ServerPlayer player) {
        return player.getFoodData().getFoodLevel() <= UmapyoiConfig.EVENT_HUNGRY_MAX_FOOD.get();
    }

    static int lightAtEyes(ServerPlayer player) {
        return player.level().getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition()));
    }

    /** Within reach of the respawn point (the world spawn when none is set), in its dimension. */
    static boolean isHome(ServerPlayer player) {
        BlockPos home = player.getRespawnPosition();
        var dimension = player.getRespawnDimension();
        if (home == null) {
            ServerLevel overworld = player.server.overworld();
            home = overworld.getSharedSpawnPos();
            dimension = overworld.dimension();
        }
        int distance = UmapyoiConfig.EVENT_HOME_DISTANCE.get();
        return player.level().dimension() == dimension
                && player.blockPosition().distSqr(home) <= (double) distance * distance;
    }

    /** It is raining, and where the player stands rain falls (not snow, not a desert). */
    static boolean rainsHere(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        return level.isRaining() && level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.RAIN;
    }

    /** The outfit slot's content as a comparable key; "" when empty. */
    static String outfitOf(ServerPlayer player) {
        ItemStack suit = UmapyoiAPI.getUmaSuit(player);
        return suit.isEmpty() ? "" : String.valueOf(ItemStack.hashItemAndComponents(suit));
    }

    static String biomeOf(ServerPlayer player) {
        return player.level().getBiome(BlockPos.containing(player.position())).unwrapKey()
                .map(key -> key.location().toString()).orElse("");
    }

    /** A food from the favourite-food candidates the soul doesn't dislike; bread if there's none. */
    private static String pickFood(ServerPlayer player, ItemStack soul) {
        List<ResourceLocation> dislikes = UmaTraits.of(soul).dislikes();
        List<ResourceLocation> foods = new ArrayList<>();
        BuiltInRegistries.ITEM.getTag(UmapyoiItemTags.FAVORITE_FOOD_CANDIDATES).ifPresent(set -> {
            for (Holder<Item> holder : set) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(holder.value());
                if (!dislikes.contains(id) && !foods.contains(id))
                    foods.add(id);
            }
        });
        if (foods.isEmpty())
            return BuiltInRegistries.ITEM.getKey(Items.BREAD).toString();
        return foods.get(player.getRandom().nextInt(foods.size())).toString();
    }

    /** The food a 貪吃 event asks for. */
    static Item foodOf(String data) {
        ResourceLocation id = ResourceLocation.tryParse(data);
        return id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.BREAD;
    }

    // ---------------------------------------------------------------- text

    /** The title; some events put what they noted into it (the food a glutton wants, the skill learned). */
    public Component displayName(String data) {
        return switch (this) {
            case WANT_FOOD -> Component.translatable("umapyoi.career.event." + id, foodOf(data).getDescription());
            case SKILL_LEARNED -> Component.translatable("umapyoi.career.event." + id, skillName(data));
            default -> Component.translatable("umapyoi.career.event." + id);
        };
    }

    public Component hint(String data) {
        return switch (this) {
            case WANT_FOOD -> Component.translatable("umapyoi.career.event." + id + ".hint", foodOf(data).getDescription());
            // "success" / "fail"
            case ACUPUNCTURE -> Component.translatable("umapyoi.career.event." + id + ".hint." + data);
            // the stat's index
            case RANDOM_STAT -> Component.translatable("umapyoi.career.event." + id + ".hint", statName(data));
            default -> Component.translatable("umapyoi.career.event." + id + ".hint");
        };
    }

    private static final String[] STAT_KEYS = { "speed", "stamina", "strength", "guts", "wisdom" };

    static Component statName(String data) {
        int index;
        try {
            index = Integer.parseInt(data);
        } catch (NumberFormatException e) {
            index = 0;
        }
        return Component.translatable("gui.umapyoi.self_training." + STAT_KEYS[Math.floorMod(index, 5)]);
    }

    static Component skillName(String data) {
        ResourceLocation id = ResourceLocation.tryParse(data);
        if (id != null && net.tracen.umapyoi.registry.UmaSkillRegistry.REGISTRY.containsKey(id))
            return net.tracen.umapyoi.registry.UmaSkillRegistry.REGISTRY.get(id).getDescription();
        return Component.literal(data);
    }

    public static Optional<CareerEventType> byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id.equals(id)).findFirst();
    }
}
