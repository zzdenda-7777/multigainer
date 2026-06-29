package multigainer.multigainer.messages.MessageListener;

import multigainer.multigainer.messages.MessageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class GameModeListener implements Listener {

    private final MessageManager messageManager;

    public GameModeListener(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        String gameMode = event.getNewGameMode().name().toLowerCase();
        String message = messageManager.getGameModeMessage(gameMode);
        event.getPlayer().sendMessage(message);
    }
}