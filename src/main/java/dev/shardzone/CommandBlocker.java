package dev.shardzone;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

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
     * Removes blocked commands from the command list sent to the player on join.
     * This prevents tab-completion of blocked commands entirely.
     *
     * @param event the command send event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandSend(final PlayerCommandSendEvent event) {
        final Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }
        final java.util.List<String> blocked = plugin.getConfig()
                .getStringList("blocked-commands");
        event.getCommands().removeIf(c -> {
            for (String b : blocked) {
                final String clean = b.startsWith("/") ? b.substring(1) : b;
                if (c.equalsIgnoreCase(clean)
                        || c.equalsIgnoreCase("bukkit:" + clean)
                        || c.equalsIgnoreCase("minecraft:" + clean)
                        || c.equalsIgnoreCase("paper:" + clean)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Removes blocked commands from tab completion for non-op players.
     *
     * @param event the tab complete event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(final TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) {
            return;
        }
        if (player.isOp()) {
            return;
        }
        final java.util.List<String> blocked = plugin.getConfig()
                .getStringList("blocked-commands");
        event.getCompletions().removeIf(c -> blocked.contains("/" + c) || blocked.contains(c));
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

        for (String b : blocked) {
            final String clean = b.startsWith("/") ? b.substring(1) : b;
            if (cmd.equals("/" + clean)
                    || cmd.equals("/bukkit:" + clean)
                    || cmd.equals("/minecraft:" + clean)) {
                event.setCancelled(true);
                final String msg = plugin.getConfig()
                        .getString("messages.blocked-command", "&cUnknown command.")
                        .replace("&", "§");
                player.sendMessage(msg);
                return;
            }
        }
    }
}