package multigainer.multigainer.data;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final Multigainer plugin;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    private final File dataFolder;

    public PlayerDataManager(Multigainer plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
        }
    }

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, k -> loadProfile(uuid));
    }

    /**
     * Loads a player profile, safely rebuilding BigNumbers from saved file structures.
     */
    public PlayerProfile loadProfile(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return new PlayerProfile(); // Returns a clean, fresh default profile setup
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Load data by splitting mantissas and exponents to safely cross the e308 barrier
        double moneyMat = config.getDouble("money.mantissa", 0.0);
        double moneyExp = config.getDouble("money.exponent", 0.0);

        double gemsMat = config.getDouble("gems.mantissa", 0.0);
        double gemsExp = config.getDouble("gems.exponent", 0.0);

        double rubiesMat = config.getDouble("rubies.mantissa", 0.0);
        double rubiesExp = config.getDouble("rubies.exponent", 0.0);

        int upgradeLevel = config.getInt("upgradeLevel", 0);

        return new PlayerProfile(
                new BigNumber(moneyMat, moneyExp),
                new BigNumber(gemsMat, gemsExp),
                new BigNumber(rubiesMat, rubiesExp),
                upgradeLevel
        );
    }

    /**
     * Saves profile data safely without risking YAML primitive overflow limits.
     */
    public void saveProfile(UUID uuid) {
        PlayerProfile profile = profiles.get(uuid);
        if (profile == null) return;

        File file = new File(dataFolder, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // Save pieces individually to completely preserve precision up to infinite scales
        config.set("money.mantissa", profile.getMoney().getMantissa());
        config.set("money.exponent", profile.getMoney().getExponent());

        config.set("gems.mantissa", profile.getGems().getMantissa());
        config.set("gems.exponent", profile.getGems().getExponent());

        config.set("rubies.mantissa", profile.getRubies().getMantissa());
        config.set("rubies.exponent", profile.getRubies().getExponent());

        config.set("upgradeLevel", profile.getUpgradeLevel());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data profile for UUID: " + uuid);
            e.printStackTrace();
        }
    }

    /**
     * Saves every currently cached player profile to the disk.
     */
    public void saveAllOnlinePlayers() {
        for (UUID uuid : profiles.keySet()) {
            saveProfile(uuid);
        }
    }

    /**
     * Saves a player's data one last time before removing them from memory.
     */
    public void removeProfile(UUID uuid) {
        saveProfile(uuid);
        profiles.remove(uuid);
    }
}