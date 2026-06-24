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
                            // Map all fields to the new PlayerProfile constructor including tier_points
                            PlayerProfile profile = new PlayerProfile(
                                    new BigNumber(rs.getDouble("money_mantissa"), rs.getDouble("money_exponent")),
                                    new BigNumber(rs.getDouble("gems_mantissa"), rs.getDouble("gems_exponent")),
                                    new BigNumber(rs.getDouble("rubies_mantissa"), rs.getDouble("rubies_exponent")),
                                    rs.getInt("upgrade_level"),
                                    rs.getInt("tier"),
                                    rs.getInt("tier_points"), // Loaded from DB
                                    rs.getInt("farming_level"),
                                    rs.getDouble("farming_xp"),
                                    rs.getInt("mining_level"),
                                    rs.getDouble("mining_xp"),
                                    rs.getDouble("rebirth_points"),
                                    rs.getInt("rebirth_count")
                            );
                            profileCache.put(uuid, profile);
                            future.complete(profile);
                            return;
                        }
                    }
                }

                // New player initialization
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
        String updateQuery = "UPDATE mg_player_profiles SET "
                + "money_mantissa = ?, money_exponent = ?, "
                + "gems_mantissa = ?, gems_exponent = ?, "
                + "rubies_mantissa = ?, rubies_exponent = ?, "
                + "upgrade_level = ?, tier = ?, tier_points = ?, farming_level = ?, farming_xp = ?, "
                + "mining_level = ?, mining_xp = ?, rebirth_points = ?, rebirth_count = ? WHERE uuid = ?";

        try (Connection conn = storageManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery)) {

            stmt.setDouble(1, profile.getMoney().getMantissa());
            stmt.setDouble(2, profile.getMoney().getExponent());
            stmt.setDouble(3, profile.getGems().getMantissa());
            stmt.setDouble(4, profile.getGems().getExponent());
            stmt.setDouble(5, profile.getRubies().getMantissa());
            stmt.setDouble(6, profile.getRubies().getExponent());
            stmt.setInt(7, profile.getUpgradeLevel());
            stmt.setInt(8, profile.getTier());
            stmt.setInt(9, profile.getTierPoints()); // Saved to DB
            stmt.setInt(10, profile.getFarmingLevel());
            stmt.setDouble(11, profile.getFarmingXp());
            stmt.setInt(12, profile.getMiningLevel());
            stmt.setDouble(13, profile.getMiningXp());
            stmt.setDouble(14, profile.getRebirthPoints());
            stmt.setInt(15, profile.getRebirthCount());
            stmt.setString(16, uuid.toString());

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
}