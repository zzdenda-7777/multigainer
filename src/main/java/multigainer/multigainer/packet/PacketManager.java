package multigainer.multigainer.packet;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.hologram.Hologram;
import de.oliver.fancyholograms.api.data.TextHologramData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.UUID;

public class PacketManager {
    private final Plugin plugin;

    public PacketManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Hologram showHologram(Player player, Location loc, String text) {
        String id = UUID.randomUUID().toString();
        TextHologramData data = new TextHologramData(id, loc);
        data.setText(java.util.List.of(text));

        Hologram h = FancyHologramsPlugin.get().getHologramManager().create(data);
        FancyHologramsPlugin.get().getHologramManager().addHologram(h);
        return h;
    }

    public void removeHologram(Hologram hologram) {
        if (hologram == null) return;
        FancyHologramsPlugin.get().getHologramManager().removeHologram(hologram);
    }
}