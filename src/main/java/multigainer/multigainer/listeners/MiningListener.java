package multigainer.multigainer.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.upgrades.UpgradeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
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

        // Prevent re-triggering if the block is already on cooldown
        if (cobbleCooldowns.contains(block.getLocation())) return;

        cobbleCooldowns.add(block.getLocation());

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(uuid);
        BigNumber finalMoneyPayout = new BigNumber(10.0).multiply(UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel()));

        profile.setMoney(profile.getMoney().add(finalMoneyPayout));
        profile.setGems(profile.getGems().add(new BigNumber(1.0)));
        plugin.getScoreboardManager().updateScoreboard(player, profile.getMoney(), profile.getGems(), profile.getRubies());

        // Visual feedback
        player.sendBlockChange(block.getLocation(), Material.BEDROCK.createBlockData());

        int entityId = (int) (Math.random() * Integer.MAX_VALUE);
        Location loc = block.getLocation().clone().add(0.5, 1.2, 0.5);

        // Spawn Armor Stand
        PacketContainer spawn = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawn.getIntegers().write(0, entityId);
        spawn.getUUIDs().write(0, UUID.randomUUID());
        spawn.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
        spawn.getDoubles().write(0, loc.getX());
        spawn.getDoubles().write(1, loc.getY());
        spawn.getDoubles().write(2, loc.getZ());
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, spawn);

        // Metadata: Invisibility (0x20) and Marker (0x10)
        PacketContainer meta = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        meta.getIntegers().write(0, entityId);
        List<WrappedWatchableObject> watchableList = new ArrayList<>();
        watchableList.add(new WrappedWatchableObject(0, (byte) 0x20));
        watchableList.add(new WrappedWatchableObject(15, (byte) 0x10));
        meta.getWatchableCollectionModifier().write(0, watchableList);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, meta);

        // Cleanup task
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks == 10) {
                    PacketContainer destroy = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                    destroy.getIntLists().write(0, Collections.singletonList(entityId));
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, destroy);
                }
                if (ticks >= 60) {
                    cobbleCooldowns.remove(block.getLocation());
                    player.sendBlockChange(block.getLocation(), Material.COBBLESTONE.createBlockData());
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
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