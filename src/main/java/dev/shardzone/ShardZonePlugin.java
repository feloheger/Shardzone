package dev.shardzone;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for ShardZone. Handles enablement, disablement, and managers.
 */
public class ShardZonePlugin extends JavaPlugin {

    private ShardZoneManager manager;
    private Economy economy;
    private MeteorDetector meteorDetector;


    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Erst Economy laden, damit die anderen Systeme darauf zugreifen können
        if (!setupEconomy()) {
            getLogger().severe("Vault/Economy nicht gefunden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }



        manager = new ShardZoneManager(this);
        manager.startTask();
        meteorDetector = new MeteorDetector(this);

        // Registrierung aller Events und Listener
        getServer().getPluginManager().registerEvents(new CommandBlocker(this), this);
        getServer().getPluginManager().registerEvents(new ShardZoneListener(this), this);

        // NEU: Der zweizeilige Name-Tag-Listener (ersetzt den alten TeamTagListener)
        getServer().getPluginManager().registerEvents(new NameTagUpdateListener(this), this);

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
        if (meteorDetector != null) meteorDetector.shutdown();
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
}