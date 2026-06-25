package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.levels.MiningLevelManager;
import multigainer.multigainer.tools.PickaxeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

import static multigainer.multigainer.tools.PickaxeManager.getBlockIndex;

public class MiningListener implements Listener {
    private final Multigainer plugin;
    private final Map<UUID, Set<Location>> brokenCobbleCache = new HashMap<>();
    private final Map<UUID, Boolean> isLevelingUp = new HashMap<>();

    // Title cooldown to avoid spamming the "upgrade pickaxe" title
    private final Map<UUID, Long> titleCooldown = new HashMap<>();

    public MiningListener(Multigainer plugin) {
        this.plugin = plugin;
    }

    private ItemDisplay spawnHiddenItemDisplay(Player viewer, Location location, ItemStack item) {
        ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setVisibleByDefault(false);
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.CENTER);
        });
        viewer.showEntity(plugin, display);
        return display;
    }

    private void spawnDropEffect(Player player, Location blockLocation, Material dropMaterial) {
        Location restingLoc = blockLocation.clone().add(0.5, 1.05, 0.5);

        final Transformation smallScale = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(0.4f, 0.4f, 0.4f),
                new AxisAngle4f(0, 0, 0, 1)
        );

        ItemDisplay display = restingLoc.getWorld().spawn(restingLoc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(dropMaterial));
            d.setVisibleByDefault(false);
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.FIXED);
            d.setInterpolationDuration(0);
            d.setInterpolationDelay(0);
            d.setTransformation(smallScale);
        });
        player.showEntity(plugin, display);

        display.setInterpolationDuration(4);
        display.setTeleportDuration(4);
        new BukkitRunnable() {
            @Override public void run() {
                if (display.isValid()) display.teleport(restingLoc.clone().add(0, 0.2, 0));
            }
        }.runTaskLater(plugin, 1L);

        new BukkitRunnable() {
            @Override public void run() {
                if (display.isValid()) {
                    display.setInterpolationDuration(4);
                    display.setTeleportDuration(4);
                    display.teleport(player.getEyeLocation().clone());
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                }
            }
        }.runTaskLater(plugin, 10L);

        new BukkitRunnable() {
            @Override public void run() {
                if (display.isValid()) { player.hideEntity(plugin, display); display.remove(); }
            }
        }.runTaskLater(plugin, 12L);
    }

    private void spawnLevelUpItemEffect(Player player, Location blockLocation) {
        Location spawnLoc = blockLocation.clone().add(0.5, 1.0, 0.5);
        ItemDisplay display = spawnHiddenItemDisplay(player, spawnLoc,
                new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));

        display.setInterpolationDuration(4);
        display.setBillboard(Display.Billboard.CENTER);

        final int totalTicks = 60;
        final int stepEvery = 4;

        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks >= totalTicks || !display.isValid()) {
                    player.hideEntity(plugin, display);
                    display.remove();
                    this.cancel();
                    return;
                }
                float progress = (float) ticks / totalTicks;
                Location nextTarget = spawnLoc.clone().add(0, 1.5 * progress, 0);
                display.teleport(nextTarget);

                float rotation = (float) (ticks * 0.35);
                float scale = 1.0f + progress;
                display.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(rotation, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
                ticks += stepEvery;
            }
        }.runTaskTimer(plugin, 0L, stepEvery);
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
        Material blockType = block.getType();

        // Check if the block is in our mining list
        int blockIndex = getBlockIndex(blockType);
        if (blockIndex == -1) return;

        // Only process if holding the custom pickaxe
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!plugin.getToolHandler().isCustomPickaxe(held)) return;

        event.setCancelled(true);

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        // Tier restriction check
        int minTier = PickaxeManager.getMinTierForBlock(blockIndex);

        if (profile.getPickaxeTier() < minTier) {
            // 1. Zobrazení hologramu nad blokem místo Title na obrazovce
            spawnUpgradeHologram(block.getLocation(), minTier);

            // 2. Vizuální efekt BEDROCK pro hráče
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK);
                }
            }.runTaskLater(plugin, 1L);

            // 3. Vrácení bloku po 3 sekundách (60 ticků)
            new BukkitRunnable() {
                @Override
                public void run() {
                    revertFakeBlockChange(player, block);
                }
            }.runTaskLater(plugin, 60L);

            return;
        }

        UUID uuid = player.getUniqueId();
        Set<Location> cobbleCooldowns = brokenCobbleCache.computeIfAbsent(uuid, k -> new HashSet<>());
        if (cobbleCooldowns.contains(block.getLocation())) return;
        cobbleCooldowns.add(block.getLocation());

        // Block-specific base multipliers
        double blockGemsMultiplier = getBlockGemsMultiplier(blockType);
        double blockXpMultiplier   = getBlockXpMultiplier(blockType);

        // Apply upgrade multipliers
        double gemUpgradeMulti = PickaxeManager.getGemMultiplier(profile.getGemMultiLevel());
        double xpUpgradeMulti  = PickaxeManager.getXpMultiplier(profile.getXpMultiLevel());

        BigNumber payout = new BigNumber(blockGemsMultiplier)
                .multiply(MiningLevelManager.getGemsMultiplier(profile.getMiningLevel()))
                .multiply(new BigNumber(gemUpgradeMulti));

        profile.setGems(profile.getGems().add(payout));

        // XP with upgrade multiplier
        double xpGain = blockXpMultiplier * xpUpgradeMulti;
        double currentXp = profile.getMiningXp() + xpGain;
        int currentLevel = profile.getMiningLevel();
        double requiredXp = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);
        boolean leveledUp = false;

        while (currentXp >= requiredXp) {
            currentXp -= requiredXp;
            currentLevel++;
            requiredXp = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);
            leveledUp = true;
        }
        profile.setMiningXp(currentXp);
        profile.setMiningLevel(currentLevel);

        // Increment block storage counter
        profile.incrementBlockStorage(blockIndex);

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

        // Fake bedrock visual
        new BukkitRunnable() {
            @Override public void run() { sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK); }
        }.runTaskLater(plugin, 1L);

        spawnDropEffect(player, block.getLocation(), blockType);
        // Předpokládám, že máš někde: double xpGained = ...;

        String gemsFormatted = NumberFormatter.format(payout);
        String xpFormatted = NumberFormatter.format(new BigNumber(xpGain));
        if (isLevelingUp.getOrDefault(player.getUniqueId(), false)) {
            return;
        }

        // Jinak pošli běžný ActionBar
        sendFixedActionBar(player, gemsFormatted, xpFormatted);
        sendFixedActionBar(player, gemsFormatted, xpFormatted);

        if (leveledUp) {
            spawnLevelUpItemEffect(player, block.getLocation());
            final int finalLevel = currentLevel;

            // Zamkneme ActionBar pro tohoto hráče
            isLevelingUp.put(player.getUniqueId(), true);

            new BukkitRunnable() {
                int count = 0;
                final String msg = "§7MINING LEVEL UP! YOUR LEVEL IS NOW §e" + finalLevel + "§7!";

                @Override
                public void run() {
                    // Po 1.5 sekundách (30 ticků celkem / 10 ticků interval = 3 průběhy)
                    // Tady máš count 30 (30 ticků = 1.5 sekundy při intervalu 10L)
                    if (count >= 60) {
                        isLevelingUp.put(player.getUniqueId(), false); // Odemkneme
                        this.cancel();
                        return;
                    }
                    player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(msg));
                    count += 10;
                }
            }.runTaskTimer(plugin, 0L, 10L); // Začne hned (0L)
        }

        new BukkitRunnable() {
            @Override public void run() {
                revertFakeBlockChange(player, block);
                cobbleCooldowns.remove(block.getLocation());
            }
        }.runTaskLater(plugin, 60L);
    }

    private void spawnUpgradeHologram(Location loc, int requiredTier) {
        String tierName = PickaxeManager.TIER_NAMES[requiredTier];
        String tierColor = PickaxeManager.TIER_COLORS[requiredTier];

        // Vytvoření TextDisplay entity
        Location spawnLoc = loc.clone().add(0.5, 1.5, 0.5); // Trochu nad blokem
        TextDisplay td = loc.getWorld().spawn(spawnLoc, TextDisplay.class);

        td.setText("§c§lPickaxe Required: " + tierColor + "§l" + tierName);
        td.setBillboard(Display.Billboard.CENTER); // Bude se vždy natáčet k hráči
        td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0)); // Průhledné pozadí
        td.setShadowed(true);

        // Po 2 sekundách hologram smažeme
        new BukkitRunnable() {
            @Override
            public void run() {
                td.remove();
            }
        }.runTaskLater(plugin, 40L); // 40 ticků = 2 sekundy
    }
    private void sendFixedActionBar(Player player, String gems, String xp) {
        int sideWidth = 22; // Uprav podle toho, jak moc to chceš roztáhnout

        String leftSide = String.format("%" + sideWidth + "s", "§7+ §b" + gems + " Gems");
        String rightSide = String.format("%-" + sideWidth + "s", "§7+ §a" + xp + " XP");

        player.sendActionBar(net.kyori.adventure.text.Component.text()
                .append(LegacyComponentSerializer.legacySection().deserialize(leftSide))
                .append(net.kyori.adventure.text.Component.text(" §8| "))
                .append(net.kyori.adventure.text.Component.text(rightSide))
                .font(org.bukkit.NamespacedKey.minecraft("uniform"))
                .build());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 1. Zkontroluj, zda hráč klikl na blok
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        // 2. Ochrana proti pravému kliknutí na "falešný" Bedrock
        // Pokud je blok v cache (tedy je to Bedrock), zrušíme jakýkoliv pravý klik
        if (brokenCobbleCache.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(block.getLocation())) {
            event.setCancelled(true);
            return; // Hráč nemůže nic dělat, dokud se blok neobnoví
        }

        // 3. Kontrola tieru (pouze pro levé kliknutí pro těžení)
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            int blockIndex = getBlockIndex(block.getType()); // Předpokládám, že máš metodu pro index
            int minTier = PickaxeManager.getMinTierForBlock(blockIndex);
            PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());

            if (profile.getPickaxeTier() < minTier) {
                event.setCancelled(true);

                // Zobrazení hologramu a efektu
                spawnUpgradeHologram(block.getLocation(), minTier);
                sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK);

                // Automatické vrácení bloku po 3 sekundách (pokud máš takovou logiku)
                new BukkitRunnable() {
                    @Override public void run() { revertFakeBlockChange(player, block); }
                }.runTaskLater(plugin, 60L);

                return;
            }
        }

    }
    // Tuto metodu si přidej do třídy (např. do Utils)
    public String formatStable(String input, int length) {
        if (input.length() >= length) return input;
        // Doplnění mezer zleva
        return String.format("%" + length + "s", input);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        brokenCobbleCache.remove(uuid);
        titleCooldown.remove(uuid);
    }

    // --- Block multiplier tables ---

    private double getBlockGemsMultiplier(Material m) {
        return switch (m) {
            case COBBLESTONE            -> 1.0;
            case COBBLED_DEEPSLATE      -> 2.0;
            case COPPER_ORE             -> 4.0;
            case DEEPSLATE_COPPER_ORE   -> 6.0;
            case COAL_ORE               -> 10.0;
            case DEEPSLATE_COAL_ORE     -> 15.0;
            case IRON_ORE               -> 25.0;
            case DEEPSLATE_IRON_ORE     -> 40.0;
            case REDSTONE_ORE           -> 70.0;
            case DEEPSLATE_REDSTONE_ORE -> 120.0;
            case LAPIS_ORE              -> 200.0;
            case DEEPSLATE_LAPIS_ORE    -> 450.0;
            case GOLD_ORE               -> 1000.0;
            case DEEPSLATE_GOLD_ORE     -> 2500.0;
            case DIAMOND_ORE            -> 10000.0;
            case DEEPSLATE_DIAMOND_ORE  -> 50000.0;
            case NETHERITE_BLOCK        -> 1000000.0;
            default -> 0.0;
        };
    }

    private double getBlockXpMultiplier(Material m) {
        return switch (m) {
            case COBBLESTONE            -> 1.0;
            case COBBLED_DEEPSLATE      -> 2.0;
            case COPPER_ORE             -> 3.0;
            case DEEPSLATE_COPPER_ORE   -> 4.0;
            case COAL_ORE               -> 5.0;
            case DEEPSLATE_COAL_ORE     -> 7.0;
            case IRON_ORE               -> 9.0;
            case DEEPSLATE_IRON_ORE     -> 11.0;
            case REDSTONE_ORE           -> 13.0;
            case DEEPSLATE_REDSTONE_ORE -> 15.0;
            case LAPIS_ORE              -> 18.0;
            case DEEPSLATE_LAPIS_ORE    -> 25.0;
            case GOLD_ORE               -> 35.0;
            case DEEPSLATE_GOLD_ORE     -> 50.0;
            case DIAMOND_ORE            -> 75.0;
            case DEEPSLATE_DIAMOND_ORE  -> 150.0;
            case NETHERITE_BLOCK        -> 1000.0;
            default -> 0.0;
        };
    }
}
