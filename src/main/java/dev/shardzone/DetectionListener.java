package dev.shardzone;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for the minecraft:brand plugin channel.
 * Meteor Client sends "meteor-client" as its brand string.
 */
public class DetectionListener implements PluginMessageListener {

    // Known Meteor brand strings (lowercase check)
    private static final Set<String> METEOR_BRANDS = Set.of(
        "meteor-client",
        "meteor client",
        "meteorclient"
    );

    // Prevent double-detection on reconnect spam
    private final Set<UUID> recentlyDetected = ConcurrentHashMap.newKeySet();

    private final BanManager banManager;

    public DetectionListener(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("minecraft:brand")) return;

        // The brand payload is a VarInt length prefix followed by the string.
        // Bukkit/Paper sends us the raw bytes; skip the first byte (VarInt for small strings).
        String brand;
        try {
            // Skip the VarInt prefix (1 byte for strings < 128 chars)
            brand = new String(message, 1, message.length - 1, StandardCharsets.UTF_8).toLowerCase().trim();
        } catch (Exception e) {
            return;
        }

        boolean isMeteor = METEOR_BRANDS.stream().anyMatch(brand::contains);

        if (isMeteor && recentlyDetected.add(player.getUniqueId())) {
            banManager.handleDetection(player, "Client Brand: " + brand);

            // Remove from set after 10s to allow re-detection if they reconnect quickly
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override public void run() { recentlyDetected.remove(player.getUniqueId()); }
            }, 10_000);
        }
    }

    public void clearPlayer(UUID uuid) {
        recentlyDetected.remove(uuid);
    }
}
