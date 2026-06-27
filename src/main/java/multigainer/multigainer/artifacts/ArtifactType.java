package multigainer.multigainer.artifacts;

public enum ArtifactType {
    EXPONENT       ("Exponent",        "§6"),
    GEM            ("Gem",             "§b"),
    FARM_XP        ("Farm XP",         "§e"),
    MINE_XP        ("Mine XP",         "§7"),
    FARM_MULTI     ("Farm Multi",      "§6"),
    GRINDING_POINTS("Grinding Points", "§c"),
    REBIRTH_POINTS ("Rebirth Points",  "§5"),
    TIER_POINTS    ("Tier Points",     "§e"),
    SEED           ("Seed",            "§a"),
    BLOCK_STORAGE  ("Block Storage",   "§8");

    public final String displayName;
    public final String color;

    ArtifactType(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
}
