package multigainer.multigainer.messages.MessageListener;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerDataManager;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.messages.MessageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class JoinListener implements Listener {

    private final Multigainer plugin;
    private final PlayerDataManager dataManager;
    private final MessageManager messageManager;

    public JoinListener(Multigainer plugin, PlayerDataManager dataManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = messageManager;
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
        
        String leaveMessage = messageManager.getLeaveMessage(event.getPlayer());
        event.setQuitMessage(leaveMessage);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerProfile profile = dataManager.getProfile(event.getPlayer().getUniqueId());
        boolean isNewPlayer = (profile == null || (profile.getTier() == 0 && profile.getMoney().toDouble() == 0));
        
        String joinMessage = messageManager.getJoinMessage(event.getPlayer(), isNewPlayer);
        event.setJoinMessage(joinMessage);
    }
}