package dev.shardzone;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for ShardZone. Handles enablement, disablement, and managers.
 */
public class ShardZonePlugin extends JavaPlugin {

    private ShardZoneManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new ShardZoneManager(this);
        manager.startTask();
        getServer().getPluginManager().registerEvents(new CommandBlocker(this), this);
        getCommand("afk").setExecutor(new AfkCommand(this));  // NEU
        getServer().getPluginManager().registerEvents(new ShardZoneListener(this), this); // NEU
        getLogger().info("ShardZone enabled.");
    }

    @Override
    public void onDisable() {
        if (this.manager != null) {
            this.manager.saveAllProgress();
        }
        getLogger().info("ShardZone disabled. Progress saved.");
    }

    /**
     * Gets the active ShardZoneManager instance.
     *
     * @return the manager instance
     */
    public ShardZoneManager getManager() {
        return this.manager;
    }
}
