package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.levels.FarmingLevelManager; // Added import to access level requirements
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.upgrades.UpgradeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class FarmingListener implements Listener {
    private final Multigainer plugin;
    private final Map<UUID, Set<Location>> brokenWheatCache = new HashMap<>();

    public FarmingListener(Multigainer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFarmlandMoistureLoss(MoistureChangeEvent event) {
        if (event.getBlock().getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFarmlandTrample(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        brokenWheatCache.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCropPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.WHEAT) {
            Ageable wheatData = (Ageable) block.getBlockData();
            wheatData.setAge(wheatData.getMaximumAge());
            block.setBlockData(wheatData);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCropBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.WHEAT) return;

        UUID uuid = player.getUniqueId();
        Set<Location> cooldowns = brokenWheatCache.computeIfAbsent(uuid, k -> new HashSet<>());
        Location loc = block.getLocation();

        if (cooldowns.contains(loc)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(uuid);

        // --- Money Calculations ---
        BigNumber baseMoney = new BigNumber(0.25);
        BigNumber activeMultiplier = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
        BigNumber finalPayout = baseMoney.multiply(activeMultiplier);
        profile.setMoney(profile.getMoney().add(finalPayout));

        // --- Farming XP & Leveling Logic ---
        double currentXp = profile.getFarmingXp() + 1.0; // Grants exactly 1 XP per wheat broken
        int currentLevel = profile.getFarmingLevel();
        double requiredXp = FarmingLevelManager.getRequiredXpForNextLevel(currentLevel);

        // Check if the player earned enough XP to advance their level tier
        while (currentXp >= requiredXp) {
            currentXp -= requiredXp; // Retain rollover XP for the next level
            currentLevel++;
            requiredXp = FarmingLevelManager.getRequiredXpForNextLevel(currentLevel);

            // Visual alert message to notify the player of their advancement
            player.sendMessage("§a§l[!] LEVEL UP! §7Your Farming Level is now §e" + currentLevel + "§7!");
        }

        // Save updated values back to the player profile object
        profile.setFarmingXp(currentXp);
        profile.setFarmingLevel(currentLevel);

        // Update the scoreboard UI with the modified stats
        plugin.getScoreboardManager().updateScoreboard(
                player,
                profile.getMoney(),
                profile.getGems(),
                profile.getRubies(),
                profile.getFarmingLevel(),
                profile.getFarmingXp(),
                profile.getMiningLevel(),
                profile.getMiningXp()
        );

        cooldowns.add(loc);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendBlockChange(loc, Material.AIR.createBlockData());
            }
        }, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cooldowns.remove(loc);
            if (player.isOnline() && loc.getBlock().getType() == Material.WHEAT) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }, 60L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Set<Location> cooldowns = brokenWheatCache.get(player.getUniqueId());
        if (cooldowns != null && cooldowns.contains(block.getLocation())) {
            event.setCancelled(true);
            player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
        }
    }
}