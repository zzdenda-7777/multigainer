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

    private int    safeGetInt(ResultSet rs, String col, int def)      { try { return rs.getInt(col);    } catch (SQLException e) { return def; } }
    private long   safeGetLong(ResultSet rs, String col, long def)    { try { return rs.getLong(col);   } catch (SQLException e) { return def; } }
    private double safeGetDouble(ResultSet rs, String col, double def){ try { return rs.getDouble(col); } catch (SQLException e) { return def; } }
}