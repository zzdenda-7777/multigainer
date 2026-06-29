package multigainer.multigainer.ArmorStand;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.util.Transformation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArmorStandCMDS implements Listener {

    private static class TeleportData {
        Location location;
        String message;
        String displayName;

        TeleportData(Location location, String message, String displayName) {
            this.location = location;
            this.message = message;
            this.displayName = displayName;
        }
    }

    private final Map<UUID, TeleportData> teleports = new HashMap<>();

    public ArmorStandCMDS() {
        loadTeleports();
    }

    private void loadTeleports() {
        teleports.clear();
        World world = Bukkit.getWorld("voidworld");

        teleports.put(UUID.fromString("c2ea9e43-32da-428a-8efb-120c69746082"),
                new TeleportData(new Location(world, 67.5, 170.0, -64.5, 0f, 0f),
                        "§8[§a§lTELEPORT§8]§f You have been teleported to the farm!", "§e§lFARMER"));

        teleports.put(UUID.fromString("d34ae6b0-91e5-4826-a06f-eb970b278d52"),
                new TeleportData(new Location(world, 56.5, 171.0, -95.5, 90f, 0f),
                        "§8[§a§lTELEPORT§8]§f You have been teleported to the mine!", "§7§lMINES"));
    }

    public void reload() {
        loadTeleports();

        // 1. Nejdřív smažeme všechny staré TextDisplay entity v okolí
        World world = Bukkit.getWorld("voidworld");
        if (world != null) {
            for (Entity entity : world.getEntitiesByClass(TextDisplay.class)) {
                // Smažeme pouze ty, které jsou blízko našich ArmorStandů (nebo prostě všechny v tom světě)
                entity.remove();
            }
        }

        // 2. Poté vytvoříme nové
        updateArmorStandNames();
    }

    private void updateArmorStandNames() {
        World world = Bukkit.getWorld("voidworld");
        if (world == null) return;

        for (Entity entity : world.getEntities()) {
            if (entity instanceof ArmorStand) {
                ArmorStand as = (ArmorStand) entity;
                if (teleports.containsKey(as.getUniqueId())) {
                    as.setCustomNameVisible(false); // Skryjeme starý nápis s pozadím
                    // Vytvoříme novou čistou entitu
                    spawnTextDisplay(as.getLocation().add(0, 2.3, 0), teleports.get(as.getUniqueId()).displayName);
                }
            }
        }
    }

    private void spawnTextDisplay(Location loc, String text) {
        TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.setText(text);
        td.setBillboard(Display.Billboard.VERTICAL);
        td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // Průhledné pozadí
        td.setShadowed(true); // Volitelně: necháme stín textu pro čitelnost
        Transformation transformation = td.getTransformation();
        transformation.getScale().set(1.5f, 1.5f, 1.5f);
        td.setTransformation(transformation);
    }

    @EventHandler
    public void onArmorStandClick(PlayerInteractAtEntityEvent event) {
        Entity clickedEntity = event.getRightClicked();
        TeleportData data = teleports.get(clickedEntity.getUniqueId());

        if (data != null) {
            event.setCancelled(true);
            event.getPlayer().teleport(data.location);
            event.getPlayer().sendMessage(data.message);
        }
    }
}