package multigainer.multigainer.armor;

import multigainer.multigainer.data.PlayerProfile;

import java.util.ArrayList;
import java.util.List;

public class ArmorManager {

    public static final String[] PIECE_NAMES = {"Helmet", "Chestplate", "Leggings", "Boots"};
    public static final int[]    PIECE_GUI_SLOTS = {10, 12, 14, 16};

    /** Energy cost to unlock each piece (helmet = tier 3, no energy cost). */
    public static final double[] UNLOCK_COSTS = {0.0, 100.0, 10_000.0, 250_000.0};

    /**
     * Returns the multiplier value for the given armor type across all 4 pieces.
     * Since a type can only exist on one piece, returns 0 (additive) or 1 (multiplicative) if not equipped.
     */
    public static double getMultiplier(PlayerProfile profile, ArmorType type) {
        for (int i = 0; i < 4; i++) {
            if (isPieceUnlocked(profile, i) && profile.getArmorType(i) == type.ordinal()) {
                return profile.getArmorValue(i);
            }
        }
        return type.additive ? 0.0 : 1.0;
    }

    /** Whether piece i is considered unlocked for the player. */
    public static boolean isPieceUnlocked(PlayerProfile profile, int i) {
        if (i == 0) return profile.getTier() >= 3;
        return profile.isArmorPieceUnlocked(i);
    }

    /** Returns all ArmorType values NOT currently assigned to a piece OTHER than pieceIndex. */
    public static ArmorType[] getAvailableTypes(PlayerProfile profile, int pieceIndex) {
        List<ArmorType> available = new ArrayList<>();
        for (ArmorType t : ArmorType.values()) {
            boolean usedByOther = false;
            for (int i = 0; i < 4; i++) {
                if (i == pieceIndex) continue;
                if (isPieceUnlocked(profile, i) && profile.getArmorType(i) == t.ordinal()) {
                    usedByOther = true;
                    break;
                }
            }
            if (!usedByOther) available.add(t);
        }
        return available.toArray(new ArmorType[0]);
    }
}
