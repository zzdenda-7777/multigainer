package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.farming.FarmingManager;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.levels.FarmingLevelManager;
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
    private final Map<UUID, Set<Location>> brokenCropCache = new HashMap<>();
    private final Random random = new Random();

    public FarmingListener(Multigainer plugin) { this.plugin = plugin; }

    @EventHandler
    public void onFarmlandMoistureLoss(MoistureChangeEvent event) {
        if (event.getBlock().getType() == Material.FARMLAND) event.setCancelled(true);
    }

    @EventHandler
    public void onFarmlandTrample(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            Block b = event.getClickedBlock();
            if (b != null && b.getType() == Material.FARMLAND) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        brokenCropCache.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCropPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            block.setBlockData(ageable);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCropBreak(BlockBreakEvent event) {
        Block  block  = event.getBlock();
        Player player = event.getPlayer();
        if (block.getType() != Material.WHEAT) return;

        UUID uuid = player.getUniqueId();
        Set<Location> cooldowns = brokenCropCache.computeIfAbsent(uuid, k -> new HashSet<>());
        Location loc = block.getLocation();
        if (cooldowns.contains(loc)) { event.setCancelled(true); return; }

        event.setCancelled(true);

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(uuid);
        // Override Paper's WHEAT resend immediately so client sees chosen crop, not wheat
        player.sendBlockChange(loc, FarmingManager.getCropBlockData(profile != null ? profile.getChosenCrop() : 0));
        if (profile == null) return;

        // ── Farm multi increment (+0.001 × farmUpgradeMulti per crop) ──
        double farmUpgDouble = UpgradeManager.getFarmTotalMultiplierDouble(profile.getFarmMultiUpgradeLevel());
        profile.setFarmMulti(profile.getFarmMulti() + 0.001 * farmUpgDouble);

        // ── Seeds (cropMulti × enchantMulti) ─────────────────────
        long cropSeedMulti = FarmingManager.getSeedMultiplier(profile.getChosenCrop());
        long enchantMulti  = rollEnchants(player, profile);
        profile.addSeeds(cropSeedMulti * enchantMulti);

        // Auto-merge if enabled
        if (profile.isAutoMerge()) FarmingManager.runAutoMerge(profile);

        // ── Farming XP & level ────────────────────────────────────
        double currentXp = profile.getFarmingXp() + 1.0;
        int    oldLevel  = profile.getFarmingLevel();
        int    level     = oldLevel;
        double reqXp     = FarmingLevelManager.getRequiredXpForNextLevel(level);

        while (currentXp >= reqXp) {
            currentXp -= reqXp;
            level++;
            reqXp = FarmingLevelManager.getRequiredXpForNextLevel(level);
            player.sendMessage("§a§l[!] §7Farm Level Up! Now §e"
                    + NumberFormatter.format(new BigNumber(level)) + "§7!");
        }
        profile.setFarmingXp(currentXp);
        profile.setFarmingLevel(level);

        if (level > oldLevel) {
            plugin.getToolHandler().checkHoeTierUp(player, profile, oldLevel, level);
        }

        // ── Scoreboard refresh ────────────────────────────────────
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateScoreboard(player,
                    profile.getMoney(), profile.getGems(), profile.getRubies(),
                    profile.getFarmingLevel(), profile.getFarmingXp(),
                    profile.getMiningLevel(), profile.getMiningXp());
        }

        // ── Fake block: briefly hide then restore chosen crop ─────
        cooldowns.add(loc);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.sendBlockChange(loc, Material.AIR.createBlockData());
        }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cooldowns.remove(loc);
            if (!player.isOnline()) return;
            player.sendBlockChange(loc, FarmingManager.getCropBlockData(profile.getChosenCrop()));
        }, 60L);
    }

    // Roll enchants rarest→common; return multiplier of first proc (or 1 if none)
    private long rollEnchants(Player player, PlayerProfile profile) {
        int farmLevel = profile.getFarmingLevel();
        for (int i = FarmingManager.ENCHANT_NAMES.length - 1; i >= 0; i--) {
            double chance = FarmingManager.getEnchantChance(i, farmLevel);
            if (chance <= 0) continue;
            if (random.nextDouble() * 100.0 < chance) {
                long multi = FarmingManager.ENCHANT_SEED_MULTI[i];
                if (profile.isEnchantMessageEnabled(i)) {
                    player.sendMessage("§8[§e⚡§8] §6§l" + FarmingManager.ENCHANT_NAMES[i]
                            + " §7activated! §e+"
                            + NumberFormatter.format(new BigNumber((double) multi)) + "x §7seeds!");
                }
                return multi;
            }
        }
        return 1L;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Set<Location> cooldowns = brokenCropCache.get(player.getUniqueId());
        if (cooldowns != null && cooldowns.contains(block.getLocation())) {
            event.setCancelled(true);
            PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
            if (profile != null)
                player.sendBlockChange(block.getLocation(), FarmingManager.getCropBlockData(profile.getChosenCrop()));
        }
    }
}