package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.levels.MiningLevelManager;
import multigainer.multigainer.upgrades.UpgradeManager;
import multigainer.multigainer.rebirth.RebirthManager;
import multigainer.multigainer.tier.TierManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MiningListener implements Listener {
    private final Multigainer plugin;
    private final Map<UUID, Set<Location>> brokenCobbleCache = new HashMap<>();

    public MiningListener(Multigainer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.COBBLESTONE) return;
        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        Set<Location> cobbleCooldowns = brokenCobbleCache.computeIfAbsent(uuid, k -> new HashSet<>());
        if (cobbleCooldowns.contains(block.getLocation())) return;
        cobbleCooldowns.add(block.getLocation());

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(uuid);

        // --- Payout Calculations with Global Cross-System Multipliers ---
        BigNumber basePayout = new BigNumber(10.0);
        BigNumber compoundingMultiplier = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
        BigNumber mineMoneyMultiplier = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());

        // Dynamic Global Multiplier Hooks
        BigNumber rebirthMultiplier = new BigNumber(RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints()));
        BigNumber tierMultiplier = new BigNumber(TierManager.getMultiplierForTier(profile.getTier()));

        // Combined Stacked Calculation Formula (Base * Upgrades * MineLevel * Rebirth * Tier)
        BigNumber payout = basePayout.multiply(compoundingMultiplier)
                .multiply(mineMoneyMultiplier)
                .multiply(rebirthMultiplier)
                .multiply(tierMultiplier);

        // Base 1.0 Gem scaling compounded exponentially by mining level tier
        BigNumber baseGems = new BigNumber(1.0);
        BigNumber mineGemsMultiplier = MiningLevelManager.getGemsMultiplier(profile.getMiningLevel());
        BigNumber finalGems = baseGems.multiply(mineGemsMultiplier);

        // Update profile currency objects
        profile.setMoney(profile.getMoney().add(payout));
        profile.setGems(profile.getGems().add(finalGems));

        // --- Mining Level Engine Logic ---
        double currentXp = profile.getMiningXp() + 1.0;
        int currentLevel = profile.getMiningLevel();
        double requiredXp = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);

        while (currentXp >= requiredXp) {
            currentXp -= requiredXp;
            currentLevel++;
            requiredXp = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);
            player.sendMessage("§b§l[!] MINE LEVEL UP! §7Your Mining Level is now §e" + currentLevel + "§7!");
        }

        profile.setMiningXp(currentXp);
        profile.setMiningLevel(currentLevel);

        // Update your 8-parameter upgraded Scoreboard layout
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

        block.setType(Material.BEDROCK);

        Location holoLoc = block.getLocation().clone().add(0.5, 1.5, 0.5);
        if (holoLoc.getWorld() != null) {
            holoLoc.getWorld().spawn(holoLoc, ArmorStand.class, hologram -> {
                hologram.setVisible(false);
                hologram.setGravity(false);
                hologram.setMarker(true);
                hologram.setPersistent(false);
                hologram.setCustomName("§a+" + NumberFormatter.format(payout) + " Money");
                hologram.setCustomNameVisible(true);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (hologram.isValid()) {
                            hologram.remove();
                        }
                    }
                }.runTaskLater(plugin, 20L);
            });
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(Material.COBBLESTONE);
                cobbleCooldowns.remove(block.getLocation());
            }
        }.runTaskLater(plugin, 60L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block != null && brokenCobbleCache.getOrDefault(event.getPlayer().getUniqueId(), Collections.emptySet()).contains(block.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        brokenCobbleCache.remove(event.getPlayer().getUniqueId());
    }
}