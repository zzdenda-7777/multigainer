package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class JoinListener implements Listener {

    private final Multigainer plugin;
    private final PlayerDataManager dataManager;

    public JoinListener(Multigainer plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    /**
     * Runs completely off the main thread.
     * Perfect for fetching database records before the player spawns.
     */

    @EventHandler
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String username = event.getName();

        try {
            // Corrected to lowercase 'l' to match standard Java method signatures
            dataManager.loadProfileAsync(uuid, username).join();
        } catch (Exception e) {
            plugin.getLogger().severe("Critical data loading error for player " + username);
            e.printStackTrace();

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    org.bukkit.ChatColor.RED + "Your player data failed to load. Please try reconnecting!");
        }
    }

    /**
     * Fires when a player disconnects. Removes them from the active map cache and flushes to DB.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        dataManager.handleQuit(uuid);
    }
}
