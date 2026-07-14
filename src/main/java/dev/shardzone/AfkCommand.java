package dev.shardzone;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/** Handles the /afk command - teleports the player to the AFK location after a countdown. */
public class AfkCommand implements CommandExecutor {

    /** The plugin instance. */
    private final ShardZonePlugin plugin;

    /**
     * Creates a new AfkCommand.
     *
     * @param plugin the plugin instance
     */
    public AfkCommand(final ShardZonePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the /afk command.
     *
     * @param sender  the command sender
     * @param command the command
     * @param label   the label used
     * @param args    the arguments
     * @return true always
     */
    @Override
    public boolean onCommand(
            final CommandSender sender,
            final Command command,
            final String label,
            final String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        final Location fromLoc = player.getLocation().clone();
        final String msgWait = plugin.getConfig()
                .getString("messages.afk-wait", "&eTeleporting in %seconds%s...")
                .replace("&", "§");
        final String msgMoved = plugin.getConfig()
                .getString("messages.afk-moved", "&cYou moved! Teleport cancelled.")
                .replace("&", "§");
        final String msgDone = plugin.getConfig()
                .getString("messages.afk", "&7Teleporting to AFK area...")
                .replace("&", "§");

        new BukkitRunnable() {
            private int countdown = 3;

            @Override
            public void run() {
                if (player.getLocation().distanceSquared(fromLoc) > 0.5) {
                    player.sendMessage(msgMoved);
                    cancel();
                    return;
                }

                if (countdown > 0) {
                    sendActionBar(player, msgWait.replace("%seconds%", String.valueOf(countdown)));
                    countdown--;
                } else {
                    final String raw = plugin.getConfig()
                            .getString("afk-location", "0,64,0,0,0,world");
                    final String[] parts = raw.split(",");
                    final double x = Double.parseDouble(parts[0]);
                    final double y = Double.parseDouble(parts[1]);
                    final double z = Double.parseDouble(parts[2]);
                    final float yaw = Float.parseFloat(parts[3]);
                    final float pitch = Float.parseFloat(parts[4]);
                    final String world = parts[5];

                    final Location loc = new Location(
                            plugin.getServer().getWorld(world), x, y, z, yaw, pitch);
                    player.teleport(loc);
                    player.sendMessage(msgDone);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    /**
     * Sends an action bar message to the player.
     *
     * @param player  the player
     * @param message the message
     */
    private void sendActionBar(final Player player, final String message) {
        try {
            final java.lang.reflect.Method method =
                    player.getClass().getMethod("sendActionBar", String.class);
            method.invoke(player, message);
        } catch (Exception e) {
            player.sendMessage(message);
        }
    }
}