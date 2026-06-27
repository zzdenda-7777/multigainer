package multigainer.multigainer.commands;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.artifacts.ArtifactManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveBlocksCommand implements CommandExecutor {
    private final Multigainer plugin;

    public GiveBlocksCommand(Multigainer plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (!player.isOp()) { player.sendMessage("§cNo permission."); return true; }

        for (ArtifactManager.ArtifactRecord r : ArtifactManager.getAll()) {
            ItemStack item = ArtifactManager.buildItem(plugin, r);
            player.getInventory().addItem(item).forEach(
                    (k, v) -> player.getWorld().dropItemNaturally(player.getLocation(), v));
        }
        player.sendMessage("§a§l✦ §aGiven all 70 artifact blocks!");
        return true;
    }
}
