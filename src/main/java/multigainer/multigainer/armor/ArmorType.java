package multigainer.multigainer.armor;

import org.bukkit.Color;

public enum ArmorType {
    EXPONENT     ("Exponent",           "§a", Color.fromRGB(0x55, 0xFF, 0x55), true,  0.10, 1.0,   0.50, 5.0,    0.01,  15.0),
    GEMS         ("Gems",               "§b", Color.fromRGB(0x55, 0xFF, 0xFF), false, 1.10, 5.0,   2.50, 25.0,   1.01, 100.0),
    GRIND_POINTS ("Grinding Points",    "§c", Color.fromRGB(0xFF, 0x55, 0x55), false, 1.05, 2.0,   1.50, 5.0,    1.01,  10.0),
    FARM_XP      ("Farm XP",            "§6", Color.fromRGB(0xFF, 0xAA, 0x00), false, 1.50, 25.0, 15.00, 250.0,  1.01,1000.0),
    MINE_XP      ("Mine XP",            "§7", Color.fromRGB(0xAA, 0xAA, 0xAA), false, 1.25, 10.0,  5.00, 35.0,   1.01, 100.0),
    TIER_POINTS  ("Tier Points",        "§e", Color.fromRGB(0xFF, 0xFF, 0x55), false, 1.05, 2.0,   1.35, 5.0,    1.01,  10.0),
    BLOCK_STORAGE("Block Storage",      "§8", Color.fromRGB(0x55, 0x55, 0x55), false, 1.05, 2.5,   1.30, 4.0,    1.01,   8.0);

    public final String displayName;
    public final String color;
    public final Color  leatherColor;
    public final boolean additive;
    public final double lowMin, lowMax, medMin, medMax, highMin, highMax;

    ArmorType(String displayName, String color, Color leatherColor, boolean additive,
              double lowMin, double lowMax, double medMin, double medMax,
              double highMin, double highMax) {
        this.displayName  = displayName;
        this.color        = color;
        this.leatherColor = leatherColor;
        this.additive     = additive;
        this.lowMin  = lowMin;  this.lowMax  = lowMax;
        this.medMin  = medMin;  this.medMax  = medMax;
        this.highMin = highMin; this.highMax = highMax;
    }

    public double rollValue(int riskLevel, java.util.Random rng) {
        double min, max;
        switch (riskLevel) {
            case 0 -> { min = lowMin;  max = lowMax; }
            case 1 -> { min = medMin;  max = medMax; }
            default -> { min = highMin; max = highMax; }
        }
        // 2 decimal places
        double raw = min + rng.nextDouble() * (max - min);
        return Math.round(raw * 100.0) / 100.0;
    }
}
