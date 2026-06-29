package multigainer.multigainer.messages.MessageListener;

import multigainer.multigainer.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TeleportListener implements Listener {

    private final MessageManager messageManager;

    public TeleportListener(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        Player player = event.getPlayer();

        if (command.startsWith("/tp ")) {
            String[] args = command.split(" ");
            if (args.length >= 2) {
                String targetName = args[1];
                Player target = Bukkit.getPlayer(targetName);
                
                if (target != null) {
                    String message = messageManager.getTeleportMessage(target.getName());
                    player.sendMessage(message);
                }
            }
        }
    }
}