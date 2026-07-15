package dev.shardzone;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Handles ban escalation for Meteor Client detections.
 * Uses LiteBans /tempban command. Escalation:
 * 1st offence → 1 min
 * 2nd offence → 7 min
 * 3rd offence → 2 hours
 * 4th offence → 12 hours
 * 5th+ offence → 24 hours
 */
public class BanManager {

    private static final String[] BAN_DURATIONS = {"1m", "7m", "2h", "12h", "24h"};
    private static final String[] BAN_DISPLAY   = {"1 Minute", "7 Minuten", "2 Stunden", "12 Stunden", "24 Stunden"};

    private final ShardZonePlugin plugin;
    private final OffenceStorage storage;

    public BanManager(ShardZonePlugin plugin, OffenceStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    /**
     * Called when a player is detected using Meteor Client.
     * Increments offence count and issues a LiteBans tempban.
     */
    public void handleDetection(Player player, String reason) {
        int offences = storage.incrementAndGet(player.getUniqueId());
        int index = Math.min(offences - 1, BAN_DURATIONS.length - 1);

        String duration = BAN_DURATIONS[index];
        String display  = BAN_DISPLAY[index];

        String banReason = "§cMeteor Client erkannt (" + reason + "). Verstoß #" + offences + ".";
        String banMessage = "§c§lGebannt für " + display + "\n§7Grund: " + banReason;

        plugin.getLogger().warning("[MeteorDetector] " + player.getName()
                + " erkannt via " + reason + " | Verstoß #" + offences + " | Ban: " + duration);

        // Kick with message first (LiteBans will also kick, this ensures immediate feedback)
        // Run on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            // LiteBans command: /tempban <player> <duration> <reason>
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "tempban " + player.getName() + " " + duration + " " + banReason
            );
        });
    }
}
