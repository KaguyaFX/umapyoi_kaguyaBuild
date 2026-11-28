package net.tracen.umapyoi.registry.skills;

/** How rare a skill is: which frame, icon border and fire it gets on the HUD, over heads and on the phone. */
public enum SkillRarity {
    /** A plain skill (level 1, passives too): silver-white. */
    NORMAL,
    /** An upper skill (level 2): gold. */
    RARE,
    /** A skill evolved from another one: cherry pink. */
    EVOLVED,
    /** A unique skill (level 3): flowing rainbow, with the unique icon. */
    UNIQUE;

    /** What a skill's level makes it when nothing else is said. */
    public static SkillRarity ofLevel(int level) {
        return level >= 3 ? UNIQUE : level == 2 ? RARE : NORMAL;
    }
}
