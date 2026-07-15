package dev.shardzone;

import com.github.retrooper.packetevents.PacketEvents;

public class MeteorDetector {

    private final DetectionListener brandListener;

    public MeteorDetector(ShardZonePlugin plugin) {
        OffenceStorage storage = new OffenceStorage(plugin);
        BanManager banManager = new BanManager(plugin, storage);

        // PacketEvents is already initialized by the server plugin — just register listener
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener(banManager));

        // Brand channel listener
        brandListener = new DetectionListener(banManager);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(
            plugin, "minecraft:brand", brandListener
        );

        // Join trigger: send fake sign on join to catch Meteor's SignEdit
        plugin.getServer().getPluginManager().registerEvents(
            new JoinSignTrigger(plugin, banManager), plugin
        );
    }

    public void shutdown() {
        // No terminate — PacketEvents server plugin handles its own lifecycle
    }
}
