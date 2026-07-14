package dev.shardzone;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/** Blocks certain commands from non-op players. */
public class CommandBlocker implements Listener {

    /** The plugin instance. */
    private final ShardZonePlugin plugin;

    /**
     * Creates a new CommandBlocker.
     *
     * @param plugin the plugin instance
     */
    public CommandBlocker(final ShardZonePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cancels blocked commands for non-op players.
     *
     * @param event the command preprocess event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }

        final String cmd = event.getMessage().toLowerCase().split(" ")[0];
        final java.util.List<String> blocked = plugin.getConfig()
                .getStringList("blocked-commands");

        if (blocked.contains(cmd)) {
            event.setCancelled(true);
            final String msg = plugin.getConfig()
                    .getString("messages.blocked-command", "&cUnknown command.")
                    .replace("&", "§");
            player.sendMessage(msg);
        }
    }
}