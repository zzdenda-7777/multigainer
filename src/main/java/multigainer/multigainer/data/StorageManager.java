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

        // Create the plugin data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Define the target file where all your data will be securely stored
        this.dbFile = new File(plugin.getDataFolder(), "userdata.db");

        try {
            if (!dbFile.exists()) {
                dbFile.createNewFile();
                plugin.getLogger().info("Successfully created fresh local userdata.db file.");
            }

            // Load the native SQLite driver built directly into Paper
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

        // Run table generation safely in an asynchronous task background thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::setupTables);
    }

    /**
     * Executes internal DDL commands to ensure local tables are ready.
     */
    private void setupTables() {
        // Note: Cleaned MySQL-specific engine properties to match native SQLite syntax
        String createProfilesTable = "CREATE TABLE IF NOT EXISTS mg_player_profiles ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "username VARCHAR(16) NOT NULL,"
                + "money_mantissa DOUBLE DEFAULT 0.0,"
                + "money_exponent DOUBLE DEFAULT 0.0,"
                + "gems_mantissa DOUBLE DEFAULT 0.0,"
                + "gems_exponent DOUBLE DEFAULT 0.0,"
                + "rubies_mantissa DOUBLE DEFAULT 0.0,"
                + "rubies_exponent DOUBLE DEFAULT 0.0,"
                + "upgrade_level INT DEFAULT 0,"
                + "farming_level INT DEFAULT 1,"
                + "farming_xp DOUBLE DEFAULT 0.0,"
                + "mining_level INT DEFAULT 1,"
                + "mining_xp DOUBLE DEFAULT 0.0,"
                + "PRIMARY KEY (uuid)"
                + ");";

        String createStatsTable = "CREATE TABLE IF NOT EXISTS mg_player_stats ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "kills INT DEFAULT 0,"
                + "deaths INT DEFAULT 0,"
                + "playtime BIGINT DEFAULT 0,"
                + "PRIMARY KEY (uuid),"
                + "FOREIGN KEY (uuid) REFERENCES mg_player_profiles(uuid) ON DELETE CASCADE"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Enable foreign keys explicitly inside SQLite session connections
            stmt.execute("PRAGMA foreign_keys = ON;");

            stmt.execute(createProfilesTable);
            stmt.execute(createStatsTable);

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