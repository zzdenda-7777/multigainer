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

    public PlayerProfile getProfile(UUID uuid) {
        return profileCache.get(uuid);
    }

    public CompletableFuture<PlayerProfile> loadProfileAsync(UUID uuid, String username) {
        CompletableFuture<PlayerProfile> future = new CompletableFuture<>();

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try (Connection conn = storageManager.getConnection()) {
                String selectQuery = "SELECT * FROM mg_player_profiles WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
                    stmt.setString(1, uuid.toString());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            PlayerProfile profile = new PlayerProfile(
                                    new BigNumber(rs.getDouble("money_mantissa"), rs.getDouble("money_exponent")),
                                    new BigNumber(rs.getDouble("gems_mantissa"), rs.getDouble("gems_exponent")),
                                    new BigNumber(rs.getDouble("rubies_mantissa"), rs.getDouble("rubies_exponent")),
                                    rs.getInt("upgrade_level"),
                                    rs.getInt("tier"),
                                    rs.getInt("tier_points"),
                                    rs.getInt("farming_level"),
                                    rs.getDouble("farming_xp"),
                                    rs.getInt("mining_level"),
                                    rs.getDouble("mining_xp"),
                                    rs.getDouble("rebirth_points"),
                                    rs.getInt("rebirth_count")
                            );

                            // Load pickaxe progression fields (0 if column not yet created)
                            profile.setPickaxeTier(safeGetInt(rs, "pickaxe_tier", 0));
                            profile.setMiningSpeedLevel(safeGetInt(rs, "mining_speed_level", 0));
                            profile.setXpMultiLevel(safeGetInt(rs, "xp_multi_level", 0));
                            profile.setGemMultiLevel(safeGetInt(rs, "gem_multi_level", 0));

                            // Load block storage counters
                            long[] storage = new long[17];
                            for (int i = 0; i < PickaxeManager.BLOCK_COLUMN_NAMES.length; i++) {
                                storage[i] = safeGetLong(rs, PickaxeManager.BLOCK_COLUMN_NAMES[i], 0L);
                            }
                            profile.setBlockStorageArray(storage);

                            profileCache.put(uuid, profile);
                            future.complete(profile);
                            return;
                        }
                    }
                }

                // New player
                String insertProfile = "INSERT INTO mg_player_profiles (uuid, username) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertProfile)) {
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
        StringBuilder updateQuery = new StringBuilder(
            "UPDATE mg_player_profiles SET "
            + "money_mantissa = ?, money_exponent = ?, "
            + "gems_mantissa = ?, gems_exponent = ?, "
            + "rubies_mantissa = ?, rubies_exponent = ?, "
            + "upgrade_level = ?, tier = ?, tier_points = ?, farming_level = ?, farming_xp = ?, "
            + "mining_level = ?, mining_xp = ?, rebirth_points = ?, rebirth_count = ?, "
            + "pickaxe_tier = ?, mining_speed_level = ?, xp_multi_level = ?, gem_multi_level = ?"
        );
        for (String col : PickaxeManager.BLOCK_COLUMN_NAMES) {
            updateQuery.append(", ").append(col).append(" = ?");
        }
        updateQuery.append(" WHERE uuid = ?");

        try (Connection conn = storageManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery.toString())) {

            stmt.setDouble(1, profile.getMoney().getMantissa());
            stmt.setDouble(2, profile.getMoney().getExponent());
            stmt.setDouble(3, profile.getGems().getMantissa());
            stmt.setDouble(4, profile.getGems().getExponent());
            stmt.setDouble(5, profile.getRubies().getMantissa());
            stmt.setDouble(6, profile.getRubies().getExponent());
            stmt.setInt(7, profile.getUpgradeLevel());
            stmt.setInt(8, profile.getTier());
            stmt.setInt(9, profile.getTierPoints());
            stmt.setInt(10, profile.getFarmingLevel());
            stmt.setDouble(11, profile.getFarmingXp());
            stmt.setInt(12, profile.getMiningLevel());
            stmt.setDouble(13, profile.getMiningXp());
            stmt.setDouble(14, profile.getRebirthPoints());
            stmt.setInt(15, profile.getRebirthCount());
            stmt.setInt(16, profile.getPickaxeTier());
            stmt.setInt(17, profile.getMiningSpeedLevel());
            stmt.setInt(18, profile.getXpMultiLevel());
            stmt.setInt(19, profile.getGemMultiLevel());

            // Block storage (params 20 to 36)
            for (int i = 0; i < PickaxeManager.BLOCK_COLUMN_NAMES.length; i++) {
                stmt.setLong(20 + i, profile.getBlockStorage(i));
            }

            stmt.setString(20 + PickaxeManager.BLOCK_COLUMN_NAMES.length, uuid.toString());
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

    private int safeGetInt(ResultSet rs, String column, int defaultValue) {
        try { return rs.getInt(column); } catch (SQLException e) { return defaultValue; }
    }

    private long safeGetLong(ResultSet rs, String column, long defaultValue) {
        try { return rs.getLong(column); } catch (SQLException e) { return defaultValue; }
    }
}