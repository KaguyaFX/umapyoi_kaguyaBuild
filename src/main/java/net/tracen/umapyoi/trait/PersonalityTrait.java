package net.tracen.umapyoi.trait;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;

/**
 * Personality traits of a three-year career. The random negative one is rolled by the Three
 * Goddesses when a soul is made; the positive one is chosen in the self-training terminal.
 * Effects come in a later step; this is the list, the names and the exclusions.
 */
public enum PersonalityTrait {
    TIMID("timid", false),
    COWARDLY("cowardly", false),
    DULL("dull", false),
    AFRAID_OF_DARK("afraid_of_dark", false),
    GLUTTON("glutton", false),
    PICKY_EATER("picky_eater", false),
    SLEEPY("sleepy", false),
    HATES_RAIN("hates_rain", false),
    LAZY("lazy", false),
    IMPATIENT("impatient", false),
    FRAGILE("fragile", false),
    LIGHT_AVERSE("light_averse", false),

    TENACIOUS("tenacious", true),
    CHEERFUL("cheerful", true),
    STUDIOUS("studious", true),
    STEADY("steady", true),
    EXCITABLE("excitable", true);

    private final String id;
    private final boolean positive;

    PersonalityTrait(String id, boolean positive) {
        this.id = id;
        this.positive = positive;
    }

    public String id() {
        return id;
    }

    public boolean positive() {
        return positive;
    }

    public Component displayName() {
        return Component.translatable("umapyoi.trait.personality." + id);
    }

    public Component description() {
        return Component.translatable("umapyoi.trait.personality." + id + ".desc");
    }

    /** The positive trait that can't be picked together with this negative one, if any. */
    @Nullable
    public PersonalityTrait excludes() {
        return switch (this) {
            case FRAGILE -> CHEERFUL;
            case LAZY -> STEADY;
            default -> null;
        };
    }

    public static Optional<PersonalityTrait> byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id.equals(id)).findFirst();
    }

    public static List<PersonalityTrait> positives() {
        return Arrays.stream(values()).filter(PersonalityTrait::positive).toList();
    }

    public static List<PersonalityTrait> negatives() {
        return Arrays.stream(values()).filter(t -> !t.positive).toList();
    }
}
