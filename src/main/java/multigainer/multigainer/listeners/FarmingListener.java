package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.levels.FarmingLevelManager;
import multigainer.multigainer.levels.MiningLevelManager; // IMPORTED
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.upgrades.UpgradeManager;
import multigainer.multigainer.rebirth.RebirthManager;
import multigainer.multigainer.tier.TierManager;
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

        // --- Money Calculations with Global Cross-System Multipliers ---
        BigNumber baseMoney = new BigNumber(0.25);
        BigNumber activeMultiplier = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());

        // Dynamic Global Multiplier Hooks
        BigNumber rebirthMultiplier = new BigNumber(RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints()));
        BigNumber tierMultiplier = new BigNumber(TierManager.getMultiplierForTier(profile.getTier()));

        // FIX: Grab Mining Multiplier to match the Scoreboard & Passive Income math
        BigNumber mineMoneyMultiplier = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());

        // Stacked Calculation Formula (Base * Upgrades * Rebirth * Tier * Mining)
        BigNumber finalPayout = baseMoney.multiply(activeMultiplier)
                .multiply(rebirthMultiplier)
                .multiply(tierMultiplier)
                .multiply(mineMoneyMultiplier);

        profile.setMoney(profile.getMoney().add(finalPayout));

        // --- Farming XP & Leveling Logic ---
        double currentXp = profile.getFarmingXp() + 1.0;
        int currentLevel = profile.getFarmingLevel();
        double requiredXp = FarmingLevelManager.getRequiredXpForNextLevel(currentLevel);

        while (currentXp >= requiredXp) {
            currentXp -= requiredXp;
            currentLevel++;
            requiredXp = FarmingLevelManager.getRequiredXpForNextLevel(currentLevel);
            player.sendMessage("§a§l[!] LEVEL UP! §7Your Farming Level is now §e" + currentLevel + "§7!");
        }

        profile.setFarmingXp(currentXp);
        profile.setFarmingLevel(currentLevel);

        // Update the scoreboard UI with the modified stats
        if (plugin.getScoreboardManager() != null) {
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
        }

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