package multigainer.multigainer;

import org.bukkit.plugin.java.JavaPlugin;
import multigainer.multigainer.listeners.MiningListener;

public final class Multigainer extends JavaPlugin {

    private MiningListener miningListener;

    @Override
    public void onEnable() {
        this.miningListener = new MiningListener(this);

        getServer().getPluginManager().registerEvents(miningListener, this);

        getLogger().info("§a✓ Mining plugin s packet-based ArmorStandy úspěšně načten!");
    }

    @Override
    public void onDisable() {
        // Žádná nutná čistota - fake entity se automaticky mažou po skončení animace
        getLogger().info("§c✗ Plugin vypnut.");
    }
}