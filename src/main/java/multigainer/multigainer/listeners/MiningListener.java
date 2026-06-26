package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.grind.GrindManager;
import multigainer.multigainer.levels.MiningLevelManager;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.perks.PerkManager;
import multigainer.multigainer.tools.PickaxeManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
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
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import org.bukkit.scheduler.BukkitTask;

public class MiningListener implements Listener {
    private final Multigainer plugin;
    private final Map<UUID, Set<Location>> brokenCobbleCache = new HashMap<>();
    private final Map<UUID, Long>          tierMsgCooldown   = new HashMap<>();
    private final Map<UUID, BukkitTask>    clearTaskMap      = new HashMap<>();
    private final Random random = new Random();

    // Player-tier requirements for each of the 17 mineable blocks (index matches PickaxeManager.BLOCKS)
    public static final int[] BLOCK_TIER_REQUIREMENTS = {
        0, 1, 3, 5, 7, 10, 12, 15, 25, 35, 45, 70, 100, 150, 200, 250, 300
    };

    public MiningListener(Multigainer plugin) { this.plugin = plugin; }

    private ItemDisplay spawnHiddenItemDisplay(Player viewer, Location location, ItemStack item) {
        World world = location.getWorld();
        if (world == null) return null;
        ItemDisplay display = world.spawn(location, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setVisibleByDefault(false);
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.CENTER);
        });
        viewer.showEntity(plugin, display);
        return display;
    }

    private void spawnDropEffect(Player player, Location blockLocation, Material dropMaterial) {
        World world = blockLocation.getWorld();
        if (world == null) return;

        Location restingLoc = blockLocation.clone().add(0.5, 1.05, 0.5);
        final Transformation smallScale = new Transformation(
                new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(0.4f, 0.4f, 0.4f), new AxisAngle4f(0, 0, 0, 1));

        ItemDisplay display = world.spawn(restingLoc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(dropMaterial));
            d.setVisibleByDefault(false);
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.FIXED);
            d.setInterpolationDuration(0);
            d.setInterpolationDelay(0);
            d.setTransformation(smallScale);
        });
        player.showEntity(plugin, display);
        display.setTeleportDuration(4);

        new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline() || !display.isValid()) { display.remove(); return; }
                display.teleport(restingLoc.clone().add(0, 0.2, 0));
            }
        }.runTaskLater(plugin, 1L);

        new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline() || !display.isValid()) { display.remove(); return; }
                display.setTeleportDuration(4);
                display.teleport(player.getEyeLocation());
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
            }
        }.runTaskLater(plugin, 10L);

        new BukkitRunnable() {
            @Override public void run() {
                if (display.isValid()) { player.hideEntity(plugin, display); display.remove(); }
            }
        }.runTaskLater(plugin, 12L);
    }

    private void spawnLevelUpItemEffect(Player player, Location blockLocation) {
        ItemDisplay display = spawnHiddenItemDisplay(player,
                blockLocation.clone().add(0.5, 1.0, 0.5),
                new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        if (display == null) return;

        display.setInterpolationDuration(4);
        display.setBillboard(Display.Billboard.CENTER);
        Location spawnLoc = blockLocation.clone().add(0.5, 1.0, 0.5);
        final int totalTicks = 60, stepEvery = 4;

        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks >= totalTicks || !display.isValid()) {
                    player.hideEntity(plugin, display); display.remove(); cancel(); return;
                }
                if (!player.isOnline()) { display.remove(); cancel(); return; }
                float progress = (float) ticks / totalTicks;
                display.teleport(spawnLoc.clone().add(0, 1.5 * progress, 0));
                display.setInterpolationDelay(-1);
                display.setInterpolationDuration(stepEvery);
                float rotation = ticks * 0.35f, scale = 1.0f + progress;
                display.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0), new AxisAngle4f(rotation, 0, 1, 0),
                        new Vector3f(scale, scale, scale), new AxisAngle4f(0, 0, 0, 1)));
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

        Block block      = event.getBlock();
        Player player    = event.getPlayer();
        Material blockType = block.getType();

        int blockIndex = PickaxeManager.getBlockIndex(blockType);
        if (blockIndex == -1) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!plugin.getToolHandler().isCustomPickaxe(held)) return;

        event.setCancelled(true);

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        // ── Pickaxe tier check (priority: show title) ─────────────────────────
        int requiredPickaxeTier = PickaxeManager.getMinTierForBlock(blockIndex);
        if (profile.getPickaxeTier() < requiredPickaxeTier) {
            String tierName  = PickaxeManager.TIER_NAMES[requiredPickaxeTier];
            String tierColor = PickaxeManager.TIER_COLORS[requiredPickaxeTier];
            long now = System.currentTimeMillis();
            if (now - tierMsgCooldown.getOrDefault(player.getUniqueId(), 0L) >= 2000L) {
                tierMsgCooldown.put(player.getUniqueId(), now);
                player.sendTitle(
                    tierColor + "§l" + tierName.toUpperCase() + " PICKAXE REQUIRED",
                    "§7Reach the " + tierColor + tierName + " §7Pickaxe to mine this block!",
                    10, 60, 15
                );
            }
            new BukkitRunnable() {
                @Override public void run() {
                    if (player.isOnline()) sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK);
                }
            }.runTaskLater(plugin, 1L);
            new BukkitRunnable() {
                @Override public void run() {
                    if (player.isOnline()) revertFakeBlockChange(player, block);
                }
            }.runTaskLater(plugin, 60L);
            return;
        }

        // ── Tier requirement check (secondary: chat message) ───────────────────
        int requiredTier = BLOCK_TIER_REQUIREMENTS[blockIndex];
        if (profile.getTier() < requiredTier) {
            long now = System.currentTimeMillis();
            if (now - tierMsgCooldown.getOrDefault(player.getUniqueId(), 0L) >= 2000L) {
                tierMsgCooldown.put(player.getUniqueId(), now);
                player.sendMessage("§c§lLOCKED §8│ §7You need §eTier " + requiredTier
                        + " §7to mine this block!");
            }
            new BukkitRunnable() {
                @Override public void run() {
                    if (player.isOnline()) sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK);
                }
            }.runTaskLater(plugin, 1L);
            new BukkitRunnable() {
                @Override public void run() {
                    if (player.isOnline()) revertFakeBlockChange(player, block);
                }
            }.runTaskLater(plugin, 60L);
            return;
        }

        UUID uuid = player.getUniqueId();
        Set<Location> cobbleCooldowns = brokenCobbleCache.computeIfAbsent(uuid, k -> new HashSet<>());
        if (cobbleCooldowns.contains(block.getLocation())) return;
        cobbleCooldowns.add(block.getLocation());

        double blockGemsMultiplier = getBlockGemsMultiplier(blockType);
        double blockXpMultiplier   = getBlockXpMultiplier(blockType);
        double gemUpgradeMulti     = PickaxeManager.getGemMultiplier(profile.getGemMultiLevel());
        double xpUpgradeMulti      = PickaxeManager.getXpMultiplier(profile.getXpMultiLevel());
        double grindGemMulti       = GrindManager.getGemMulti(profile.getGrindGemMultiLevel());

        BigNumber payout = new BigNumber(blockGemsMultiplier)
                .multiply(MiningLevelManager.getGemsMultiplier(profile.getMiningLevel()))
                .multiply(new BigNumber(gemUpgradeMulti))
                .multiply(new BigNumber(grindGemMulti));

        profile.setGems(profile.getGems().add(payout));

        // ── Mining XP (× grind XP multi) ─────────────────────────────────────
        double grindMineXpMulti = GrindManager.getMineXpMulti(profile.getGrindMineXpLevel());
        double xpGain      = blockXpMultiplier * xpUpgradeMulti * grindMineXpMulti;
        double currentXp   = profile.getMiningXp() + xpGain;
        int    currentLevel = profile.getMiningLevel();
        double requiredXp  = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);
        boolean leveledUp  = false;

        while (currentXp >= requiredXp) {
            currentXp -= requiredXp;
            currentLevel++;
            requiredXp = MiningLevelManager.getRequiredXpForNextLevel(currentLevel);
            leveledUp = true;
        }
        profile.setMiningXp(currentXp);
        profile.setMiningLevel(currentLevel);
        profile.incrementBlockStorage(blockIndex);

        // ── Grinding Points (1/500 chance, reduced by upgrade) ────────────────
        double mineDenominator = GrindManager.getMiningChanceDenominator(profile.getGrindChanceLevel());
        if (random.nextDouble() * mineDenominator < 1.0) {
            double gpEarned = GrindManager.getGPMulti(profile.getGrindGPMultiLevel());
            profile.addGrindingPoints(gpEarned);
            if (profile.isGrindMessagesEnabled()) {
                player.sendMessage("§2§l[+] §a" + NumberFormatter.format(new BigNumber(gpEarned))
                        + " §2Grinding Points §8(§7Mining§8)");
            }
        }

        // ── Perk drops (tier 5+ required) ─────────────────────────────────────
        if (profile.getTier() >= 5) {
            for (int i = 0; i < PerkManager.PERK_COUNT; i++) {
                double perkDenom = PerkManager.getPerkChanceDenominator(i, profile.getPerkChanceLevel(i));
                if (random.nextDouble() * perkDenom < 1.0) {
                    profile.incrementPerkCount(i);
                    if (profile.isPerkMessageEnabled(i)) {
                        player.sendMessage(PerkManager.PERK_COLORS[i] + "§l[✦] §7You found a "
                            + PerkManager.PERK_COLORS[i] + PerkManager.PERK_NAMES[i]
                            + " §7perk! §8(§f" + NumberFormatter.format(new BigNumber(profile.getPerkCount(i)))
                            + "§8x total)");
                    }
                }
            }
        }

        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateScoreboard(player,
                    profile.getMoney(), profile.getGems(), profile.getRubies(),
                    profile.getFarmingLevel(), profile.getFarmingXp(),
                    profile.getMiningLevel(), profile.getMiningXp());
        }

        new BukkitRunnable() {
            @Override public void run() {
                if (player.isOnline()) sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK);
            }
        }.runTaskLater(plugin, 1L);

        spawnDropEffect(player, block.getLocation(), blockType);
        // Action bar: show per-block gains; vanishes 1 second after last break
        if (leveledUp) {
            spawnLevelUpItemEffect(player, block.getLocation());
            showActionBar(player, "§b§l[!] §7Mining Level Up! Level §e" + currentLevel);
        } else {
            sendFixedMineActionBar(player,
                    NumberFormatter.format(payout),
                    NumberFormatter.format(new BigNumber(xpGain)));
        }

        new BukkitRunnable() {
            @Override public void run() {
                if (player.isOnline()) revertFakeBlockChange(player, block);
                cobbleCooldowns.remove(block.getLocation());
            }
        }.runTaskLater(plugin, 60L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block   = event.getClickedBlock();
        Player player = event.getPlayer();
        if (block != null && brokenCobbleCache.getOrDefault(player.getUniqueId(), Collections.emptySet())
                .contains(block.getLocation())) {
            event.setCancelled(true);
            sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        brokenCobbleCache.remove(uuid);
        tierMsgCooldown.remove(uuid);
        BukkitTask t = clearTaskMap.remove(uuid);
        if (t != null) t.cancel();
    }

    private void sendFixedMineActionBar(Player player, String gems, String xp) {
        int sideWidth = 22;
        String leftSide  = String.format("%" + sideWidth + "s", "§7+ §b" + gems + " Gems");
        String rightSide = String.format("%-" + sideWidth + "s", "§7+ §a" + xp + " XP");

        net.kyori.adventure.text.Component bar = net.kyori.adventure.text.Component.text()
                .font(Key.key("minecraft", "uniform"))
                .append(LegacyComponentSerializer.legacySection().deserialize(leftSide))
                .append(LegacyComponentSerializer.legacySection().deserialize(" §8| "))
                .append(LegacyComponentSerializer.legacySection().deserialize(rightSide))
                .build();
        scheduleActionBar(player, bar);
    }

    private void showActionBar(Player player, String legacyText) {
        scheduleActionBar(player, LegacyComponentSerializer.legacySection().deserialize(legacyText));
    }

    private void scheduleActionBar(Player player, net.kyori.adventure.text.Component component) {
        player.sendActionBar(component);
        BukkitTask old = clearTaskMap.put(player.getUniqueId(), null);
        if (old != null) old.cancel();
        BukkitTask clear = new BukkitRunnable() {
            @Override public void run() {
                clearTaskMap.remove(player.getUniqueId());
                if (player.isOnline())
                    player.sendActionBar(net.kyori.adventure.text.Component.empty());
            }
        }.runTaskLater(plugin, 20L);
        clearTaskMap.put(player.getUniqueId(), clear);
    }

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
