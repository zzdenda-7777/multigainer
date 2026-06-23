package multigainer.multigainer.commands;

import multigainer.multigainer.Multigainer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final Multigainer plugin;

    public ReloadCommand(Multigainer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only allow administrators or console to run configuration refreshes
        if (!sender.hasPermission("multigainer.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command!");
            return true;
        }

        // Handle sub-command verification logic: /multigainer reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.YELLOW + "Attempting to safely reload plugin configuration values...");

            try {
                // 1. Tell the server to read the custom config.yml file from disk again
                plugin.reloadConfig();

                // 2. Refresh any memory maps or settings inside your managers here
                // Example: plugin.getIncomeManager().loadConfigValues();

                sender.sendMessage(ChatColor.GREEN + "✔ Multigainer configurations successfully reloaded!");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "❌ Critical failure occurred while reloading configs!");
                e.printStackTrace();
            }
            return true;
        }

        // Default layout helper text if they type it wrong
        sender.sendMessage(ChatColor.GOLD + "=== Multigainer Management ===");
        sender.sendMessage(ChatColor.YELLOW + "/multigainer reload " + ChatColor.WHITE + "- Safely refreshes system files.");
        return true;
    }
}