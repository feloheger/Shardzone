package dev.shardzone;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener to handle ShardZone related player events.
 */
public class ShardZoneListener implements Listener {

    private final ShardZonePlugin plugin;

    /**
     * Constructs a new ShardZoneListener.
     *
     * @param plugin the plugin instance
     */
    public ShardZoneListener(ShardZonePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the player quit event to save player progress.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.plugin.getManager().savePlayer(player.getUniqueId());
    }
}