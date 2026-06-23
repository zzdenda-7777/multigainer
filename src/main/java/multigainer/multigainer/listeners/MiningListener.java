package multigainer.multigainer.listeners;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.data.property.Visibility;
import de.oliver.fancyholograms.api.hologram.Hologram;
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
import org.bukkit.block.data.BlockData;
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

    private int hologramCounter = 0;

    public MiningListener(Multigainer plugin) {
        this.plugin = plugin;
    }

    /**
     * Vytvoří packet-based floating text hologram (FancyHolograms), viditelný POUZE
     * pro daného hráče, který plynule stoupá nahoru a po 1 sekundě zmizí.
     * Žádná reálná entita se na serveru nevytváří - jen packety pro daného klienta.
     */
    private void spawnPacketHologram(Player player, Location location, String legacyText) {
        if (location.getWorld() == null) return;

        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        String hologramName = "mg_temp_" + player.getUniqueId() + "_" + (hologramCounter++);

        TextHologramData data = new TextHologramData(hologramName, location);
        data.setText(List.of(legacyText));
        data.setVisibility(Visibility.MANUAL);
        data.setPersistent(false);

        Hologram hologram = manager.create(data);

        // FIX: registrovat hologram u manageru, jinak removeHologram() nemá co smazat
        manager.addHologram(hologram);

        hologram.forceShowHologram(player);

        // Rychlý "pop up" efekt: vyskočí o 0.6 bloku nahoru během 6 ticků (0.3s),
        // pak ihned zmizí - ne pomalé plynulé stoupání po celou sekundu
        final int popUpTicks = 6;
        final double popUpHeight = 0.6;
        final double stepPerTick = popUpHeight / popUpTicks;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= popUpTicks) {
                    hologram.forceHideHologram(player);
                    manager.removeHologram(hologram);
                    this.cancel();
                    return;
                }
                Location current = hologram.getData().getLocation();
                hologram.getData().setLocation(current.clone().add(0, stepPerTick, 0));
                hologram.getData().setHasChanges(true);
                hologram.forceUpdate();
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Pošle danému hráči packet, který MU SAMOTNÉMU změní vzhled bloku na bedrock,
     * aniž by se cokoliv změnilo na serveru nebo pro ostatní hráče.
     */
    private void sendFakeBlockChange(Player player, Location location, Material fakeMaterial) {
        BlockData fakeData = fakeMaterial.createBlockData();
        player.sendBlockChange(location, fakeData);
    }

    /**
     * Vrátí danému hráči packet s reálným blokem, aby se jeho klient
     * resynchronizoval se skutečným stavem na serveru.
     */
    private void revertFakeBlockChange(Player player, Block realBlock) {
        player.sendBlockChange(realBlock.getLocation(), realBlock.getBlockData());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.COBBLESTONE) return;
        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        Set<Location> cobbleCooldowns = brokenCobbleCache.computeIfAbsent(uuid, k -> new HashSet<>());
        if (cobbleCooldowns.contains(block.getLocation())) return;
        cobbleCooldowns.add(block.getLocation());

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(uuid);

        BigNumber basePayout = new BigNumber(10.0);
        BigNumber compoundingMultiplier = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
        BigNumber mineMoneyMultiplier = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());

        BigNumber rebirthMultiplier = new BigNumber(RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints()));
        BigNumber tierMultiplier = new BigNumber(TierManager.getMultiplierForTier(profile.getTier()));

        BigNumber payout = basePayout.multiply(compoundingMultiplier)
                .multiply(mineMoneyMultiplier)
                .multiply(rebirthMultiplier)
                .multiply(tierMultiplier);

        BigNumber baseGems = new BigNumber(1.0);
        BigNumber mineGemsMultiplier = MiningLevelManager.getGemsMultiplier(profile.getMiningLevel());
        BigNumber finalGems = baseGems.multiply(mineGemsMultiplier);

        profile.setMoney(profile.getMoney().add(payout));
        profile.setGems(profile.getGems().add(finalGems));

        double currentXp = profile.getMiningXp() + 1.0;
        int currentLevel = profile.getMiningLevel();
        double requiredXp = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);

        while (currentXp >= requiredXp) {
            currentXp -= requiredXp;
            currentLevel++;
            requiredXp = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);
            Location levelUpLoc = player.getLocation().clone().add(0, 2.5, 0);
            spawnPacketHologram(player, levelUpLoc, "§b§l[!] MINE LEVEL UP! §7Your Mining Level is now §e" + currentLevel + "§7!");
        }

        profile.setMiningXp(currentXp);
        profile.setMiningLevel(currentLevel);

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

        // FIX: posíláme fake block change o 1 tick později, aby ho server
        // nepřebil svým vlastním "block unchanged" resync packetem po cancelu eventu
        new BukkitRunnable() {
            @Override
            public void run() {
                sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK);
            }
        }.runTaskLater(plugin, 1L);

        Location holoLoc = block.getLocation().clone().add(0.5, 1.5, 0.5);
        spawnPacketHologram(player, holoLoc, "§a+" + NumberFormatter.format(payout) + " Money");

        new BukkitRunnable() {
            @Override
            public void run() {
                revertFakeBlockChange(player, block);
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
        UUID uuid = event.getPlayer().getUniqueId();
        brokenCobbleCache.remove(uuid);
    }
}