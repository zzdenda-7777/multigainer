package multigainer.multigainer;

import multigainer.multigainer.commands.StatsCommand;
import multigainer.multigainer.commands.ReloadCommand;
import multigainer.multigainer.rebirth.RebirthListener;
import multigainer.multigainer.rebirth.RebirthGUI;
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
        this.storageManager = new StorageManager(this);
        this.storageManager.init();

        this.playerDataManager = new PlayerDataManager(this, storageManager);

        this.scoreboardManager = new ScoreboardManager();
        this.packetManager = new PacketManager(this);
        this.miningListener = new MiningListener(this);
        this.incomeManager = new IncomeManager(this);
        this.upgradeHandler = new UpgradeItemHandler(this);
        this.toolHandler = new ToolItemHandler(this);

        FarmingListener farmingListener = new FarmingListener(this);
        JoinListener joinListener = new JoinListener(this, playerDataManager);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(miningListener, this);
        getServer().getPluginManager().registerEvents(farmingListener, this);
        getServer().getPluginManager().registerEvents(joinListener, this);
        getServer().getPluginManager().registerEvents(upgradeHandler, this);
        getServer().getPluginManager().registerEvents(toolHandler, this);

        // Register the Rebirth Listener
        getServer().getPluginManager().registerEvents(new RebirthListener(this), this);

        // Command Registrations
        if (getCommand("upgrades") != null) {
            getCommand("upgrades").setExecutor(upgradeHandler);
        }

        if (this.getCommand("stats") != null) {
            this.getCommand("stats").setExecutor(new StatsCommand(this));
        }

        if (this.getCommand("multigainer") != null) {
            this.getCommand("multigainer").setExecutor(new ReloadCommand(this));
        }

        // Add command for /rebirth
        if (getCommand("rebirth") != null) {
            getCommand("rebirth").setExecutor(new CommandExecutor() {
                @Override
                public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
                    if (!(sender instanceof Player)) return true;
                    Player p = (Player) sender;
                    RebirthGUI.open(p, getPlayerDataManager().getProfile(p.getUniqueId()));
                    return true;
                }
            });
        }

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
            player.getInventory().setItem(0, toolHandler.getCustomHoe());
            player.getInventory().setItem(1, toolHandler.getCustomPickaxe());
            player.getInventory().setItem(4, upgradeHandler.getUpgradeEmerald());
        }

        getLogger().info("§a✓ Multigainer data systems successfully loaded!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllOnlinePlayersSynchronously();
        }
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
        player.getInventory().setItem(0, toolHandler.getCustomHoe());
        player.getInventory().setItem(1, toolHandler.getCustomPickaxe());
        player.getInventory().setItem(4, upgradeHandler.getUpgradeEmerald());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.handleQuit(event.getPlayer().getUniqueId());
    }

    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
}