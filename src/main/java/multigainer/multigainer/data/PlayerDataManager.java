package multigainer.multigainer.data;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.math.BigNumber;
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

    // Thread-safe map cache for 200 concurrent players
    private final Map<UUID, PlayerProfile> profileCache = new ConcurrentHashMap<>();

    public PlayerDataManager(Multigainer plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    /**
     * Gets an online player's profile directly from the fast in-memory cache.
     */
    public PlayerProfile getProfile(UUID uuid) {
        return profileCache.get(uuid);
    }

    /**
     * Loads a player's profile from the database asynchronously.
     * If the profile doesn't exist, it creates a new entry natively.
     */
    public CompletableFuture<PlayerProfile> loadProfileAsync(UUID uuid, String username) {
        CompletableFuture<PlayerProfile> future = new CompletableFuture<>();

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try (Connection conn = storageManager.getConnection()) {

                // 1. Check if the profile already exists in the table
                String selectQuery = "SELECT * FROM mg_player_profiles WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
                    stmt.setString(1, uuid.toString());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            // Reconstruct existing player metrics from database
                            PlayerProfile profile = new PlayerProfile(
                                    new BigNumber(rs.getDouble("money_mantissa"), rs.getDouble("money_exponent")),
                                    new BigNumber(rs.getDouble("gems_mantissa"), rs.getDouble("gems_exponent")),
                                    new BigNumber(rs.getDouble("rubies_mantissa"), rs.getDouble("rubies_exponent")),
                                    rs.getInt("upgrade_level")
                            );
                            profile.setFarmingLevel(rs.getInt("farming_level"));
                            profile.setFarmingXp(rs.getDouble("farming_xp"));
                            profile.setMiningLevel(rs.getInt("mining_level"));
                            profile.setMiningXp(rs.getDouble("mining_xp"));

                            profileCache.put(uuid, profile);
                            future.complete(profile);
                            return;
                        }
                    }
                }

                // 2. Fallback: Player is new. Create default profile rows inside database
                String insertProfile = "INSERT INTO mg_player_profiles (uuid, username) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertProfile)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, username);
                    stmt.executeUpdate();
                }

                String insertStats = "INSERT INTO mg_player_stats (uuid) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertStats)) {
                    stmt.setString(1, uuid.toString());
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

    /**
     * Saves a single player's profile data asynchronously.
     */
    public void saveProfileAsync(UUID uuid) {
        PlayerProfile profile = profileCache.get(uuid);
        if (profile == null) return;

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String updateQuery = "UPDATE mg_player_profiles SET "
                    + "money_mantissa = ?, money_exponent = ?, "
                    + "gems_mantissa = ?, gems_exponent = ?, "
                    + "rubies_mantissa = ?, rubies_exponent = ?, "
                    + "upgrade_level = ?, farming_level = ?, farming_xp = ?, "
                    + "mining_level = ?, mining_xp = ? WHERE uuid = ?";

            try (Connection conn = storageManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateQuery)) {

                stmt.setDouble(1, profile.getMoney().getMantissa());
                stmt.setDouble(2, profile.getMoney().getExponent());
                stmt.setDouble(3, profile.getGems().getMantissa());
                stmt.setDouble(4, profile.getGems().getExponent());
                stmt.setDouble(5, profile.getRubies().getMantissa());
                stmt.setDouble(6, profile.getRubies().getExponent());
                stmt.setInt(7, profile.getUpgradeLevel());
                stmt.setInt(8, profile.getFarmingLevel());
                stmt.setDouble(9, profile.getFarmingXp());
                stmt.setInt(10, profile.getMiningLevel());
                stmt.setDouble(11, profile.getMiningXp());
                stmt.setString(12, uuid.toString());

                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not save player profile data for UUID: " + uuid);
                e.printStackTrace();
            }
        });
    }

    /**
     * Synchronously saves an individual player's profile data.
     * Used strictly during server shutdown sequences.
     */
    public void saveProfileSynchronously(UUID uuid, PlayerProfile profile) {
        String updateQuery = "UPDATE mg_player_profiles SET "
                + "money_mantissa = ?, money_exponent = ?, "
                + "gems_mantissa = ?, gems_exponent = ?, "
                + "rubies_mantissa = ?, rubies_exponent = ?, "
                + "upgrade_level = ?, farming_level = ?, farming_xp = ?, "
                + "mining_level = ?, mining_xp = ? WHERE uuid = ?";

        try (Connection conn = storageManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery)) {

            stmt.setDouble(1, profile.getMoney().getMantissa());
            stmt.setDouble(2, profile.getMoney().getExponent());
            stmt.setDouble(3, profile.getGems().getMantissa());
            stmt.setDouble(4, profile.getGems().getExponent());
            stmt.setDouble(5, profile.getRubies().getMantissa());
            stmt.setDouble(6, profile.getRubies().getExponent());
            stmt.setInt(7, profile.getUpgradeLevel());
            stmt.setInt(8, profile.getFarmingLevel());
            stmt.setDouble(9, profile.getFarmingXp());
            stmt.setInt(10, profile.getMiningLevel());
            stmt.setDouble(11, profile.getMiningXp());
            stmt.setString(12, uuid.toString());

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not safely flush player data profile synchronously for UUID: " + uuid);
            e.printStackTrace();
        }
    }

    /**
     * Loops through all cached data profiles and flushes them to the DB synchronously.
     */
    public void saveAllOnlinePlayersSynchronously() {
        for (Map.Entry<UUID, PlayerProfile> entry : profileCache.entrySet()) {
            saveProfileSynchronously(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Saves a player's profile data one final time before evicting them from the active cache map.
     */
    public void handleQuit(UUID uuid) {
        PlayerProfile profile = profileCache.remove(uuid);
        if (profile != null) {
            saveProfileSynchronously(uuid, profile);
        }
    }
}