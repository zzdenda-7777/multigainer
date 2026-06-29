package multigainer.multigainer.messages.MessageListener;

import multigainer.multigainer.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class UnknownCommandListener implements Listener {

    private final MessageManager messageManager;

    public UnknownCommandListener(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onUnknownCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();
        String commandName = command.split(" ")[0].replace("/", "");
        Player player = event.getPlayer();
        
        Command bukkitCommand = Bukkit.getCommandMap().getCommand(commandName);
        
        // Pokud příkaz neexistuje
        if (bukkitCommand == null) {
            String message = messageManager.getUnknownCommandMessage(command);
            player.sendMessage(message);
            event.setCancelled(true);
            return;
        }
        
        // Pokud příkaz existuje, ale hráč nemá oprávnění
        if (!hasPermission(player, bukkitCommand, commandName)) {
            String message = messageManager.getUnknownCommandMessage(command);
            player.sendMessage(message);
            event.setCancelled(true);
        }
    }

    private boolean hasPermission(Player player, Command command, String commandName) {
        // Check if player has permission for the command
        try {
            String permission = command.getPermission();
            if (permission != null && !permission.isEmpty()) {
                return player.hasPermission(permission);
            }
            
            // For OP commands, check if player is OP
            if (commandNameRequiresOP(commandName)) {
                return player.isOp();
            }
            
            return true;
        } catch (Exception e) {
            return true; // If we can't check permission, let it through
        }
    }

    private boolean commandNameRequiresOP(String commandName) {
        // Commands that require OP
        return commandName.equalsIgnoreCase("op") ||
               commandName.equalsIgnoreCase("deop") ||
               commandName.equalsIgnoreCase("stop") ||
               commandName.equalsIgnoreCase("reload") ||
               commandName.equalsIgnoreCase("holotp") ||
               commandName.equalsIgnoreCase("holoreload") ||
               commandName.equalsIgnoreCase("holocenter");
    }
}