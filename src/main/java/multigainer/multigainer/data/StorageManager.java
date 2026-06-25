package multigainer.multigainer.data;

import multigainer.multigainer.Multigainer;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class StorageManager {
    private final Multigainer plugin;
    private File dbFile;

    public StorageManager(Multigainer plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.getLogger().info("=================================================");
        plugin.getLogger().info("🚀 MULTIGAINER DEBUG: SWITCHING TO LOCAL SQLITE 🚀");
        plugin.getLogger().info("=================================================");

        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        this.dbFile = new File(plugin.getDataFolder(), "userdata.db");

        try {
            if (!dbFile.exists()) {
                dbFile.createNewFile();
                plugin.getLogger().info("Successfully created fresh local userdata.db file.");
            }
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite driver could not be found within the server container!");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to physically write the local data file to disk!");
            e.printStackTrace();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::setupTables);
    }

    private void setupTables() {
        String createProfilesTable = "CREATE TABLE IF NOT EXISTS mg_player_profiles ("
                + "uuid TEXT PRIMARY KEY, username TEXT, "
                + "money_mantissa REAL DEFAULT 0, money_exponent REAL DEFAULT 0, "
                + "gems_mantissa REAL DEFAULT 0, gems_exponent REAL DEFAULT 0, "
                + "rubies_mantissa REAL DEFAULT 0, rubies_exponent REAL DEFAULT 0, "
                + "upgrade_level INTEGER DEFAULT 0, tier INTEGER DEFAULT 1, tier_points INTEGER DEFAULT 0, "
                + "farming_level INTEGER DEFAULT 1, farming_xp REAL DEFAULT 0, "
                + "mining_level INTEGER DEFAULT 1, mining_xp REAL DEFAULT 0, "
                + "rebirth_points REAL DEFAULT 0, rebirth_count INTEGER DEFAULT 0"
                + ");";

        String createStatsTable = "CREATE TABLE IF NOT EXISTS mg_player_stats ("
                + "uuid TEXT PRIMARY KEY, kills INTEGER DEFAULT 0, deaths INTEGER DEFAULT 0, "
                + "playtime BIGINT DEFAULT 0, "
                + "FOREIGN KEY (uuid) REFERENCES mg_player_profiles(uuid) ON DELETE CASCADE);";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute(createProfilesTable);
            stmt.execute(createStatsTable);

            // Legacy migrations — all silently ignored if column already exists
            String[] all = {
                    // Core legacy
                    "ALTER TABLE mg_player_profiles ADD COLUMN tier INTEGER DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN tier_points INTEGER DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN rebirth_points REAL DEFAULT 0.0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN rebirth_count INTEGER DEFAULT 0;",
                    // Pickaxe
                    "ALTER TABLE mg_player_profiles ADD COLUMN pickaxe_tier INTEGER DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN mining_speed_level INTEGER DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN xp_multi_level INTEGER DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN gem_multi_level INTEGER DEFAULT 0;",
                    // Block storage
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_cobblestone BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_cobbled_deepslate BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_copper_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_deepslate_copper_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_coal_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_deepslate_coal_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_iron_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_deepslate_iron_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_redstone_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_deepslate_redstone_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_lapis_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_deepslate_lapis_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_gold_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_deepslate_gold_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_diamond_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_deepslate_diamond_ore BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN storage_netherite_block BIGINT DEFAULT 0;",
                    // Farming (seed currencies 0-6, meta)
                    "ALTER TABLE mg_player_profiles ADD COLUMN seed_storage_0 BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN seed_storage_1 BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN seed_storage_2 BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN seed_storage_3 BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN seed_storage_4 BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN seed_storage_5 BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN seed_storage_6 BIGINT DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN farm_multi REAL DEFAULT 1.0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN chosen_crop INTEGER DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN enchant_msg_tnt INTEGER DEFAULT 1;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN enchant_msg_nuke INTEGER DEFAULT 1;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN enchant_msg_world_eater INTEGER DEFAULT 1;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN enchant_msg_universe_destroyer INTEGER DEFAULT 1;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN hoe_tier INTEGER DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN auto_merge INTEGER DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN gem_upgrade_level INTEGER DEFAULT 0;",
                    "ALTER TABLE mg_player_profiles ADD COLUMN farm_multi_upgrade_level INTEGER DEFAULT 0;"
            };
            for (String sql : all) {
                try { stmt.execute(sql); } catch (SQLException ignored) {}
            }

            plugin.getLogger().info("Database initialization complete. Local schemas checked.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Critical exception encountered setting up local SQL tables!");
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dbFile == null) throw new SQLException("Cannot pull connection: SQLite database file uninitialized.");
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    public void close() {
        plugin.getLogger().info("Local database system safely disconnected.");
    }
}