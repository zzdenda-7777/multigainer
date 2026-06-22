package multigainer.multigainer;

import multigainer.multigainer.data.PlayerDataManager;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.income.IncomeManager;
import multigainer.multigainer.listeners.MiningListener;
import multigainer.multigainer.listeners.FarmingListener;
import multigainer.multigainer.upgrades.UpgradeItemHandler; // Fixed import
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
    private PlayerDataManager playerDataManager;
    private IncomeManager incomeManager;
    private UpgradeItemHandler upgradeHandler; // Stored as field

    @Override
    public void onEnable() {
        this.playerDataManager = new PlayerDataManager(this);
        this.scoreboardManager = new ScoreboardManager();
        this.packetManager = new PacketManager(this);
        this.miningListener = new MiningListener(this);
        this.incomeManager = new IncomeManager(this);
        this.upgradeHandler = new UpgradeItemHandler(this); // Initialized

        FarmingListener farmingListener = new FarmingListener(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(miningListener, this);
        getServer().getPluginManager().registerEvents(farmingListener, this);
        getServer().getPluginManager().registerEvents(upgradeHandler, this);

        if (getCommand("upgrades") != null) {
            getCommand("upgrades").setExecutor(upgradeHandler);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = playerDataManager.getProfile(player.getUniqueId());
            scoreboardManager.createScoreboard(player, profile.getMoney(), profile.getGems(), profile.getRubies());
            player.getInventory().setItem(4, upgradeHandler.getUpgradeEmerald());
        }

        getLogger().info("§a✓ Multigainer data systems successfully loaded!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllOnlinePlayers();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = playerDataManager.getProfile(player.getUniqueId());
        scoreboardManager.createScoreboard(player, profile.getMoney(), profile.getGems(), profile.getRubies());
        player.getInventory().setItem(4, upgradeHandler.getUpgradeEmerald());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.removeProfile(event.getPlayer().getUniqueId());
    }

    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
}