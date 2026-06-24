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

public class MiningListener implements Listener {
    private final Multigainer plugin;
    private final Map<UUID, Set<Location>> brokenCobbleCache = new HashMap<>();

    public MiningListener(Multigainer plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Pomocné: spawnuje entitu, kterou hned schová VŠEM kromě jednoho hráče.
    // Server entitu technicky vytvoří, ale spawn packet jde jen tomu jednomu
    // hráči - pro síť i pro ostatní hráče je to identické jako packet-only.
    // ------------------------------------------------------------------
    private ItemDisplay spawnHiddenItemDisplay(Player viewer, Location location, ItemStack item) {
        ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setVisibleByDefault(false); // nikdo ji nevidí defaultně
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.CENTER);
        });
        viewer.showEntity(plugin, display); // ukázat JEN tomuto hráči
        return display;
    }

    /**
     * Drop efekt - ItemDisplay (packet-based, žádná fyzika na serveru).
     * 1. Item vyskočí z bloku a krátce se vznáší
     * 2. Po 0.5s (10 ticků) odletí k hráči
     * 3. Po 0.6s (12 ticků) se entita VŽDY smaže (pojistka), peníze už byly
     * připsány dřív v onBlockBreak, takže animace je čistě vizuální bonus
     *
     * Billboard.FIXED - item má pevnou orientaci v prostoru, NEOTÁČÍ se za
     * hráčovou kamerou (na rozdíl @param CENTER, který by sledoval pohled hráče)
     */
    private void spawnDropEffect(Player player, Location blockLocation, Material dropMaterial) {
        Location restingLoc = blockLocation.clone().add(0.5, 1.05, 0.5);

        final Transformation smallScale = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(0.4f, 0.4f, 0.4f),
                new AxisAngle4f(0, 0, 0, 1)
        );

        // FIX: transformace (scale) se nastavuje UVNITŘ spawn callbacku, ne po
        // něm - tím je součástí prvního spawn packetu. Navíc explicitně
        // nastavíme interpolation duration na 0 PŘED transformací, aby klient
        // velikost nikdy neinterpoloval z výchozí (1:1 blok) hodnoty.
        ItemDisplay display = restingLoc.getWorld().spawn(restingLoc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(dropMaterial));
            d.setVisibleByDefault(false);
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.FIXED); // pevná orientace, nesleduje hráče
            d.setInterpolationDuration(0);
            d.setInterpolationDelay(0);
            d.setTransformation(smallScale);
        });
        player.showEntity(plugin, display);

        // Krátká pauza (1 tick), než entita "ustálí" svůj počáteční vzhled
        // u klienta, než na ní začneme dělat jakékoliv další transformace/pohyb

        // Krok 1: poskočí o kousek výš (vanilla "pop" efekt)
        display.setInterpolationDuration(4);
        display.setTeleportDuration(4);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (display.isValid()) {
                    display.teleport(restingLoc.clone().add(0, 0.2, 0));
                }
            }
        }.runTaskLater(plugin, 1L);

        // Krok 2: po 0.5s (10 ticků) odletí k hráči
        new BukkitRunnable() {
            @Override
            public void run() {
                if (display.isValid()) {
                    display.setInterpolationDuration(4);
                    display.setTeleportDuration(4);
                    display.teleport(player.getEyeLocation().clone());

                    // Přehrání zvuku sebrání itemu přímo pro hráče
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                }
            }
        }.runTaskLater(plugin, 10L);

        // Krok 3: POJISTKA - po 0.6s (12 ticků) se entita vždy smaže,
        // bez ohledu na to, jestli let k hráči stihl dospět
        new BukkitRunnable() {
            @Override
            public void run() {
                if (display.isValid()) {
                    player.hideEntity(plugin, display);
                    display.remove();
                }
            }
        }.runTaskLater(plugin, 12L);
    }

    /**
     * Level-up efekt - smithing template vyletí z bloku, otáčí se a
     * postupně se zvětšuje po dobu 3 sekund (60 ticků).
     */
    private void spawnLevelUpItemEffect(Player player, Location blockLocation) {
        Location spawnLoc = blockLocation.clone().add(0.5, 1.0, 0.5);
        ItemDisplay display = spawnHiddenItemDisplay(player, spawnLoc,
                new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));

        // Necháme klienta plynule interpolovat každý krok - update po několika
        // ticích, ne každý tick, aby se ušetřily zbytečné packety
        display.setInterpolationDuration(4);
        display.setBillboard(Display.Billboard.CENTER);

        final int totalTicks = 60; // 3 sekundy
        final int stepEvery = 4;   // update packet každé 4 ticky (ne každý tick)

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= totalTicks || !display.isValid()) {
                    player.hideEntity(plugin, display);
                    display.remove();
                    this.cancel();
                    return;
                }

                float progress = (float) ticks / totalTicks;

                // Vyletí o 1.5 bloku nahoru během celé animace
                Location current = display.getLocation();
                Location nextTarget = spawnLoc.clone().add(0, 1.5 * progress, 0);
                display.teleport(nextTarget);

                // Otáčení kolem Y osy + postupné zvětšování (1.0x -> 2.0x)
                float rotation = (float) (ticks * 0.35);
                float scale = 1.0f + progress; // 1.0 -> 2.0

                Transformation transformation = new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(rotation, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 0, 1)
                );
                display.setTransformation(transformation);

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

        // --- Kontrola bloku s odděleným násobitelem pro drahokamy a XP ---
        double blockGemsMultiplier;
        double blockXpMultiplier;

        if (blockType == Material.COBBLESTONE) {
            blockGemsMultiplier = 1.0;
            blockXpMultiplier = 1.0;
        } else if (blockType == Material.COBBLED_DEEPSLATE) {
            blockGemsMultiplier = 2.0;
            blockXpMultiplier = 2.0;
        } else if (blockType == Material.COPPER_ORE) {
            blockGemsMultiplier = 4.0;
            blockXpMultiplier = 3.0;
        } else if (blockType == Material.DEEPSLATE_COPPER_ORE) {
            blockGemsMultiplier = 6.0;
            blockXpMultiplier = 4.0;
        } else if (blockType == Material.COAL_ORE) {
            blockGemsMultiplier = 10.0;
            blockXpMultiplier = 5.0;
        } else if (blockType == Material.DEEPSLATE_COAL_ORE) {
            blockGemsMultiplier = 15.0;
            blockXpMultiplier = 7.0;
        } else if (blockType == Material.IRON_ORE) {
            blockGemsMultiplier = 25.0;
            blockXpMultiplier = 9.0;
        } else if (blockType == Material.DEEPSLATE_IRON_ORE) {
            blockGemsMultiplier = 40.0;
            blockXpMultiplier = 11.0;
        } else if (blockType == Material.REDSTONE_ORE) {
            blockGemsMultiplier = 70.0;
            blockXpMultiplier = 13.0;
        } else if (blockType == Material.DEEPSLATE_REDSTONE_ORE) {
            blockGemsMultiplier = 120.0;
            blockXpMultiplier = 15.0;
        } else if (blockType == Material.LAPIS_ORE) {
            blockGemsMultiplier = 200.0;
            blockXpMultiplier = 18.0;
        } else if (blockType == Material.DEEPSLATE_LAPIS_ORE) {
            blockGemsMultiplier = 450.0;
            blockXpMultiplier = 25.0;
        } else if (blockType == Material.GOLD_ORE) {
            blockGemsMultiplier = 1000.0;
            blockXpMultiplier = 35.0;
        } else if (blockType == Material.DEEPSLATE_GOLD_ORE) {
            blockGemsMultiplier = 2500.0;
            blockXpMultiplier = 50.0;
        } else if (blockType == Material.DIAMOND_ORE) {
            blockGemsMultiplier = 10000.0;
            blockXpMultiplier = 75.0;
        } else if (blockType == Material.DEEPSLATE_DIAMOND_ORE) {
            blockGemsMultiplier = 50000.0;
            blockXpMultiplier = 150.0;
        } else if (blockType == Material.NETHERITE_BLOCK) {
            blockGemsMultiplier = 1000000.0;
            blockXpMultiplier = 1000.0;
        } else {
            return; // Jakýkoliv jiný nezařazený blok ignorujeme
        }

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        Set<Location> cobbleCooldowns = brokenCobbleCache.computeIfAbsent(uuid, k -> new HashSet<>());
        if (cobbleCooldowns.contains(block.getLocation())) return;
        cobbleCooldowns.add(block.getLocation());

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(uuid);

        // Výpočet odměny drahokamů za použití nového blockGemsMultiplier
        BigNumber payout = new BigNumber(blockGemsMultiplier)
                .multiply(MiningLevelManager.getGemsMultiplier(profile.getMiningLevel()));

        profile.setGems(profile.getGems().add(payout));

        // Výpočet a přidání zkušeností (XP) za využití dedikovaného blockXpMultiplier
        double currentXp = profile.getMiningXp() + blockXpMultiplier;
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

        // --- Vizuální efekty ---

        // Packet-based block change, jen pro tohoto hráče (o 1 tick později,
        // aby to server nepřebil svým resync packetem po cancelu eventu)
        new BukkitRunnable() {
            @Override
            public void run() {
                sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK);
            }
        }.runTaskLater(plugin, 1L);

        // Drop efekt + action bar text, jen pro tohoto hráče
        // Vyletí přesný typ vytěženého bloku podle proměnné blockType
        spawnDropEffect(player, block.getLocation(), blockType);
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .deserialize("§7+ §b" + NumberFormatter.format(payout) + " Gems"));

        if (leveledUp) {
            spawnLevelUpItemEffect(player, block.getLocation());

            // Vytvoříme lokální kopii pro použití v inner class
            final int finalLevel = currentLevel;

            new BukkitRunnable() {
                int count = 0;
                String levelUpMsg = "§b§l[!] §7Mining Level Up! §7Your level is now §e" + finalLevel + "§7!";
                @Override
                public void run() {
                    if (count >= 30) {
                        this.cancel();
                        return;
                    }
                    player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                            .deserialize(levelUpMsg));
                    count += 10;
                }
            }.runTaskTimer(plugin, 20L, 10L);
        }

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
        Player player = event.getPlayer();
        if (block != null && brokenCobbleCache.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(block.getLocation())) {
            event.setCancelled(true);

            // FIX: znovu poslat fake bedrock packet, protože interakce s blokem
            // (např. place/use item) může vyvolat server-side resend reálného
            // stavu bloku, který by náš fake packet přebil
            sendFakeBlockChange(player, block.getLocation(), Material.BEDROCK);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        brokenCobbleCache.remove(event.getPlayer().getUniqueId());
    }
}