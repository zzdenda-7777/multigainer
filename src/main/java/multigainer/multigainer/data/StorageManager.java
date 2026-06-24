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

    /**
     * Initializes the local SQLite database file and sets up tables asynchronously.
     */
    public void init() {
        plugin.getLogger().info("=================================================");
        plugin.getLogger().info("🚀 MULTIGAINER DEBUG: SWITCHING TO LOCAL SQLITE 🚀");
        plugin.getLogger().info("=================================================");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

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

    /**
     * Executes internal DDL commands to ensure local tables are ready.
     */
    private void setupTables() {
        String createProfilesTable = "CREATE TABLE IF NOT EXISTS mg_player_profiles ("
                + "uuid TEXT PRIMARY KEY, "
                + "username TEXT, "
                + "money_mantissa REAL DEFAULT 0, money_exponent REAL DEFAULT 0, "
                + "gems_mantissa REAL DEFAULT 0, gems_exponent REAL DEFAULT 0, "
                + "rubies_mantissa REAL DEFAULT 0, rubies_exponent REAL DEFAULT 0, "
                + "upgrade_level INTEGER DEFAULT 0, "
                + "tier INTEGER DEFAULT 1, "
                + "tier_points INTEGER DEFAULT 0, " // Added tier_points column
                + "farming_level INTEGER DEFAULT 1, farming_xp REAL DEFAULT 0, "
                + "mining_level INTEGER DEFAULT 1, mining_xp REAL DEFAULT 0, "
                + "rebirth_points REAL DEFAULT 0, "
                + "rebirth_count INTEGER DEFAULT 0"
                + ");";

        String createStatsTable = "CREATE TABLE IF NOT EXISTS mg_player_stats ("
                + "uuid TEXT PRIMARY KEY, "
                + "kills INTEGER DEFAULT 0, "
                + "deaths INTEGER DEFAULT 0, "
                + "playtime BIGINT DEFAULT 0, "
                + "FOREIGN KEY (uuid) REFERENCES mg_player_profiles(uuid) ON DELETE CASCADE"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute(createProfilesTable);
            stmt.execute(createStatsTable);

            // AUTOMATIC MIGRATION: Programmatically adds new columns if an old database file exists
            try {
                stmt.execute("ALTER TABLE mg_player_profiles ADD COLUMN tier INTEGER DEFAULT 0;");
                plugin.getLogger().info("⚠️ Database Migration: Added missing 'tier' column.");
            } catch (SQLException ignored) {
                // Column already exists, safe to ignore
            }

            // ADDED: Auto-migration block for tier_points column
            try {
                stmt.execute("ALTER TABLE mg_player_profiles ADD COLUMN tier_points INTEGER DEFAULT 0;");
                plugin.getLogger().info("⚠️ Database Migration: Added missing 'tier_points' column.");
            } catch (SQLException ignored) {
                // Column already exists, safe to ignore
            }

            try {
                stmt.execute("ALTER TABLE mg_player_profiles ADD COLUMN rebirth_points REAL DEFAULT 0.0;");
                plugin.getLogger().info("⚠️ Database Migration: Added missing 'rebirth_points' column.");
            } catch (SQLException ignored) {
                // Column already exists, safe to ignore
            }

            try {
                stmt.execute("ALTER TABLE mg_player_profiles ADD COLUMN rebirth_count INTEGER DEFAULT 0;");
                plugin.getLogger().info("⚠️ Database Migration: Added missing 'rebirth_count' column.");
            } catch (SQLException ignored) {
                // Column already exists, safe to ignore
            }

            // Pickaxe progression columns
            String[] pickaxeCols = {
                "ALTER TABLE mg_player_profiles ADD COLUMN pickaxe_tier INTEGER DEFAULT 0;",
                "ALTER TABLE mg_player_profiles ADD COLUMN mining_speed_level INTEGER DEFAULT 0;",
                "ALTER TABLE mg_player_profiles ADD COLUMN xp_multi_level INTEGER DEFAULT 0;",
                "ALTER TABLE mg_player_profiles ADD COLUMN gem_multi_level INTEGER DEFAULT 0;"
            };
            for (String sql : pickaxeCols) {
                try { stmt.execute(sql); } catch (SQLException ignored) {}
            }

            // Block storage columns (one counter per mineable block type)
            String[] storageCols = {
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
                "ALTER TABLE mg_player_profiles ADD COLUMN storage_netherite_block BIGINT DEFAULT 0;"
            };
            for (String sql : storageCols) {
                try { stmt.execute(sql); } catch (SQLException ignored) {}
            }

            plugin.getLogger().info("Database initialization complete. Local schemas checked.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Critical exception encountered setting up local SQL tables!");
            e.printStackTrace();
        }
    }

    /**
     * Provides an active connection straight to the local flat file.
     */
    public Connection getConnection() throws SQLException {
        if (dbFile == null) {
            throw new SQLException("Cannot pull connection: SQLite database file uninitialized.");
        }
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    /**
     * Flushes local resource allocations on closure.
     */
    public void close() {
        plugin.getLogger().info("Local database system safely disconnected.");
    }
}