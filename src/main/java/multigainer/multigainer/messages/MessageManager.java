package multigainer.multigainer.messages;

import multigainer.multigainer.Multigainer;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

public class MessageManager {

    private final Multigainer plugin;
    private final AtomicInteger playerJoinCounter;

    public MessageManager(Multigainer plugin) {
        this.plugin = plugin;
        this.playerJoinCounter = new AtomicInteger(0);
    }

    public String getJoinMessage(Player player, boolean isNewPlayer) {
        String playerName = player.getName();
        
        if (isNewPlayer) {
            int joinNumber = playerJoinCounter.incrementAndGet();
            return "§8[§eNEW §6PLAYER§l§8] §7Welcome to the server " + playerName + "! #welcome" + joinNumber;
        } else {
            return "§8[§eM§6G§l§8] §7" + playerName + " joined the game";
        }
    }

    public String getLeaveMessage(Player player) {
        String playerName = player.getName();
        return "§8[§eM§6G§l§8] §7" + playerName + " left the game";
    }

    public String getGameModeMessage(String gameMode) {
        return "§8[§eM§6G§l§8] §7Set own game mode to " + gameMode;
    }

    public String getTeleportMessage(String targetPlayerName) {
        return "§8[§eM§6G§l§8]§7 You have been teleported to " + targetPlayerName;
    }

    public String getUnknownCommandMessage(String command) {
        return "§8[§eM§6G§l§8]§7 Unknown command: " + command;
    }

    public int getPlayerJoinCounter() {
        return playerJoinCounter.get();
    }
}