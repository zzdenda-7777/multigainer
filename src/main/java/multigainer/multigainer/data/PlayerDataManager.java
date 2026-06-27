package multigainer.multigainer.data;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.tools.PickaxeManager;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final Multigainer plugin;
    private final StorageManager storageManager;
    private final Map<UUID, PlayerProfile> profileCache = new ConcurrentHashMap<>();

    public PlayerDataManager(Multigainer plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    public PlayerProfile getProfile(UUID uuid) { return profileCache.get(uuid); }

    public CompletableFuture<PlayerProfile> loadProfileAsync(UUID uuid, String username) {
        CompletableFuture<PlayerProfile> future = new CompletableFuture<>();

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try (Connection conn = storageManager.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM mg_player_profiles WHERE uuid = ?")) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            PlayerProfile profile = new PlayerProfile(
                                    new BigNumber(rs.getDouble("money_mantissa"), rs.getDouble("money_exponent")),
                                    new BigNumber(rs.getDouble("gems_mantissa"),  rs.getDouble("gems_exponent")),
                                    new BigNumber(rs.getDouble("rubies_mantissa"),rs.getDouble("rubies_exponent")),
                                    rs.getInt("upgrade_level"), rs.getInt("tier"), rs.getInt("tier_points"),
                                    rs.getInt("farming_level"), rs.getDouble("farming_xp"),
                                    rs.getInt("mining_level"),  rs.getDouble("mining_xp"),
                                    rs.getDouble("rebirth_points"), rs.getInt("rebirth_count")
                            );

                            // Pickaxe
                            profile.setPickaxeTier(safeGetInt(rs, "pickaxe_tier", 0));
                            profile.setMiningSpeedLevel(safeGetInt(rs, "mining_speed_level", 0));
                            profile.setXpMultiLevel(safeGetInt(rs, "xp_multi_level", 0));
                            profile.setGemMultiLevel(safeGetInt(rs, "gem_multi_level", 0));
                            long[] blockStorage = new long[17];
                            for (int i = 0; i < PickaxeManager.BLOCK_COLUMN_NAMES.length; i++) {
                                blockStorage[i] = safeGetLong(rs, PickaxeManager.BLOCK_COLUMN_NAMES[i], 0L);
                            }
                            profile.setBlockStorageArray(blockStorage);

                            // Farming (7 seed currency tiers)
                            long[] seedStorage = new long[7];
                            for (int i = 0; i < 7; i++) {
                                seedStorage[i] = safeGetLong(rs, "seed_storage_" + i, 0L);
                            }
                            profile.setSeedStorageArray(seedStorage);
                            profile.setFarmMulti(safeGetDouble(rs, "farm_multi", 1.0));
                            profile.setChosenCrop(safeGetInt(rs, "chosen_crop", 0));
                            profile.setHoeTier(safeGetInt(rs, "hoe_tier", 0));
                            profile.setAutoMerge(safeGetInt(rs, "auto_merge", 0) == 1);
                            boolean[] enchantMsgs = {
                                    safeGetInt(rs, "enchant_msg_tnt", 1) == 1,
                                    safeGetInt(rs, "enchant_msg_nuke", 1) == 1,
                                    safeGetInt(rs, "enchant_msg_world_eater", 1) == 1,
                                    safeGetInt(rs, "enchant_msg_universe_destroyer", 1) == 1
                            };
                            profile.setEnchantMessagesEnabled(enchantMsgs);
                            profile.setGemUpgradeLevel(safeGetInt(rs, "gem_upgrade_level", 0));
                            profile.setFarmMultiUpgradeLevel(safeGetInt(rs, "farm_multi_upgrade_level", 0));

                            // Grinding Points
                            profile.setGrindingPoints(safeGetDouble(rs, "grinding_points", 0.0));
                            profile.setGrindMessagesEnabled(safeGetInt(rs, "grind_messages", 1) == 1);
                            profile.setGrindChanceLevel(safeGetInt(rs, "grind_chance_level", 0));
                            profile.setGrindExponentLevel(safeGetInt(rs, "grind_exponent_level", 0));
                            profile.setGrindFarmMultiLevel(safeGetInt(rs, "grind_farm_multi_level", 0));
                            profile.setGrindGemMultiLevel(safeGetInt(rs, "grind_gem_multi_level", 0));
                            profile.setGrindFarmXpLevel(safeGetInt(rs, "grind_farm_xp_level", 0));
                            profile.setGrindMineXpLevel(safeGetInt(rs, "grind_mine_xp_level", 0));
                            profile.setGrindSeedMultiLevel(safeGetInt(rs, "grind_seed_multi_level", 0));
                            profile.setGrindGPMultiLevel(safeGetInt(rs, "grind_gp_multi_level", 0));

                            // Farming level-up message
                            profile.setLevelUpFarmMessageEnabled(safeGetInt(rs, "level_up_farm_msg", 1) == 1);

                            // Perks
                            int[] perkCounts = new int[5];
                            int[] perkChanceLevels = new int[5];
                            boolean[] perkMsgs = new boolean[5];
                            for (int i = 0; i < 5; i++) {
                                perkCounts[i]       = safeGetInt(rs, "perk_count_" + i, 0);
                                perkChanceLevels[i] = safeGetInt(rs, "perk_chance_level_" + i, 0);
                                perkMsgs[i]         = safeGetInt(rs, "perk_msg_" + i, 1) == 1;
                            }
                            profile.setPerkCounts(perkCounts);
                            profile.setPerkChanceLevels(perkChanceLevels);
                            profile.setPerkMessagesEnabled(perkMsgs);

                            // Item slot positions
                            profile.setHoeSlot(safeGetInt(rs, "hoe_slot", 0));
                            profile.setPickaxeSlot(safeGetInt(rs, "pickaxe_slot", 1));
                            profile.setUpgradeSlot(safeGetInt(rs, "upgrade_slot", 4));

                            // Artifact slots
                            profile.setArtifactSlot(0, safeGetString(rs, "artifact_slot_0", ""));
                            profile.setArtifactSlot(1, safeGetString(rs, "artifact_slot_1", ""));
                            profile.setArtifactSlot(2, safeGetString(rs, "artifact_slot_2", ""));
                            profile.setArtifactSlotUnlocked(1, safeGetInt(rs, "artifact_slot_1_unlocked", 0) == 1);
                            profile.setArtifactSlotUnlocked(2, safeGetInt(rs, "artifact_slot_2_unlocked", 0) == 1);

                            // Worker / Production
                            profile.setWorkerLevel(safeGetInt(rs, "worker_level", 0));
                            profile.setWorkerXp(safeGetDouble(rs, "worker_xp", 0.0));
                            profile.setWorkerEnergy(safeGetDouble(rs, "worker_energy", 0.0));

                            // Armor system
                            profile.setArmorPieceUnlocked(1, safeGetInt(rs, "armor_chest_unlocked", 0) == 1);
                            profile.setArmorPieceUnlocked(2, safeGetInt(rs, "armor_legs_unlocked",  0) == 1);
                            profile.setArmorPieceUnlocked(3, safeGetInt(rs, "armor_boots_unlocked", 0) == 1);
                            for (int i = 0; i < 4; i++) {
                                profile.setArmorType(i,  safeGetInt(rs,    "armor_type_"  + i, -1));
                                profile.setArmorValue(i, safeGetDouble(rs, "armor_value_" + i,  0.0));
                            }
                            profile.setArmorLowBuys( safeGetInt(rs, "armor_low_buys",  0));
                            profile.setArmorMedBuys( safeGetInt(rs, "armor_med_buys",  0));
                            profile.setArmorHighBuys(safeGetInt(rs, "armor_high_buys", 0));
                            profile.setSkipAnimationUnlocked(safeGetInt(rs, "armor_skip_anim_unlocked", 0) == 1);
                            profile.setSkipAnimationEnabled(safeGetInt(rs, "armor_skip_anim_enabled",  0) == 1);
                            profile.setCropsFarmed(safeGetLong(rs, "crops_farmed", 0L));

                            // Artifact vault
                            String vaultStr = safeGetString(rs, "artifact_vault", "");
                            if (!vaultStr.isEmpty()) {
                                String[] parts = vaultStr.split("\\|", -1);
                                String[] vault = new String[45];
                                for (int i = 0; i < 45 && i < parts.length; i++) vault[i] = parts[i];
                                profile.setArtifactVault(vault);
                            }

                            profileCache.put(uuid, profile);
                            future.complete(profile);
                            return;
                        }
                    }
                }

                // New player
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO mg_player_profiles (uuid, username) VALUES (?, ?)")) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, username);
                    stmt.executeUpdate();
                }
                PlayerProfile newProfile = new PlayerProfile();
                profileCache.put(uuid, newProfile);
                future.complete(newProfile);

            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public void saveProfileSynchronously(UUID uuid, PlayerProfile profile) {
        StringBuilder q = new StringBuilder(
                "UPDATE mg_player_profiles SET "
                        + "money_mantissa=?,money_exponent=?,gems_mantissa=?,gems_exponent=?,"
                        + "rubies_mantissa=?,rubies_exponent=?,upgrade_level=?,tier=?,tier_points=?,"
                        + "farming_level=?,farming_xp=?,mining_level=?,mining_xp=?,"
                        + "rebirth_points=?,rebirth_count=?,"
                        + "pickaxe_tier=?,mining_speed_level=?,xp_multi_level=?,gem_multi_level=?"
        );
        for (String col : PickaxeManager.BLOCK_COLUMN_NAMES) q.append(",").append(col).append("=?");
        for (int i = 0; i < 7; i++) q.append(",seed_storage_").append(i).append("=?");
        q.append(",farm_multi=?,chosen_crop=?,hoe_tier=?,auto_merge=?");
        q.append(",enchant_msg_tnt=?,enchant_msg_nuke=?,enchant_msg_world_eater=?,enchant_msg_universe_destroyer=?");
        q.append(",gem_upgrade_level=?,farm_multi_upgrade_level=?");
        q.append(",grinding_points=?,grind_messages=?");
        q.append(",grind_chance_level=?,grind_exponent_level=?");
        q.append(",grind_farm_multi_level=?,grind_gem_multi_level=?");
        q.append(",grind_farm_xp_level=?,grind_mine_xp_level=?");
        q.append(",grind_seed_multi_level=?,grind_gp_multi_level=?");
        q.append(",level_up_farm_msg=?");
        for (int i = 0; i < 5; i++) q.append(",perk_count_").append(i).append("=?");
        for (int i = 0; i < 5; i++) q.append(",perk_chance_level_").append(i).append("=?");
        for (int i = 0; i < 5; i++) q.append(",perk_msg_").append(i).append("=?");
        q.append(",hoe_slot=?,pickaxe_slot=?,upgrade_slot=?");
        q.append(",artifact_slot_0=?,artifact_slot_1=?,artifact_slot_2=?");
        q.append(",artifact_slot_1_unlocked=?,artifact_slot_2_unlocked=?");
        q.append(",artifact_vault=?");
        q.append(",worker_level=?,worker_xp=?,worker_energy=?");
        q.append(",armor_chest_unlocked=?,armor_legs_unlocked=?,armor_boots_unlocked=?");
        q.append(",armor_type_0=?,armor_type_1=?,armor_type_2=?,armor_type_3=?");
        q.append(",armor_value_0=?,armor_value_1=?,armor_value_2=?,armor_value_3=?");
        q.append(",armor_low_buys=?,armor_med_buys=?,armor_high_buys=?");
        q.append(",armor_skip_anim_unlocked=?,armor_skip_anim_enabled=?,crops_farmed=?");
        q.append(" WHERE uuid=?");

        try (Connection conn = storageManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(q.toString())) {

            stmt.setDouble(1,  profile.getMoney().getMantissa());
            stmt.setDouble(2,  profile.getMoney().getExponent());
            stmt.setDouble(3,  profile.getGems().getMantissa());
            stmt.setDouble(4,  profile.getGems().getExponent());
            stmt.setDouble(5,  profile.getRubies().getMantissa());
            stmt.setDouble(6,  profile.getRubies().getExponent());
            stmt.setInt(7,     profile.getUpgradeLevel());
            stmt.setInt(8,     profile.getTier());
            stmt.setInt(9,     profile.getTierPoints());
            stmt.setInt(10,    profile.getFarmingLevel());
            stmt.setDouble(11, profile.getFarmingXp());
            stmt.setInt(12,    profile.getMiningLevel());
            stmt.setDouble(13, profile.getMiningXp());
            stmt.setDouble(14, profile.getRebirthPoints());
            stmt.setInt(15,    profile.getRebirthCount());
            stmt.setInt(16,    profile.getPickaxeTier());
            stmt.setInt(17,    profile.getMiningSpeedLevel());
            stmt.setInt(18,    profile.getXpMultiLevel());
            stmt.setInt(19,    profile.getGemMultiLevel());

            int base = 20;
            for (int i = 0; i < PickaxeManager.BLOCK_COLUMN_NAMES.length; i++) {
                stmt.setLong(base++, profile.getBlockStorage(i));
            }
            for (int i = 0; i < 7; i++) stmt.setLong(base++, profile.getSeedStorage(i));
            stmt.setDouble(base++, profile.getFarmMulti());
            stmt.setInt(base++,    profile.getChosenCrop());
            stmt.setInt(base++,    profile.getHoeTier());
            stmt.setInt(base++,    profile.isAutoMerge() ? 1 : 0);
            stmt.setInt(base++,    profile.isEnchantMessageEnabled(0) ? 1 : 0);
            stmt.setInt(base++,    profile.isEnchantMessageEnabled(1) ? 1 : 0);
            stmt.setInt(base++,    profile.isEnchantMessageEnabled(2) ? 1 : 0);
            stmt.setInt(base++,    profile.isEnchantMessageEnabled(3) ? 1 : 0);
            stmt.setInt(base++,    profile.getGemUpgradeLevel());
            stmt.setInt(base++,    profile.getFarmMultiUpgradeLevel());
            stmt.setDouble(base++, profile.getGrindingPoints());
            stmt.setInt(base++,    profile.isGrindMessagesEnabled() ? 1 : 0);
            stmt.setInt(base++,    profile.getGrindChanceLevel());
            stmt.setInt(base++,    profile.getGrindExponentLevel());
            stmt.setInt(base++,    profile.getGrindFarmMultiLevel());
            stmt.setInt(base++,    profile.getGrindGemMultiLevel());
            stmt.setInt(base++,    profile.getGrindFarmXpLevel());
            stmt.setInt(base++,    profile.getGrindMineXpLevel());
            stmt.setInt(base++,    profile.getGrindSeedMultiLevel());
            stmt.setInt(base++,    profile.getGrindGPMultiLevel());
            stmt.setInt(base++,    profile.isLevelUpFarmMessageEnabled() ? 1 : 0);
            for (int i = 0; i < 5; i++) stmt.setInt(base++, profile.getPerkCount(i));
            for (int i = 0; i < 5; i++) stmt.setInt(base++, profile.getPerkChanceLevel(i));
            for (int i = 0; i < 5; i++) stmt.setInt(base++, profile.isPerkMessageEnabled(i) ? 1 : 0);
            stmt.setInt(base++, profile.getHoeSlot());
            stmt.setInt(base++, profile.getPickaxeSlot());
            stmt.setInt(base++, profile.getUpgradeSlot());
            stmt.setString(base++, profile.getArtifactSlot(0));
            stmt.setString(base++, profile.getArtifactSlot(1));
            stmt.setString(base++, profile.getArtifactSlot(2));
            stmt.setInt(base++, profile.isArtifactSlotUnlocked(1) ? 1 : 0);
            stmt.setInt(base++, profile.isArtifactSlotUnlocked(2) ? 1 : 0);
            StringBuilder vaultSb = new StringBuilder();
            for (int i = 0; i < 45; i++) {
                if (i > 0) vaultSb.append("|");
                vaultSb.append(profile.getArtifactVaultSlot(i));
            }
            stmt.setString(base++, vaultSb.toString());
            stmt.setInt(base++,    profile.getWorkerLevel());
            stmt.setDouble(base++, profile.getWorkerXp());
            stmt.setDouble(base++, profile.getWorkerEnergy());
            stmt.setInt(base++, profile.isArmorPieceUnlocked(1) ? 1 : 0);
            stmt.setInt(base++, profile.isArmorPieceUnlocked(2) ? 1 : 0);
            stmt.setInt(base++, profile.isArmorPieceUnlocked(3) ? 1 : 0);
            for (int i = 0; i < 4; i++) stmt.setInt(base++, profile.getArmorType(i));
            for (int i = 0; i < 4; i++) stmt.setDouble(base++, profile.getArmorValue(i));
            stmt.setInt(base++, profile.getArmorLowBuys());
            stmt.setInt(base++, profile.getArmorMedBuys());
            stmt.setInt(base++, profile.getArmorHighBuys());
            stmt.setInt(base++,  profile.isSkipAnimationUnlocked() ? 1 : 0);
            stmt.setInt(base++,  profile.isSkipAnimationEnabled()  ? 1 : 0);
            stmt.setLong(base++, profile.getCropsFarmed());
            stmt.setString(base,   uuid.toString());
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Could not sync profile for " + uuid);
            e.printStackTrace();
        }
    }

    public void saveAllOnlinePlayersSynchronously() {
        profileCache.forEach(this::saveProfileSynchronously);
    }

    public void handleQuit(UUID uuid) {
        PlayerProfile profile = profileCache.remove(uuid);
        if (profile != null) saveProfileSynchronously(uuid, profile);
    }

    private int    safeGetInt(ResultSet rs, String col, int def)       { try { return rs.getInt(col);    } catch (SQLException e) { return def; } }
    private long   safeGetLong(ResultSet rs, String col, long def)    { try { return rs.getLong(col);   } catch (SQLException e) { return def; } }
    private double safeGetDouble(ResultSet rs, String col, double def){ try { return rs.getDouble(col); } catch (SQLException e) { return def; } }
    private String safeGetString(ResultSet rs, String col, String def){ try { String v = rs.getString(col); return v != null ? v : def; } catch (SQLException e) { return def; } }
}