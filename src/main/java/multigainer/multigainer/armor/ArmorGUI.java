package multigainer.multigainer.armor;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;

public class ArmorGUI implements Listener {

    public static final String TITLE = "§8⚔ §5§lArmor §8⚔";
    private static final int SIZE = 54;

    private static final int RISK_LOW  = 0;
    private static final int RISK_MED  = 1;
    private static final int RISK_HIGH = 2;

    private static final int SLOT_SKIP_ANIM = 4;

    // Armor piece GUI slots
    private static final int[] PIECE_SLOTS = {10, 12, 14, 16};

    // Risk button slots per risk level × piece index
    private static final int[][] RISK_SLOTS = {
        {28, 30, 32, 34},   // low
        {37, 39, 41, 43},   // medium
        {46, 48, 50, 52}    // high
    };

    // Cumulative tick offsets for each spin frame; last entry = reveal tick
    private static final int[] FRAME_TICKS = {
        2, 4, 6, 8, 10, 12,        // fast phase  (6 frames, Δ=2)
        15, 18, 21, 24,             // medium phase (4 frames, Δ=3)
        29, 34, 39,                 // slow phase   (3 frames, Δ=5)
        47, 59,                     // very slow    (2 frames, Δ=8/12)
        63                          // REVEAL
    };

    private static final Set<UUID> animating = Collections.synchronizedSet(new HashSet<>());

    private final Multigainer plugin;
    private final Random rng = new Random();

    public ArmorGUI(Multigainer plugin) { this.plugin = plugin; }

