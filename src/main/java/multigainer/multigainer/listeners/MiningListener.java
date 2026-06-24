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
import de.oliver.fancyholograms.api.data.ItemHologramData;
import multigainer.multigainer.upgrades.UpgradeManager;
import multigainer.multigainer.rebirth.RebirthManager;
import multigainer.multigainer.tier.TierManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MiningListener implements Listener {
    private final Multigainer plugin;
    private final Map<UUID, Set<Location>> brokenCobbleCache = new HashMap<>();

    public MiningListener(Multigainer plugin) {
        this.plugin = plugin;
    }

    private void spawnPacketHologram(Player player, Location location, String legacyText, boolean showItem) {
        if (location.getWorld() == null) return;
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();

        // 1. Zvýšeno na 1.6, aby byl text jasně NAD blokem
        Location centerLoc = new Location(location.getWorld(), location.getX() + 0.5, location.getY() + 1.6, location.getZ() + 0.5);
        String baseId = UUID.randomUUID().toString();

        // 1. Text hologram (otáčí se za hráčem)
        TextHologramData textData = new TextHologramData("t_" + baseId, centerLoc.clone());
        textData.setText(List.of(legacyText));
        textData.setVisibility(Visibility.MANUAL);
        textData.setPersistent(false);
        try {
            textData.setBackground(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            textData.setBillboard(Display.Billboard.VERTICAL);
        } catch (Exception ignored) {}

        Hologram textHolo = manager.create(textData);
        manager.addHologram(textHolo);
        textHolo.forceShowHologram(player);

        // 2. Diamant (umístěn těsně pod textem)
        Hologram itemHolo = null;
        if (showItem) {
            // -0.3 je menší offset (posun dolů), takže bude diamant blíž textu
            ItemHologramData itemData = new ItemHologramData("i_" + baseId, centerLoc.clone().add(0, -0.1, 0));
            itemData.setItemStack(new ItemStack(Material.DIAMOND));
            try {
                itemData.setBillboard(Display.Billboard.VERTICAL);
                itemData.setScale(new org.joml.Vector3f(0.35f, 0.35f, 0.35f));
            } catch (Exception ignored) {}
            itemData.setVisibility(Visibility.MANUAL);
            itemData.setPersistent(false);
            itemHolo = manager.create(itemData);
            manager.addHologram(itemHolo);
            itemHolo.forceShowHologram(player);
        }

        final Hologram finalItemHolo = itemHolo;
        new BukkitRunnable() {
            @Override
            public void run() {
                textHolo.forceHideHologram(player); manager.removeHologram(textHolo);
                if (finalItemHolo != null) { finalItemHolo.forceHideHologram(player); manager.removeHologram(finalItemHolo); }
            }
        }.runTaskLater(plugin, 40L);
    }

    private void spawnLevelUpHologram(Player player, Location location, int newLevel) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        // 1.6 je výška, aby byl hologram dobře vidět nad blokem
        Location centerLoc = new Location(location.getWorld(), location.getX() + 0.5, location.getY() + 1.6, location.getZ() + 0.5);
        String baseId = UUID.randomUUID().toString();

        // 1. Krumpáč se záři (Enchant)
        ItemStack itemStack = new ItemStack(Material.STONE_PICKAXE);
        // Pokud máš 1.20.6+, použij UNBREAKING, pokud starší, DURABILITY
        itemStack.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        itemStack.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        ItemHologramData itemData = new ItemHologramData("i_lvl_" + baseId, centerLoc);
        itemData.setItemStack(itemStack);
        try {
            itemData.setScale(new org.joml.Vector3f(0.6f, 0.6f, 0.6f));
            // VERTICAL zajistí otáčení za hráčem (360°)
            itemData.setBillboard(Display.Billboard.VERTICAL);
        } catch (Exception ignored) {}
        Hologram itemHolo = manager.create(itemData);
        manager.addHologram(itemHolo);
        itemHolo.forceShowHologram(player);

        // 2. Text hologram (posunutý o 0.5 nad krumpáč)
        TextHologramData textData = new TextHologramData("t_lvl_" + baseId, centerLoc.clone().add(0, 0.5, 0));
        textData.setText(List.of("§c§lLEVEL " + newLevel + "!"));
        try {
            // VERTICAL zajistí otáčení za hráčem (360°)
            textData.setBillboard(Display.Billboard.VERTICAL);
        } catch (Exception ignored) {}

        Hologram textHolo = manager.create(textData);
        manager.addHologram(textHolo);
        textHolo.forceShowHologram(player);

        // Animace barev
        new BukkitRunnable() {
            int tick = 0;
            String[] colors = {"§c", "§6", "§e", "§a", "§b", "§d", "§5"};
            @Override
            public void run() {
                if (tick >= 40) {
                    itemHolo.forceHideHologram(player); textHolo.forceHideHologram(player);
                    manager.removeHologram(itemHolo); manager.removeHologram(textHolo);
                    this.cancel(); return;
                }
                StringBuilder sb = new StringBuilder("§l");
                String text = "LEVEL " + newLevel + "!";
                for (int i = 0; i < text.length(); i++) {
                    sb.append(colors[(i + tick) % colors.length]).append(text.charAt(i));
                }
                textData.setText(List.of(sb.toString()));
                textHolo.forceUpdate();
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void sendFakeBlockChange(Player player, Location location, Material fakeMaterial) {
        player.sendBlockChange(location, fakeMaterial.createBlockData());
    }

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

        // --- Logika odměn ---
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(uuid);
        BigNumber payout = new BigNumber(10.0)
                .multiply(UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel()))
                .multiply(MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel()))
                .multiply(new BigNumber(RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints())))
                .multiply(new BigNumber(TierManager.getMultiplierForTier(profile.getTier())));

        profile.setMoney(profile.getMoney().add(payout));
        profile.setGems(profile.getGems().add(new BigNumber(1.0).multiply(MiningLevelManager.getGemsMultiplier(profile.getMiningLevel()))));

        double currentXp = profile.getMiningXp() + 1.0;
        int currentLevel = profile.getMiningLevel();
        double requiredXp = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);

        while (currentXp >= requiredXp) {
            currentXp -= requiredXp;
            currentLevel++;
            requiredXp = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);
            spawnLevelUpHologram(player, block.getLocation(), currentLevel);
        }
        profile.setMiningXp(currentXp);
        profile.setMiningLevel(currentLevel);
        // ---------------------

        // 1. Vynucení změny: Nejdříve AIR, pak BEDROCK (packet-based flow)
        player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());

        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendBlockChange(block.getLocation(), Material.BEDROCK.createBlockData());
            }
        }.runTaskLater(plugin, 1L);

        // 2. Hologram
        spawnPacketHologram(player, block.getLocation(), "§7+ §b§l" + NumberFormatter.format(payout), true);

        // 3. Po 3s (60 ticků) zpět na Cobble
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendBlockChange(block.getLocation(), Material.COBBLESTONE.createBlockData());
                cobbleCooldowns.remove(block.getLocation());
            }
        }.runTaskLater(plugin, 60L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 1. Kontrola, zda hráč kliká na blok
        Block block = event.getClickedBlock();
        if (block == null) return;

        // 2. Kontrola, zda je hráč v cooldownu
        UUID uuid = event.getPlayer().getUniqueId();
        if (brokenCobbleCache.containsKey(uuid) && brokenCobbleCache.get(uuid).contains(block.getLocation())) {

            // 3. Zrušíme ÚPLNĚ VŠECHNO (pravé i levé kliknutí)
            event.setCancelled(true);

            // 4. "Vynucení" Bedrocku znovu (pokud by klient přesto začal blikat)
            // Toto zajistí, že i když hráč klikne, Bedrock tam zůstane "přibitý"
            event.getPlayer().sendBlockChange(block.getLocation(), Material.BEDROCK.createBlockData());
            event.getPlayer().swingHand(event.getHand());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        brokenCobbleCache.remove(event.getPlayer().getUniqueId());

    }
}