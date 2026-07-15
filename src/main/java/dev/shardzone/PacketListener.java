package dev.shardzone;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEditBook;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PacketListener extends PacketListenerAbstract {

    private final BanManager banManager;
    private final Set<UUID> recentlyDetected = ConcurrentHashMap.newKeySet();

    public PacketListener(BanManager banManager) {
        super(PacketListenerPriority.HIGH);
        this.banManager = banManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.EDIT_BOOK) return;

        Player player = (Player) event.getPlayer();
        if (player == null) return;

        WrapperPlayClientEditBook wrapper = new WrapperPlayClientEditBook(event);

        // Meteor's SignEdit sends books with 0 pages or signs them immediately — vanilla never does this
        boolean signing = wrapper.readBoolean();
        int pageCount = wrapper.getPages().size();

        if (signing && pageCount == 0 && recentlyDetected.add(player.getUniqueId())) {
            banManager.handleDetection(player, "SignEdit/Book Exploit");

            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override public void run() { recentlyDetected.remove(player.getUniqueId()); }
            }, 10_000);
        }
    }
}
