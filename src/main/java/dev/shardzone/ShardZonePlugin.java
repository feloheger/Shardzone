package dev.shardzone;

 // Import des neuen Mod-Detectors
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for ShardZone. Handles enablement, disablement, and managers.
 */
public class ShardZonePlugin extends JavaPlugin {

    private ShardZoneManager manager;
    private Economy economy; // Das ist die einzige Ökonomie-Variable, die wir brauchen!

    private static ShardZonePlugin instance;

    @Override
    public void onEnable() {
        // WICHTIG: Instanz für den statischen Zugriff (z.B. im PlayerListener) setzen!
        instance = this;

        saveDefaultConfig();

        // Erst Economy laden, damit die anderen Systeme darauf zugreifen können
        if (!setupEconomy()) {
            getLogger().severe("Vault/Economy nicht gefunden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Hilfs-Join-Event (kannst du für Debugging behalten oder löschen)
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                getLogger().info("[DEBUG] PlayerJoinEvent fired for: " + e.getPlayer().getName());
            }
        }, this);

        manager = new ShardZoneManager(this);
        manager.startTask();

        // BalTop initialisieren
        BalTop balTop = new BalTop(this, this.economy);

        // Registrierung aller Events und Listener
        getServer().getPluginManager().registerEvents(new CommandBlocker(this), this);
        getServer().getPluginManager().registerEvents(new ShardZoneListener(this), this);

        // NEU: Der zweizeilige Name-Tag-Listener (ersetzt den alten TeamTagListener)
        getServer().getPluginManager().registerEvents(new NameTagUpdateListener(this), this);

        // NEU: Registrierung des neuen Mod-Detektors (PlayerListener)
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        // Registrierung von Commands
        getCommand("afk").setExecutor(new AfkCommand(this));

        getLogger().info("ShardZone enabled.");
    }

    @Override
    public void onDisable() {
        if (this.manager != null) {
            this.manager.saveAllProgress();
        }
        getLogger().info("ShardZone disabled. Progress saved.");

        // Instanz beim Deaktivieren leeren, um Memory Leaks zu vermeiden
        instance = null;
    }

    /**
     * Gets the active ShardZoneManager instance.
     *
     * @return the manager instance
     */
    public ShardZoneManager getManager() {
        return this.manager;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static ShardZonePlugin getInstance() {
        return instance;
    }
}