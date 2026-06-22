package multigainer.multigainer.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class MiningListener implements Listener {
    private final Plugin plugin;

    public MiningListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.COBBLESTONE) return;
        event.setCancelled(true);

        // 1. Změna bloku (paket)
        player.sendBlockChange(block.getLocation(), Material.BEDROCK.createBlockData());

        // 2. Unikátní ID pro paketovou entitu
        int entityId = (int) (Math.random() * Integer.MAX_VALUE);
        Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);

        // 3. Spawn paket
        PacketContainer spawn = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawn.getIntegers().write(0, entityId);
        spawn.getUUIDs().write(0, UUID.randomUUID());
        spawn.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
        spawn.getDoubles().write(0, loc.getX());
        spawn.getDoubles().write(1, loc.getY());
        spawn.getDoubles().write(2, loc.getZ());
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, spawn);

        // 4. Metadata paket (neviditelnost) - ID 15 je pro ArmorStand (Invisible)
        PacketContainer meta = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        meta.getIntegers().write(0, entityId);
        // Poznámka: Indexy metadat se mohou měnit podle verze Minecraftu!
        // Toto je zjednodušený příklad, v praxi potřebuješ správný DataWatcher.
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, meta);

        // 5. Animace (pouze posílání teleport paketů)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 60) {
                    // Paket na zničení entity
                    PacketContainer destroy = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                    destroy.getIntLists().write(0, java.util.Collections.singletonList(entityId));
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, destroy);

                    player.sendBlockChange(block.getLocation(), Material.COBBLESTONE.createBlockData());
                    this.cancel();
                    return;
                }

                // Teleport paket
                PacketContainer teleport = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
                teleport.getIntegers().write(0, entityId);
                teleport.getDoubles().write(0, loc.getX());
                teleport.getDoubles().write(1, loc.getY() + (ticks * 0.03));
                teleport.getDoubles().write(2, loc.getZ());
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, teleport);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}