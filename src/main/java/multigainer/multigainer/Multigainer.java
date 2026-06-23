package multigainer.multigainer;

import multigainer.multigainer.data.PlayerDataManager;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.data.StorageManager;
import multigainer.multigainer.income.IncomeManager;
import multigainer.multigainer.listeners.MiningListener;
import multigainer.multigainer.listeners.FarmingListener;
import multigainer.multigainer.listeners.JoinListener;
import multigainer.multigainer.upgrades.UpgradeItemHandler;
import multigainer.multigainer.tools.ToolItemHandler;
import multigainer.multigainer.packet.PacketManager;
import multigainer.multigainer.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Multigainer extends JavaPlugin implements Listener {

    private MiningListener miningListener;
    private ScoreboardManager scoreboardManager;
    private PacketManager packetManager;
    private StorageManager storageManager;
    private PlayerDataManager playerDataManager;
    private IncomeManager incomeManager;
    private UpgradeItemHandler upgradeHandler;
    private ToolItemHandler toolHandler;

    @Override
    public void onEnable() {
        // 1. Initialize Database Connection Pool Infrastructure First
        this.storageManager = new StorageManager(this);
        this.storageManager.init();

        // 2. Provide the database connection pool manager to your Player Data system
        this.playerDataManager = new PlayerDataManager(this, storageManager);

        this.scoreboardManager = new ScoreboardManager();
        this.packetManager = new PacketManager(this);
        this.miningListener = new MiningListener(this);
        this.incomeManager = new IncomeManager(this);
        this.upgradeHandler = new UpgradeItemHandler(this);
        this.toolHandler = new ToolItemHandler(this);

        FarmingListener farmingListener = new FarmingListener(this);
        JoinListener joinListener = new JoinListener(this, playerDataManager);

        // Register event systems
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(miningListener, this);
        getServer().getPluginManager().registerEvents(farmingListener, this);
        getServer().getPluginManager().registerEvents(joinListener, this);
        getServer().getPluginManager().registerEvents(upgradeHandler, this);
        getServer().getPluginManager().registerEvents(toolHandler, this);

        if (getCommand("upgrades") != null) {
            getCommand("upgrades").setExecutor(upgradeHandler);
        }

        // Handles all currently online players if the plugin reloads
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = playerDataManager.getProfile(player.getUniqueId());

            if (profile != null) {
                scoreboardManager.createScoreboard(player, profile.getMoney(),
                        profile.getGems(),
                        profile.getRubies(),
                        profile.getFarmingLevel(),
                        profile.getFarmingXp(),
                        profile.getMiningLevel(),
                        profile.getMiningXp()
                );
            }

            // Give items safely on startup
            player.getInventory().setItem(0, toolHandler.getCustomHoe());
            player.getInventory().setItem(1, toolHandler.getCustomPickaxe());
            player.getInventory().setItem(4, upgradeHandler.getUpgradeEmerald());
        }

        getLogger().info("§a✓ Multigainer data systems successfully loaded!");
    }

    @Override
    public void onDisable() {
        // 3. Use the updated synchronous database flushing method call
        if (playerDataManager != null) {
            playerDataManager.saveAllOnlinePlayersSynchronously();
        }

        // 4. Close database pool connections cleanly on server stop/reload
        if (storageManager != null) {
            storageManager.close();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = playerDataManager.getProfile(player.getUniqueId());

        if (profile != null) {
            scoreboardManager.createScoreboard(player, profile.getMoney(),
                    profile.getGems(),
                    profile.getRubies(),
                    profile.getFarmingLevel(),
                    profile.getFarmingXp(),
                    profile.getMiningLevel(),
                    profile.getMiningXp()
            );
        }

        // Give items safely when a player joins the server
        player.getInventory().setItem(0, toolHandler.getCustomHoe());
        player.getInventory().setItem(1, toolHandler.getCustomPickaxe());
        player.getInventory().setItem(4, upgradeHandler.getUpgradeEmerald());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 5. Use updated eviction and data synchronization sequence method name
        playerDataManager.handleQuit(event.getPlayer().getUniqueId());
    }

    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
}