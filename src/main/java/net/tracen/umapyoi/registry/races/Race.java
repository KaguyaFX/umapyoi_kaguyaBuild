package net.tracen.umapyoi.registry.races;

import com.google.common.base.Functions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerState;
import net.tracen.umapyoi.career.UmaCareerData;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.registry.races.field.RaceField;
import net.tracen.umapyoi.registry.races.tags.RaceTag;
import net.tracen.umapyoi.registry.umadata.Motivations;
import net.tracen.umapyoi.registry.umadata.UmaData;
import net.tracen.umapyoi.registry.umadata.UmaDataBasicStatus;
import net.tracen.umapyoi.registry.umadata.UmaDataTraining;
import net.tracen.umapyoi.utils.*;

import java.util.*;
import java.util.stream.Collectors;

import static net.tracen.umapyoi.registry.races.field.RaceFieldRegistry.CONST_ADAPTIVE;

public class Race {
    public static final ResourceKey<Registry<Race>> REGISTRY_KEY = ResourceKey
            .createRegistryKey(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "races"));
    /** The zodiac cups, the Major Matches after URA Finals (for the achievement for winning one). */
    public static final TagKey<Race> ZODIAC_CUPS = TagKey.create(REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "zodiac_cups"));

    public static Codec<Race> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(Race::id),
                    RaceRanking.CODEC.fieldOf("ranking").forGetter(Race::ranking),
                    Codec.INT.fieldOf("length").forGetter(r -> r.length),
                    Codec.INT.fieldOf("time").forGetter(Race::time),
                    Year.CODEC.listOf().fieldOf("year").forGetter((r) -> r.year.stream().toList()),
                    Surface.CODEC.fieldOf("surface").forGetter(r -> r.surface),
                    ResourceLocation.CODEC.listOf().fieldOf("tags").forGetter((r) -> r.tags.stream().toList()),
                    ResourceLocation.CODEC.fieldOf("field").forGetter(r -> r.field),
                    Codec.INT.listOf().optionalFieldOf("attribute_correction", List.of()).forGetter((r) -> r.attrCorr.stream().toList()),
                    Codec.INT.optionalFieldOf("reference_level", 5).forGetter(Race::referenceLevel),
                    Codec.STRING.listOf().optionalFieldOf("allow_status", List.of("trained", "retired")).forGetter((r) -> r.allowStatus.stream().map(s -> s.toLowerCase()).toList()),
                    Codec.BOOL.optionalFieldOf("exclusive", true).forGetter(Race::exclusive),
                    Codec.INT.optionalFieldOf("later_then", 0).forGetter(Race::laterThen),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("after_race", List.of()).forGetter((r) -> r.afterRace.stream().toList()),
                    Codec.STRING.optionalFieldOf("texture_predicate_override").forGetter(r -> Optional.ofNullable(r.texturePredicateOverride)),
                    RaceDifficulty.CODEC.optionalFieldOf("difficulty", RaceDifficulty.NONE).forGetter(Race::difficulty)
            ).apply(instance, Race::new)
    );

    public final ResourceLocation id;
    public ResourceLocation id() { return this.id; }

    public final RaceRanking ranking;
    public RaceRanking ranking() { return this.ranking; }

    public final int length;
    public int length() {
        return this.length;
    }

    public Distance distance(ItemStack stack) {
        return this.distance != Distance.ADAPTIVE ? this.distance : Distance.AdaptiveEvaluation(stack);
    }

    public int length(ItemStack stack) {
        return this.distance != Distance.ADAPTIVE ? this.length : Distance.AdaptiveEvaluation(stack).defaultLength;
    }

    public final int time;
    public int time() {
        return this.time;
    }

    public final Set<Year> year;
    public final Set<ResourceLocation> tags;
    public final Surface surface;
    public Surface surface() { return this.surface; }
    public Surface surface(ItemStack stack) {
        return this.surface != Surface.ADAPTIVE ? this.surface : Surface.AdaptiveCollapse(stack);
    }
    public final Distance distance;
    public final ResourceLocation field;
    public ResourceLocation field() { return this.field; }
    public RaceField field(Level world, ItemStack stack) {
        Registry<RaceField> registry = UmapyoiAPI.getRaceFieldRegistry(world);
        if (this.field.equals(CONST_ADAPTIVE)) {
            Map<RaceField, Integer> makeupMap = registry.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toMap(
                    Functions.identity(),
                    field -> field.compareToAbs(this.distance(stack), this.surface(stack))
            ));
            int minimum = makeupMap.values().stream().filter(i -> i != -1).min(Comparator.naturalOrder()).orElse(-1);
            List<RaceField> fieldCandidate = makeupMap.entrySet().stream().filter(et -> (minimum == -1 || et.getValue() == minimum)).map(Map.Entry::getKey).sorted().toList(); // 确保可复现性
            return fieldCandidate.get(world.getRandom().nextInt(0, fieldCandidate.size()));
        }
        return registry.get(this.field);
    }
    public final Set<Integer> attrCorr;
    private final double[] correction;
    public final int referenceLevel;
    public int referenceLevel() { return this.referenceLevel; }
    public final Set<String> allowStatus;
    public final boolean exclusive;
    public boolean exclusive() { return this.exclusive; }
    public final int laterThen;
    public int laterThen() { return this.laterThen; }
    public final Set<ResourceLocation> afterRace;
    public final String texturePredicateOverride;
    public String texturePredicateOverride() {
        return this.texturePredicateOverride;
    }
    /** How hard its computer runners are in a race meeting, beyond its grade; whether it's a Major Match. */
    public final RaceDifficulty difficulty;
    public RaceDifficulty difficulty() {
        return this.difficulty;
    }
    public Race(ResourceLocation id, RaceRanking ranking, int length, int time, Set<Year> year, Surface surface,
                Set<ResourceLocation> tags, ResourceLocation field, Set<Integer> attrCorr, int referenceLevel, Set<String> allowStatus,
                boolean exclusive, int laterThen, Set<ResourceLocation> afterRace, String texturePredicateOverride, RaceDifficulty difficulty) {
        this.id = id;
        this.difficulty = difficulty;
        this.ranking = ranking;
        this.length = length;
        this.time = time;
        this.year = year;
        this.surface = surface;
        this.distance = Distance.match(length);
        this.tags = tags;
        this.field = field;
        this.attrCorr = attrCorr;
        this.correction = new double[]{
                attrCorr.contains(0) ? 1.05 : 1,
                attrCorr.contains(1) ? 1.05 : 1,
                attrCorr.contains(2) ? 1.05 : 1,
                attrCorr.contains(3) ? 1.05 : 1,
                attrCorr.contains(4) ? 1.05 : 1
        };
        this.referenceLevel = referenceLevel;
        this.allowStatus = allowStatus;
        this.exclusive = exclusive;
        this.laterThen = laterThen;
        this.afterRace = afterRace;
        this.texturePredicateOverride = texturePredicateOverride;
    }

    public Race(ResourceLocation id, RaceRanking ranking, int length, int time, List<Year> year, Surface surface,
                List<ResourceLocation> tags, ResourceLocation field, List<Integer> attrCorr, int referenceLevel, List<String> allowStatus,
                boolean exclusive, int laterThen, List<ResourceLocation> afterRace, Optional<String> texture, RaceDifficulty difficulty) {
        this(id, ranking, length, time,
                new HashSet<>(year),
                surface,
                new HashSet<>(tags),
                field,
                new HashSet<>(attrCorr),
                referenceLevel,
                new HashSet<>(allowStatus),
                exclusive, laterThen, new HashSet<>(afterRace), texture.orElse(null), difficulty
        );
    }

    public boolean isAllowed(ItemStack stack) {
        UmaDataTraining training = stack.get(DataComponentsTypeRegistry.UMADATA_TRAINING);
        if (training == null) return this.allowStatus.contains("retired");
        return this.allowStatus.contains(training.hasTrained() ? "trained" : "untrained");
    }

    public boolean isAvailableToUmaSoul(ItemStack stack) {
        return this.getUnavailableReason(stack) == null;
    }

    /**
     * Explains why the soul cannot enter this race, or returns null if it can.
     * isAvailableToUmaSoul() is defined by this method, so the rule and the
     * message shown in the race terminal always agree.
     */
    @Nullable
    public Component getUnavailableReason(ItemStack stack) {
        if (this.id.equals(RaceRegistry.DEFAULT.location()) || !stack.is(ItemRegistry.UMA_SOUL.get()))
            return Component.translatable("tooltip.umapyoi.race.unavailable.invalid");
        boolean debuted = UmaSoulUtils.hasUmaSoulDebut(stack);
        if (this.ranking == RaceRanking.DEBUT && debuted)
            return Component.translatable("tooltip.umapyoi.race.unavailable.already_debuted");
        if (this.ranking != RaceRanking.DEBUT && !debuted)
            return Component.translatable("tooltip.umapyoi.race.unavailable.need_debut");

        UmaRaceHistory hist = stack.getOrDefault(DataComponentsTypeRegistry.UMA_RACE_HISTORY, UmaRaceHistory.DEFAULT);
        int last = hist.lastAttendTime();
        // Only a soul in its three-year career keeps to the calendar, and the calendar is its career's:
        // a race whose every year has gone by in the career is closed. Any race still ahead may be run
        // whenever one likes, again and again, and running one closes nothing. Any other soul (automatic
        // training, career over, retired) may run every race.
        if (this.exclusive && keepsToCalendar(stack)) {
            int now = careerDate(stack);
            boolean ahead = false;
            for (Year year: this.year) {
                if (year.ordinal() * 24 + this.time >= now) {
                    ahead = true;
                    break;
                }
            }
            if (!ahead)
                return Component.translatable("tooltip.umapyoi.race.unavailable.time_passed", describeRaceTime(now));
        }
        // A race asked for first counts only if won for real (see DataComponentsTypeRegistry.RACES_PASSED).
        if (!this.afterRace.isEmpty()) {
            List<ResourceLocation> passed = stack.getOrDefault(DataComponentsTypeRegistry.RACES_PASSED, List.of());
            boolean canAttend = false;
            for (ResourceLocation key: passed){
                if (this.afterRace.contains(key)) {
                    canAttend = true;
                    break;
                }
            }
            if (!canAttend) {
                MutableComponent names = Component.empty();
                this.afterRace.stream().sorted().forEachOrdered(key -> {
                    if (!names.getSiblings().isEmpty())
                        names.append(" / ");
                    names.append(Component.translatable(Util.makeDescriptionId("race", key) + ".name"));
                });
                return Component.translatable("tooltip.umapyoi.race.unavailable.need_previous_race", names);
            }
        }
        if (!this.isAllowed(stack)) {
            MutableComponent allowed = Component.empty();
            for (String status : List.of("untrained", "trained", "retired")) {
                if (!this.allowStatus.contains(status))
                    continue;
                if (!allowed.getSiblings().isEmpty())
                    allowed.append(" / ");
                allowed.append(Component.translatable("tooltip.umapyoi.race.status." + status));
            }
            return Component.translatable("tooltip.umapyoi.race.unavailable.wrong_status",
                    Component.translatable("tooltip.umapyoi.race.status." + soulStatus(stack)), allowed);
        }
        if (last < laterThen)
            return Component.translatable("tooltip.umapyoi.race.unavailable.too_early", describeRaceTime(laterThen));
        return null;
    }

    /** Whether races this soul's career has gone past are closed to it. */
    public static boolean keepsToCalendar(ItemStack stack) {
        return stack.get(DataComponentsTypeRegistry.CAREER_STATE) == CareerState.IN_CAREER
                && stack.get(DataComponentsTypeRegistry.CAREER_DATA) != null;
    }

    /** Where a soul's three-year career is, on the race calendar (11 = Junior, early June, to 72). */
    private static int careerDate(ItemStack stack) {
        UmaCareerData career = stack.get(DataComponentsTypeRegistry.CAREER_DATA);
        return career == null ? 0 : career.periodIndex();
    }

    private static String soulStatus(ItemStack stack) {
        UmaDataTraining training = stack.get(DataComponentsTypeRegistry.UMADATA_TRAINING);
        if (training == null) return "retired";
        return training.hasTrained() ? "trained" : "untrained";
    }

    /** Turns a race-calendar index (year * 24 + half-month, 1-based) into e.g. "Classic Early April". */
    private static Component describeRaceTime(int time) {
        if (time <= 0)
            return Component.translatable("tooltip.umapyoi.race.time.none");
        int yearIndex = Math.min((time - 1) / 24, Year.values().length - 1);
        int halfMonth = (time - 1) % 24 + 1;
        return Component.translatable("tooltip.umapyoi.race.time." + Year.values()[yearIndex].name().toLowerCase())
                .append(" ")
                .append(Component.translatable("tooltip.umapyoi.race.time." + halfMonth));
    }

    public double getUmaFactorCorrection(ItemStack stack, Level world) {
        UmaDataBasicStatus propertiesAsLevel = UmaSoulUtils.getProperty(stack);
        int fieldSituation = world.getRandom().nextIntBetweenInclusive(0, 3);
        ResourceLocation nameLoc = UmaSoulUtils.getName(stack);
        UmaData umaData = UmapyoiAPI.getUmaDataRegistry(world).getOptional(nameLoc).orElseGet(() -> {
            Umapyoi.getLogger().info("Warning: {} doesn't exist.", nameLoc);
            return UmaData.DEFAULT_UMA;
        });
        Position umaPosition = umaData.position();
        double distanceFactor = this.distance.GetMultiplier(stack);
        double surfaceFactor = this.surface.GetMultiplier(stack);
        double fieldSituationFactor = 1 - (fieldSituation * 0.05d); // last factor
        Motivations motivation = UmaSoulUtils.getMotivation(stack);

        double totalProperties = propertiesAsLevel.speed() * (2 - umaPosition.speedFactor) * this.correction[0] + propertiesAsLevel.stamina()
                * (2 - umaPosition.staminaFactor) * this.correction[1] + propertiesAsLevel.strength() * this.correction[2] +
                propertiesAsLevel.guts() * this.correction[3] + propertiesAsLevel.wisdom() * this.correction[4];
        return totalProperties / this.referenceLevel * motivation.getMultiplier() * surfaceFactor * distanceFactor * fieldSituationFactor;
    }

    public double getSelfProp(ItemStack stack, Level world) {
        ResourceLocation nameLoc = UmaSoulUtils.getName(stack);
        UmaData umaData = UmapyoiAPI.getUmaDataRegistry(world).getOptional(nameLoc).orElseGet(() -> {
            Umapyoi.getLogger().info("Warning: {} doesn't exist.", nameLoc);
            return UmaData.DEFAULT_UMA;
        });
        UmaDataBasicStatus propertiesAsLevel = UmaSoulUtils.getProperty(stack);
        Position umaPosition = umaData.position();
        double totalProperties = propertiesAsLevel.speed() * (2 - umaPosition.speedFactor) * this.correction[0] + propertiesAsLevel.stamina()
                * (2 - umaPosition.staminaFactor) * this.correction[1] + propertiesAsLevel.strength() * this.correction[2] +
                propertiesAsLevel.guts() * this.correction[3] + propertiesAsLevel.wisdom() * this.correction[4];
        Motivations motivation = UmaSoulUtils.getMotivation(stack);
        return totalProperties * motivation.getMultiplier();
    }

    public double offScalar(ItemStack stack, Level world) {
        double selfProp = this.getSelfProp(stack, world);
        return Math.min(selfProp, this.referenceLevel) / Math.max(selfProp, this.referenceLevel);
    }

    public boolean isPassed(ItemStack stack, Level world) {
        return this.getSelfProp(stack, world) >= this.referenceLevel;
    }

    public static void attendRace(ItemStack soul, ResourceLocation race) {
        soul.update(DataComponentsTypeRegistry.UMA_RACE_HISTORY, UmaRaceHistory.DEFAULT, i -> {
            HashMap<ResourceLocation, Integer> attendMap = new HashMap<>(i.attended());
            attendMap.compute(race, (rl, v) -> v == null ? 1 : v + 1);
            return new UmaRaceHistory(i.historyWon(), attendMap, i.lastAttendTime(), i.hasDebut(), i.attendRaceTags());
        });
    }

    public static void winRace(ItemStack soul, ResourceLocation race) {
        soul.update(DataComponentsTypeRegistry.UMA_RACE_HISTORY, UmaRaceHistory.DEFAULT, i -> {
            HashSet<ResourceLocation> wonSet = new HashSet<>(i.historyWon());
            wonSet.add(race);
            return new UmaRaceHistory(wonSet, i.attended(), i.lastAttendTime(), i.hasDebut(), i.attendRaceTags());
        });
    }

    public static void setLastAttend(ItemStack soul, int lastAttend) {
        soul.update(DataComponentsTypeRegistry.UMA_RACE_HISTORY, UmaRaceHistory.DEFAULT, i -> new UmaRaceHistory(i.historyWon(), i.attended(), lastAttend, i.hasDebut(), i.attendRaceTags()));
    }

    /** Notes that the soul won this race for real (a race meeting; a Major Match in one's own). */
    public static void pass(ItemStack soul, ResourceLocation race) {
        List<ResourceLocation> passed = soul.getOrDefault(DataComponentsTypeRegistry.RACES_PASSED, List.of());
        if (passed.contains(race))
            return;
        List<ResourceLocation> more = new ArrayList<>(passed);
        more.add(race);
        soul.set(DataComponentsTypeRegistry.RACES_PASSED, List.copyOf(more));
    }

    /** Why the terminal's quick race can't run this race with this soul: a Major Match is only run for real. */
    @Nullable
    public Component getQuickRaceProblem(ItemStack stack) {
        if (this.difficulty.isMajor())
            return Component.translatable("tooltip.umapyoi.race.unavailable.major_only");
        return getUnavailableReason(stack);
    }

    public static void setDebut(ItemStack soul) {
        soul.update(DataComponentsTypeRegistry.UMA_RACE_HISTORY, UmaRaceHistory.DEFAULT, i -> new UmaRaceHistory(i.historyWon(), i.attended(), i.lastAttendTime(), true, i.attendRaceTags()));
    }

    public void followUp(ItemStack stack, Level level) {
        followUp(stack, level, this.isPassed(stack, level));
    }

    /** Notes on a soul that it ran this race, and perhaps won it. */
    public void followUp(ItemStack stack, Level level, boolean won) {
        UmaRaceHistory hist = stack.getOrDefault(DataComponentsTypeRegistry.UMA_RACE_HISTORY, UmaRaceHistory.DEFAULT);

        if (won) {
            tags.stream().map(UmapyoiAPI.getRaceTagRegistry(level)::get).filter(Objects::nonNull)
                    .forEach((t) -> t.applyToUmaSoul(stack, this));

            winRace(stack, this.id);
        }

        attendRace(stack, this.id);

        // The calendar moves on to this race's date, the earliest of its years still ahead.
        // A race the soul has passed (one that is run again, or run by a soul off the calendar) leaves it where it is.
        int lastAttend = hist.lastAttendTime();
        this.year.stream().filter(y -> (y.ordinal() * 24 + this.time) > lastAttend).min(Comparator.naturalOrder())
                .ifPresent(y -> setLastAttend(stack, y.ordinal() * 24 + this.time));

        if (this.ranking == RaceRanking.DEBUT) {
            setDebut(stack);
        }
    }

    public static class RaceBuilder {
        private RaceRanking ranking;
        private int length;
        private int time;
        private final Set<Year> year;
        private final Set<ResourceLocation> tags;
        private Surface surface;
        private ResourceLocation field;
        private final Set<Integer> attrCorr;
        private Integer referenceLevel;
        private boolean exclusive;
        private Set<String> allowStatus;
        private int later;
        private Set<ResourceLocation> afterRace;
        private String texturePredicateOverride;
        private RaceDifficulty difficulty = RaceDifficulty.NONE;

        public RaceBuilder() {
            this.ranking = RaceRanking.DEBUT;
            this.length = 0;
            this.time = 1;
            this.surface = Surface.TURF;
            this.year = new HashSet<>();
            this.tags = new HashSet<>();
            this.field = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "unknown");
            this.attrCorr = new HashSet<>();
            this.referenceLevel = null;
            this.exclusive = true;
            this.allowStatus = new HashSet<>(List.of("trained", "retired"));
            this.later = 0;
            this.afterRace = new HashSet<>();
            this.texturePredicateOverride = null;
        }

        public RaceBuilder setLength(int len) {
            this.length = len;
            return this;
        }

        public RaceBuilder setRanking(RaceRanking rank) {
            this.ranking = rank;
            return this;
        }

        public RaceBuilder setTime(int time) {
            this.time = time;
            return this;
        }

        public RaceBuilder setTime(int month, boolean isLatter) {
            return this.setTime((month - 1) * 2 + (isLatter ? 2 : 1));
        }

        public RaceBuilder addYear(Year... years) {
            Collections.addAll(this.year, years);
            return this;
        }

        public RaceBuilder addTags(ResourceLocation... locs) {
            Collections.addAll(this.tags, locs);
            return this;
        }

        @SafeVarargs
        public final RaceBuilder addTags(ResourceKey<RaceTag>... locs) {
            Arrays.stream(locs).map(ResourceKey::location).forEach(this::addTags);
            return this;
        }

        public RaceBuilder setSurface(Surface surface) {
            this.surface = surface;
            return this;
        }

        public RaceBuilder setField(String field) {
            return this.setField(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, field));
        }

        public RaceBuilder setField(ResourceKey<RaceField> field) {
            return this.setField(field.location());
        }

        public RaceBuilder setField(ResourceLocation field) {
            this.field = field;
            return this;
        }

        public RaceBuilder addAttr(int... attrs) {
            for (int v: attrs) this.attrCorr.add(v);
            return this;
        }

        public RaceBuilder setReferenceLevel(int referenceLevel) {
            this.referenceLevel = referenceLevel;
            return this;
        }

        public RaceBuilder setAllowStatus(String... growth) {
            this.allowStatus = Arrays.stream(growth).map(String::toLowerCase).collect(Collectors.toSet());
            return this;
        }

        public RaceBuilder addAllowStatus(String... growth) {
            this.allowStatus.addAll(Arrays.stream(growth).map(String::toLowerCase).toList());
            return this;
        }

        public RaceBuilder setExclusive(boolean exclusive) {
            this.exclusive = exclusive;
            return this;
        }

        public RaceBuilder onlyIfLaterThen(int time) {
            this.later = time;
            return this;
        }

        public RaceBuilder onlyIfLaterThen(ResourceLocation... races){
            this.afterRace.addAll(Arrays.stream(races).toList());
            return this;
        }

        public RaceBuilder setTexture(String texturePredicateOverride) {
            this.texturePredicateOverride = texturePredicateOverride;
            return this;
        }

        /** Grades stronger than usual for its grade, in a race meeting: the famous races. */
        public RaceBuilder setStrength(int grades) {
            this.difficulty = new RaceDifficulty(grades, this.difficulty.major());
            return this;
        }

        /** A Major Match: see {@link RaceDifficulty}. */
        public RaceBuilder setMajor(int floor, RaceDifficulty.Range above, RaceDifficulty.Range rise, RaceDifficulty.Moods moods, boolean announce) {
            this.difficulty = RaceDifficulty.major(floor, above, rise, moods, announce);
            return this;
        }

        public Race create(ResourceLocation id) {
            return new Race(id, this.ranking, this.length, this.time, this.year, this.surface, this.tags, this.field,
                    this.attrCorr, Optional.ofNullable(this.referenceLevel).orElseGet(() ->
                        switch (this.ranking) {
                            case DEBUT, PREOP -> 5;
                            case OP -> 15;
                            case GIII -> 25;
                            case GII -> 30;
                            case GI -> 35;
                        }
            ), this.allowStatus, this.exclusive, this.later, this.afterRace, this.texturePredicateOverride, this.difficulty);
        }
    }
}