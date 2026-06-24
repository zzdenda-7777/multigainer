package multigainer.multigainer;

import multigainer.multigainer.commands.StatsCommand;
import multigainer.multigainer.commands.ReloadCommand;
import multigainer.multigainer.commands.GiveCommand;
import multigainer.multigainer.rebirth.RebirthListener;
import multigainer.multigainer.rebirth.RebirthGUI;
import multigainer.multigainer.tier.TierGUI;
import multigainer.multigainer.tier.TierListener;
import multigainer.multigainer.data.PlayerDataManager;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.data.StorageManager;
import multigainer.multigainer.income.IncomeManager;
import multigainer.multigainer.listeners.MiningListener;
import multigainer.multigainer.listeners.FarmingListener;
import multigainer.multigainer.listeners.JoinListener;
import multigainer.multigainer.upgrades.UpgradeItemHandler;
import multigainer.multigainer.tools.PickaxeBlockStorageGUI;
import multigainer.multigainer.tools.PickaxeGUI;
import multigainer.multigainer.tools.PickaxeUpgradeGUI;
import multigainer.multigainer.tools.ToolGUI;
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

    private ScoreboardManager scoreboardManager;
    private StorageManager storageManager;
    private PlayerDataManager playerDataManager;
    private UpgradeItemHandler upgradeHandler;
    private ToolItemHandler toolHandler;
    private ToolGUI toolGUI;
    private PickaxeGUI pickaxeGUI;
    private PickaxeUpgradeGUI pickaxeUpgradeGUI;
    private PickaxeBlockStorageGUI pickaxeBlockStorageGUI;

    @Override
    public void onEnable() {
        this.storageManager = new StorageManager(this);
        this.storageManager.init();
        this.playerDataManager = new PlayerDataManager(this, storageManager);
        this.scoreboardManager = new ScoreboardManager(this);

        this.upgradeHandler = new UpgradeItemHandler(this);
        this.toolHandler = new ToolItemHandler(this);
        this.toolGUI = new ToolGUI(this);

        // Pickaxe GUI system
        this.pickaxeGUI = new PickaxeGUI(this);
        this.pickaxeUpgradeGUI = new PickaxeUpgradeGUI(this);
        this.pickaxeBlockStorageGUI = new PickaxeBlockStorageGUI(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmingListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this, playerDataManager), this);
        getServer().getPluginManager().registerEvents(upgradeHandler, this);
        getServer().getPluginManager().registerEvents(toolHandler, this);
        getServer().getPluginManager().registerEvents(toolGUI, this);
        getServer().getPluginManager().registerEvents(pickaxeGUI, this);
        getServer().getPluginManager().registerEvents(pickaxeUpgradeGUI, this);
        getServer().getPluginManager().registerEvents(pickaxeBlockStorageGUI, this);
        getServer().getPluginManager().registerEvents(new RebirthListener(this), this);
        getServer().getPluginManager().registerEvents(new TierListener(this), this);

        if (getCommand("upgrades") != null) getCommand("upgrades").setExecutor(upgradeHandler);
        if (getCommand("stats") != null) getCommand("stats").setExecutor(new StatsCommand(this));

        if (getCommand("multigainer") != null) {
            getCommand("multigainer").setExecutor((sender, command, label, args) -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                    String[] newArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                    return new GiveCommand(this).onCommand(sender, command, label, newArgs);
                }
                return new ReloadCommand(this).onCommand(sender, command, label, args);
            });
        }

        if (getCommand("rebirth") != null) {
            getCommand("rebirth").setExecutor((sender, cmd, label, args) -> {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                RebirthGUI.open(p, getPlayerDataManager().getProfile(p.getUniqueId()));
                return true;
            });
        }

        if (getCommand("tier") != null) {
            getCommand("tier").setExecutor((sender, cmd, label, args) -> {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                TierGUI.open(p, getPlayerDataManager().getProfile(p.getUniqueId()));
                return true;
            });
        }

        new IncomeManager(this);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerProfile profile = playerDataManager.getProfile(p.getUniqueId());
                    if (profile != null) {
                        scoreboardManager.updateScoreboard(p, profile.getMoney(), profile.getGems(),
                                profile.getRubies(), profile.getFarmingLevel(), profile.getFarmingXp(),
                                profile.getMiningLevel(), profile.getMiningXp());
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);

        getLogger().info("§a✓ Multigainer data systems successfully loaded!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAllOnlinePlayersSynchronously();
        if (storageManager != null) storageManager.close();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        PlayerProfile profile = playerDataManager.getProfile(p.getUniqueId());
        if (profile != null) {
            if (profile.getTier() < 0 || profile.getMoney().toDouble() == 0 && profile.getRebirthPoints() == 0 && profile.getTier() == 1) {
                profile.setTier(0);
            }

            scoreboardManager.createScoreboard(p, profile.getMoney(), profile.getGems(),
                    profile.getRubies(), profile.getFarmingLevel(), profile.getFarmingXp(),
                    profile.getMiningLevel(), profile.getMiningXp());
        }
        p.getInventory().setItem(0, toolHandler.getCustomHoe());
        p.getInventory().setItem(1, toolHandler.getPickaxeForProfile(profile));
        p.getInventory().setItem(4, upgradeHandler.getUpgradeEmerald());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.handleQuit(event.getPlayer().getUniqueId());
    }

    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ScoreboardManager getScoreboardManager() { return this.scoreboardManager; }
    public ToolItemHandler getToolHandler() { return toolHandler; }
    public ToolGUI getToolGUI() { return toolGUI; }
}