    // ── Open ─────────────────────────────────────────────────────────────────

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        populate(inv, profile);
        player.openInventory(inv);
    }

    // ── Populate ─────────────────────────────────────────────────────────────

    private static void populate(Inventory inv, PlayerProfile profile) {
        // Fill everything with black panes first
        ItemStack black = pane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) inv.setItem(i, black);

        // Skip animation button at slot 4
        inv.setItem(SLOT_SKIP_ANIM, makeSkipAnimButton(profile));

        // Armor piece display (slots 10, 12, 14, 16)
        for (int p = 0; p < 4; p++) inv.setItem(PIECE_SLOTS[p], makePieceItem(p, profile));

        // Energy info at slot 22
        inv.setItem(22, makeEnergyItem(profile));

        // Only the 4 specific slots per risk level get colored panes
        for (int p = 0; p < 4; p++) {
            inv.setItem(RISK_SLOTS[0][p], makeRiskButton(RISK_LOW,  p, profile)); // lime:   28,30,32,34
            inv.setItem(RISK_SLOTS[1][p], makeRiskButton(RISK_MED,  p, profile)); // orange: 37,39,41,43
            inv.setItem(RISK_SLOTS[2][p], makeRiskButton(RISK_HIGH, p, profile)); // red:    46,48,50,52
        }
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private static ItemStack makeSkipAnimButton(PlayerProfile profile) {
        boolean unlocked = profile.isSkipAnimationUnlocked();
        boolean enabled  = unlocked && profile.isSkipAnimationEnabled();

        Material mat = unlocked ? (enabled ? Material.LIME_DYE : Material.GRAY_DYE) : Material.RED_DYE;
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();

        if (unlocked) {
            m.setDisplayName(enabled ? "§a§lSkip Animation §8[§aON§8]" : "§7§lSkip Animation §8[§cOFF§8]");
        } else {
            m.setDisplayName("§c§lSkip Animation §8[§cLocked§8]");
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Skip the roll animation for an instant result.");
        lore.add(" ");
        if (!unlocked) {
            lore.add("§7Cost§8: §f" + NumberFormatter.format(new BigNumber(1000)) + " §e⚡");
            lore.add("§7Energy§8: §f" + fmtE(profile.getWorkerEnergy()) + " §e⚡");
            lore.add(" ");
            lore.add("§eClick to purchase!");
        } else {
            lore.add("§7Status§8: " + (enabled ? "§aEnabled" : "§cDisabled"));
            lore.add(" ");
            lore.add("§eClick to " + (enabled ? "§cdisable" : "§aenable") + "§e!");
        }
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    private static ItemStack makeEnergyItem(PlayerProfile profile) {
        ItemStack item = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName("§e§lEnergy Balance");
        m.setLore(List.of(
            "§7Stored: §f" + fmtE(profile.getWorkerEnergy()) + " §e⚡",
            " ",
            "§8Generated by your Worker every minute."
        ));
        item.setItemMeta(m);
        return item;
    }

    private static ItemStack makePieceItem(int p, PlayerProfile profile) {
        boolean unlocked = ArmorManager.isPieceUnlocked(profile, p);
        String name = ArmorManager.PIECE_NAMES[p];
        Material mat = pieceMat(p);

        if (!unlocked) {
            // Locked display
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName("§8§l" + name + " §8[§cLocked§8]");
            List<String> lore = new ArrayList<>();
            if (p == 0) {
                lore.add("§7Reach §eTier 3 §7to unlock.");
                lore.add("§7Current tier: §f" + profile.getTier());
            } else {
                double cost = ArmorManager.UNLOCK_COSTS[p];
                lore.add("§7Cost: §f" + fmtE(cost) + " §e⚡");
                lore.add("§7Energy: §f" + fmtE(profile.getWorkerEnergy()) + " §e⚡");
                lore.add(" ");
                lore.add("§eClick to unlock!");
            }
            m.setLore(lore);
            item.setItemMeta(m);
            return item;
        }

        // Unlocked — check if rolled
        int typeOrd = profile.getArmorType(p);
        if (typeOrd < 0 || typeOrd >= ArmorType.values().length) {
            // Empty slot
            return makeLeather(mat, Color.WHITE, "§f§l" + name + " §8[§7Empty§8]",
                List.of("§7No bonus rolled yet.", " ", "§7Roll below to receive a bonus!"));
        }

        // Has a bonus
        ArmorType type = ArmorType.values()[typeOrd];
        double val = profile.getArmorValue(p);
        String valStr = type.additive ? "§f+" + fmtE(val) : "§f×" + fmtE(val);
        return makeLeather(mat, type.leatherColor,
            type.color + "§l" + name + " §8[" + type.color + type.displayName + "§8]",
            List.of(
                "§7Bonus: " + type.color + type.displayName + " " + valStr,
                " ",
                "§8Roll again to replace."
            ));
    }

    private static ItemStack makeRiskButton(int risk, int p, PlayerProfile profile) {
        boolean unlocked = ArmorManager.isPieceUnlocked(profile, p);
        String name = ArmorManager.PIECE_NAMES[p];

        Material mat;
        String riskName, col;
        long cost;
        switch (risk) {
            case RISK_LOW  -> { mat = Material.LIME_STAINED_GLASS_PANE;   riskName = "Low Risk";    col = "§a"; cost = 1L   + profile.getArmorLowBuys(); }
            case RISK_MED  -> { mat = Material.ORANGE_STAINED_GLASS_PANE; riskName = "Medium Risk"; col = "§6"; cost = 25L  + 25L * profile.getArmorMedBuys(); }
            default        -> { mat = Material.RED_STAINED_GLASS_PANE;    riskName = "High Risk";   col = "§c"; cost = 200L + 200L * profile.getArmorHighBuys(); }
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(col + "§l" + riskName + " §8▶ §f" + name);

        List<String> lore = new ArrayList<>();
        if (!unlocked) {
            lore.add("§cUnlock this armor piece first!");
        } else {
            lore.add("§7Cost: §f" + cost + " §e⚡");
            lore.add("§7Energy: §f" + fmtE(profile.getWorkerEnergy()) + " §e⚡");
            lore.add(" ");
            lore.add(col + "Ranges:");
            for (ArmorType t : ArmorType.values()) {
                double lo, hi;
                if (risk == RISK_LOW)  { lo = t.lowMin;  hi = t.lowMax;  }
                else if (risk == RISK_MED)  { lo = t.medMin;  hi = t.medMax;  }
                else                   { lo = t.highMin; hi = t.highMax; }
                String range = t.additive
                    ? "(+" + fmtE(lo) + " — +" + fmtE(hi) + ")"
                    : "(×" + fmtE(lo) + " — ×" + fmtE(hi) + ")";
                lore.add(t.color + t.displayName + " §8" + range);
            }
        }
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    // ── Click handler ─────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TITLE.equals(LegacyComponentSerializer.legacySection().serialize(event.getView().title()))) return;
        event.setCancelled(true);
        if (animating.contains(player.getUniqueId())) return;

        int raw = event.getRawSlot();
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        // Skip animation button
        if (raw == SLOT_SKIP_ANIM) { handleSkipAnimClick(player, profile); return; }

        // Piece click → unlock
        for (int p = 0; p < 4; p++) {
            if (raw == PIECE_SLOTS[p]) { handlePieceClick(player, profile, p); return; }
        }

        // Risk click
        for (int risk = 0; risk < 3; risk++) {
            for (int p = 0; p < 4; p++) {
                if (raw == RISK_SLOTS[risk][p]) { handleRiskClick(player, profile, risk, p); return; }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!TITLE.equals(LegacyComponentSerializer.legacySection().serialize(event.getView().title()))) return;

        if (animating.contains(player.getUniqueId())) {
            // Prevent closing during animation — reopen next tick
            PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
            if (profile != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) open(player, profile, plugin);
                }, 1L);
            }
            return;
        }

        animating.remove(player.getUniqueId());
    }

    // Prevent taking custom leather armor out of armor inventory slots
    @EventHandler
    public void onArmorSlotProtect(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory)) return;

        int slot = event.getSlot();
        if (slot < 36 || slot > 39) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        Material mat = item.getType();
        if (mat != Material.LEATHER_HELMET && mat != Material.LEATHER_CHESTPLATE
                && mat != Material.LEATHER_LEGGINGS && mat != Material.LEATHER_BOOTS) return;

        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        event.setCancelled(true);
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private void handleSkipAnimClick(Player player, PlayerProfile profile) {
        if (!profile.isSkipAnimationUnlocked()) {
            if (profile.getWorkerEnergy() < 250) {
                player.sendMessage("§cNeed §f250 §c⚡, you have §f" + fmtE(profile.getWorkerEnergy()) + " §c⚡.");
                return;
            }
            profile.setWorkerEnergy(profile.getWorkerEnergy() - 250);
            profile.setSkipAnimationUnlocked(true);
            profile.setSkipAnimationEnabled(true);
            player.sendMessage("§a§l✔ §aSkip Animation purchased and activated!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        } else {
            boolean newState = !profile.isSkipAnimationEnabled();
            profile.setSkipAnimationEnabled(newState);
            player.sendMessage("§7Skip Animation " + (newState ? "§a§lenabled" : "§c§ldisabled") + "§7.");
        }
        refreshInventory(player, profile);
    }

    private void handlePieceClick(Player player, PlayerProfile profile, int p) {
        if (p == 0) return; // helmet auto-unlocks via tier
        if (ArmorManager.isPieceUnlocked(profile, p)) return;

        double cost = ArmorManager.UNLOCK_COSTS[p];
        if (profile.getWorkerEnergy() < cost) {
            player.sendMessage("§cNeed §f" + fmtE(cost) + " §c⚡, you have §f" + fmtE(profile.getWorkerEnergy()) + " §c⚡.");
            return;
        }
        profile.setWorkerEnergy(profile.getWorkerEnergy() - cost);
        profile.setArmorPieceUnlocked(p, true);
        player.sendMessage("§a§l✔ §a" + ArmorManager.PIECE_NAMES[p] + " armor slot unlocked!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        equipArmor(player, profile); // give white leather immediately
        refreshInventory(player, profile);
    }

    private void handleRiskClick(Player player, PlayerProfile profile, int risk, int p) {
        if (!ArmorManager.isPieceUnlocked(profile, p)) {
            player.sendMessage("§cUnlock this armor piece first!");
            return;
        }

        long cost;
        switch (risk) {
            case RISK_LOW  -> cost = 1L   + profile.getArmorLowBuys();
            case RISK_MED  -> cost = 25L  + 25L * profile.getArmorMedBuys();
            default        -> cost = 200L + 200L * profile.getArmorHighBuys();
        }

        if (profile.getWorkerEnergy() < cost) {
            player.sendMessage("§cNeed §f" + cost + " §c⚡, you have §f" + fmtE(profile.getWorkerEnergy()) + " §c⚡.");
            return;
        }

        // Deduct cost & increment counter
        profile.setWorkerEnergy(profile.getWorkerEnergy() - cost);
        switch (risk) {
            case RISK_LOW  -> profile.setArmorLowBuys(profile.getArmorLowBuys()   + 1);
            case RISK_MED  -> profile.setArmorMedBuys(profile.getArmorMedBuys()   + 1);
            default        -> profile.setArmorHighBuys(profile.getArmorHighBuys() + 1);
        }

        // Choose winner from types not on other pieces
        ArmorType[] available = ArmorManager.getAvailableTypes(profile, p);
        if (available.length == 0) {
            player.sendMessage("§cNo types available — all are taken by other pieces!");
            return;
        }
        ArmorType winner = available[rng.nextInt(available.length)];
        double winnerValue = winner.rollValue(risk, rng);

        // Skip animation if purchased and enabled
        if (profile.isSkipAnimationUnlocked() && profile.isSkipAnimationEnabled()) {
            profile.setArmorType(p, winner.ordinal());
            profile.setArmorValue(p, winnerValue);
            equipArmor(player, profile);
            refreshInventory(player, profile);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.0f);
            String valStr = winner.additive ? "§f+" + fmtE(winnerValue) : "§f×" + fmtE(winnerValue);
            player.sendMessage("§5§l⚔ §f" + ArmorManager.PIECE_NAMES[p]
                + " §8→ " + winner.color + "§l" + winner.displayName + " " + valStr);
            return;
        }

        // Lock and animate
        animating.add(player.getUniqueId());
        runAnimation(player, profile, p, winner, winnerValue);
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    private void runAnimation(Player player, PlayerProfile profile, int pieceIndex, ArmorType winner, double winnerValue) {
        int guiSlot = PIECE_SLOTS[pieceIndex];
        Material mat = pieceMat(pieceIndex);
        ArmorType[] allTypes = ArmorType.values();
        int spinFrames = FRAME_TICKS.length - 1; // last entry is reveal tick

        // Pre-generate random frame sequence; last spin frame shows winner for "landing" feel
        int[] frameTypeIdx = new int[spinFrames];
        for (int i = 0; i < spinFrames - 1; i++) frameTypeIdx[i] = rng.nextInt(allTypes.length);
        frameTypeIdx[spinFrames - 1] = winner.ordinal(); // final spin frame = winner

        for (int frame = 0; frame < spinFrames; frame++) {
            final int f = frame;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isGUIOpen(player)) return;
                ArmorType ft = allTypes[frameTypeIdx[f]];
                // Slightly vary display to look random
                String dispName = ft.color + "§l" + ft.displayName;
                ItemStack frameItem = makeLeather(mat, ft.leatherColor, dispName, List.of("§8§o▶ Rolling..."));
                player.getOpenInventory().getTopInventory().setItem(guiSlot, frameItem);

                // Pitch: high at start (fast), lower as animation slows
                float pitch = 1.8f - (f / (float) spinFrames) * 0.9f;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, pitch);
            }, FRAME_TICKS[frame]);
        }

        // Reveal at last tick
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            animating.remove(player.getUniqueId());
            profile.setArmorType(pieceIndex, winner.ordinal());
            profile.setArmorValue(pieceIndex, winnerValue);

            if (!isGUIOpen(player)) return;

            equipArmor(player, profile); // put colored leather in actual inventory slot
            refreshInventory(player, profile);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.0f);

            String valStr = winner.additive ? "§f+" + fmtE(winnerValue) : "§f×" + fmtE(winnerValue);
            player.sendMessage("§5§l⚔ §f" + ArmorManager.PIECE_NAMES[pieceIndex]
                + " §8→ " + winner.color + "§l" + winner.displayName + " " + valStr);
        }, FRAME_TICKS[FRAME_TICKS.length - 1]);
    }

    // ── Armor equipping ───────────────────────────────────────────────────────

    /** Equips colored leather armor in actual inventory slots for all unlocked pieces. */
    public static void equipArmor(Player player, PlayerProfile profile) {
        for (int p = 0; p < 4; p++) {
            if (!ArmorManager.isPieceUnlocked(profile, p)) continue;
            int typeOrd = profile.getArmorType(p);
            ItemStack armorItem;
            if (typeOrd < 0 || typeOrd >= ArmorType.values().length) {
                // Unlocked but no type yet — white leather
                armorItem = makeLeather(pieceMat(p), Color.WHITE,
                    "§f§l" + ArmorManager.PIECE_NAMES[p],
                    List.of("§7No bonus rolled yet.", "§8Use /armor to roll!"));
            } else {
                ArmorType type = ArmorType.values()[typeOrd];
                double val = profile.getArmorValue(p);
                String valStr = type.additive ? "§f+" + fmtE(val) : "§f×" + fmtE(val);
                armorItem = makeLeather(pieceMat(p), type.leatherColor,
                    type.color + "§l" + ArmorManager.PIECE_NAMES[p] + " §8[" + type.color + type.displayName + "§8]",
                    List.of("§7Bonus: " + type.color + type.displayName + " " + valStr));
            }
            switch (p) {
                case 0 -> player.getInventory().setHelmet(armorItem);
                case 1 -> player.getInventory().setChestplate(armorItem);
                case 2 -> player.getInventory().setLeggings(armorItem);
                case 3 -> player.getInventory().setBoots(armorItem);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void refreshInventory(Player player, PlayerProfile profile) {
        populate(player.getOpenInventory().getTopInventory(), profile);
    }

    private static boolean isGUIOpen(Player player) {
        if (!player.isOnline()) return false;
        try {
            return TITLE.equals(LegacyComponentSerializer.legacySection().serialize(
                player.getOpenInventory().title()));
        } catch (Exception e) { return false; }
    }

    private static Material pieceMat(int p) {
        return switch (p) {
            case 0 -> Material.LEATHER_HELMET;
            case 1 -> Material.LEATHER_CHESTPLATE;
            case 2 -> Material.LEATHER_LEGGINGS;
            default -> Material.LEATHER_BOOTS;
        };
    }

    private static ItemStack makeLeather(Material mat, Color color, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta m = (LeatherArmorMeta) item.getItemMeta();
        m.setColor(color);
        m.setDisplayName(displayName);
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    private static ItemStack pane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(name);
        item.setItemMeta(m);
        return item;
    }

    static String fmtE(double v) {
        if (v >= 1_000_000) return NumberFormatter.format(new BigNumber(v));
        if (v >= 1_000)     return String.format("%.1f", v / 1_000.0).replaceAll("\\.0$", "") + "K";
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
