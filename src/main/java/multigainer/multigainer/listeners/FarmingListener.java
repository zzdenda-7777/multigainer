package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
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

        BigNumber baseMoney = new BigNumber(0.25);
        BigNumber activeMultiplier = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
        BigNumber finalPayout = baseMoney.multiply(activeMultiplier);

        profile.setMoney(profile.getMoney().add(finalPayout));
        plugin.getScoreboardManager().updateScoreboard(player, profile.getMoney(), profile.getGems(), profile.getRubies());

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

    // Fixed: Blocks interaction on harvested gaps to prevent visual client-side wheat duplication loops
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