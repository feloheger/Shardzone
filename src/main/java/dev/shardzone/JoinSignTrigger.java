package dev.shardzone;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * On join: sends a fake OPEN_SIGN_EDITOR packet to the player.
 * Meteor Client's SignEdit module automatically responds with UPDATE_SIGN.
 * Vanilla clients do not respond — detection is clean.
 */
public class JoinSignTrigger implements Listener {

    // Players who have been sent the fake sign and we are waiting for response
    private final Set<UUID> pendingCheck = ConcurrentHashMap.newKeySet();

    // Fake sign position far underground so it can never be a real block
    private static final Vector3i FAKE_POS = new Vector3i(0, -64, 0);

    private final ShardZonePlugin plugin;
    private final BanManager banManager;

    public JoinSignTrigger(ShardZonePlugin plugin, BanManager banManager) {
        this.plugin = plugin;
        this.banManager = banManager;

        // Register packet listener for UPDATE_SIGN responses
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGH) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) return;

                Player player = (Player) event.getPlayer();
                if (player == null) return;
                if (!pendingCheck.remove(player.getUniqueId())) return;

                WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(event);
                Vector3i pos = wrapper.getBlockPosition();

                // Only flag if response matches our fake position
                if (pos.x == FAKE_POS.x && pos.y == FAKE_POS.y && pos.z == FAKE_POS.z) {
                    banManager.handleDetection(player, "SignEdit Auto-Response");
                }
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Send fake sign 2 ticks after join (let client fully load in)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            pendingCheck.add(player.getUniqueId());

            // Send OPEN_SIGN_EDITOR packet
            WrapperPlayServerOpenSignEditor packet = new WrapperPlayServerOpenSignEditor(FAKE_POS, true);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);

            // Cancel pending check after 5s — vanilla client never responds
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                pendingCheck.remove(player.getUniqueId()), 100L
            );

        }, 40L); // 2 second delay
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingCheck.remove(event.getPlayer().getUniqueId());
    }
}
