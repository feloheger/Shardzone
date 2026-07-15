package dev.shardzone.moddeteckt;


import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import dev.shardzone.ShardZonePlugin;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {
    private static final class PendingCheck {

        private final String id;
        private final String modName;
        private final String translationKey;
        private final String fallback;


        public PendingCheck(
                String id,
                String modName,
                String translationKey,
                String fallback
        ) {
            this.id = id;
            this.modName = modName;
            this.translationKey = translationKey;
            this.fallback = fallback;
        }


        public String getId() {
            return id;
        }


        public String getModName() {
            return modName;
        }


        public String getTranslationKey() {
            return translationKey;
        }


        public String getFallback() {
            return fallback;
        }
    }
    private static final Map<UUID, List<PendingCheck>> pendingChecks
            = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> pendingTimeouts = new ConcurrentHashMap<>();
    private static final Map<UUID, Queue<List<PendingCheck>>> pendingQueue =
            new ConcurrentHashMap<>();
    private static final Map<UUID, BlockData> oldBlocks =
            new ConcurrentHashMap<>();
    private static final Map<UUID, Vector3i> fakeSignPositions =
            new ConcurrentHashMap<>();

    public PlayerListener() {
        registerPacketListener();
    }

    private void registerPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new com.github.retrooper.packetevents.event.SimplePacketListenerAbstract() {
            @Override
            public void onPacketPlayReceive(PacketPlayReceiveEvent event) {

                if (event.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) {
                    return;
                }

                Player player = Bukkit.getPlayer(event.getUser().getUUID());

                if (player == null) {
                    return;
                }

                WrapperPlayClientUpdateSign packet =
                        new WrapperPlayClientUpdateSign(event);

                String[] lines = packet.getTextLines();

                if (lines == null || lines.length == 0) {
                    return;
                }

                UUID uuid = player.getUniqueId();

                List<PendingCheck> checks =
                        pendingChecks.get(uuid);


                if(checks == null || checks.isEmpty()) {

                    return;
                }

                for(String line : lines) {


                    if(line == null) {
                        continue;
                    }


                    String clean = normalize(line);



                    for(PendingCheck check : checks) {

                        if(clean.equals(normalize(check.getTranslationKey()))
                                || clean.equals(normalize(check.getFallback()))) {


                            Bukkit.getScheduler().runTask(
                                    ShardZonePlugin.getInstance(),
                                    () -> fail(
                                            player,
                                            check.getModName()
                                    )
                            );

                            return;
                        }
                    }
                }


                Queue<List<PendingCheck>> queue =
                        pendingQueue.get(uuid);

                if (queue == null) {
                    return;
                }

                List<PendingCheck> next =
                        queue.poll();

                if (next != null) {

                    Bukkit.getScheduler().runTask(
                            ShardZonePlugin.getInstance(),
                            () -> sendModCheck(player, next)
                    );

                } else {

                    pendingQueue.remove(uuid);

                }
            }
        });
    }
    private String normalize(String text) {

        if(text == null) {
            return "";
        }


        return text
                .replace("§r", "")
                .replace("\"", "")
                .trim()
                .toLowerCase();
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if(FloodgateApi.getInstance().isFloodgatePlayer(uuid)) {
            return;
        }
        if(pendingQueue.containsKey(uuid)) {
            return;
        }

        ConfigurationSection mods = ShardZonePlugin.getInstance()
                .getConfig()
                .getConfigurationSection("disallowed-mods");

        if (mods == null) {
            return;
        }

        Queue<List<PendingCheck>> queue =
                new LinkedList<>();
        List<PendingCheck> checks =
                new ArrayList<>();
        for (String id : mods.getKeys(false)) {


            ConfigurationSection section =
                    mods.getConfigurationSection(id);


            if(section == null) {
                continue;
            }


            String modName =
                    section.getString(
                            "name",
                            id
                    );


            String translation =
                    section.getString(
                            "translation-key"
                    );


            String fallback =
                    section.getString(
                            "fallback"
                    );


            if(translation == null || fallback == null) {
                continue;
            }


            checks.add(
                    new PendingCheck(
                            id,
                            modName,
                            translation,
                            fallback
                    )
            );
        }
        if (checks.isEmpty()) {
            return;
        }

        for(int i = 0; i < checks.size(); i += 4) {


            int end =
                    Math.min(
                            i + 4,
                            checks.size()
                    );


            queue.add(
                    new ArrayList<>(
                            checks.subList(
                                    i,
                                    end
                            )
                    )
            );
        }
        if (queue.isEmpty()) {
            return;
        }

        pendingQueue.put(uuid, queue);

        List<PendingCheck> first =
                queue.poll();

        if (first != null) {

            Bukkit.getScheduler().runTaskLater(
                    ShardZonePlugin.getInstance(),
                    () -> sendModCheck(player, first),
                    20L
            );

        }
    }

    private void sendModCheck(
            Player player,
            List<PendingCheck> checks
    ) {

        if(checks == null || checks.isEmpty()) {
            Bukkit.getLogger().warning(
                    "[ModCheck] Keine Checks zum Senden"
            );
            return;
        }


        Bukkit.getLogger().info("[ModCheck] Starte ModCheck für " + player.getName());

        UUID uuid = player.getUniqueId();

        pendingChecks.put(uuid, checks);
        Bukkit.getLogger().info("[ModCheck] PendingCheck gespeichert: " + uuid);


        Location eye = player.getEyeLocation();

        Bukkit.getLogger().info(
                "[ModCheck] Eye Location: " +
                        eye.getX() + " " +
                        eye.getY() + " " +
                        eye.getZ()
        );


        Location signLocation = eye.clone().add(
                eye.getDirection().normalize().multiply(2)
        );
        Block block =
                signLocation.getBlock();


        oldBlocks.put(
                uuid,
                block.getBlockData()
        );

        Bukkit.getLogger().info(
                "[ModCheck] Schild Position berechnet: " +
                        signLocation.getBlockX() + " " +
                        signLocation.getBlockY() + " " +
                        signLocation.getBlockZ()
        );


        Vector3i pos = new Vector3i(
                signLocation.getBlockX(),
                Math.max(signLocation.getBlockY(), 1),
                signLocation.getBlockZ()
        );
        fakeSignPositions.put(
                uuid,
                pos
        );

        BlockData data = Bukkit.createBlockData(
                "minecraft:oak_wall_sign[facing=north]"
        );

        WrappedBlockState state =
                SpigotConversionUtil.fromBukkitBlockData(data);

        int id = state.getGlobalId();

        Bukkit.getLogger().info(
                "[ModCheck] BlockState ID = " + id
        );

        WrapperPlayServerBlockChange packet =
                new WrapperPlayServerBlockChange(
                        pos,
                        id
                );




        Bukkit.getLogger().info(
                "[ModCheck] BlockChange Packet erstellt"
        );


        /*
         * Sign NBT
         */







        NBTList<NBTCompound> messages =
                new NBTList<>(NBTType.COMPOUND);



        int line = 0;


        for(PendingCheck check : checks) {


            if(line >= 4) {
                break;
            }


            NBTCompound component =
                    new NBTCompound();



            component.setTag(
                    "translate",
                    new NBTString(
                            check.getTranslationKey()
                    )
            );


            component.setTag(
                    "fallback",
                    new NBTString(
                            check.getFallback()
                    )
            );


            messages.addTag(component);


            line++;
        }



        while(line < 4) {


            messages.addTag(
                    new NBTCompound()
            );


            line++;
        }


        Bukkit.getLogger().info(
                "[ModCheck] Messages NBT erstellt"
        );


        NBTCompound frontText = new NBTCompound();

        frontText.setTag(
                "messages",
                messages
        );

        frontText.setTag(
                "color",
                new NBTString("black")
        );

        frontText.setTag(
                "has_glowing_text",
                new NBTByte((byte) 0)
        );


        Bukkit.getLogger().info(
                "[ModCheck] FrontText erstellt"
        );


        NBTCompound signNBT = new NBTCompound();

        signNBT.setTag(
                "front_text",
                frontText
        );
        NBTCompound backText = new NBTCompound();


        backText.setTag(
                "messages",
                messages
        );


        backText.setTag(
                "color",
                new NBTString("black")
        );


        backText.setTag(
                "has_glowing_text",
                new NBTByte((byte) 0)
        );


        signNBT.setTag(
                "back_text",
                backText
        );

        signNBT.setTag(
                "is_waxed",
                new NBTByte((byte) 0)
        );


        Bukkit.getLogger().info(
                "[ModCheck] SignNBT fertig"
        );


        WrapperPlayServerBlockEntityData blockEntityData =
                new WrapperPlayServerBlockEntityData(
                        pos,
                        BlockEntityTypes.SIGN,
                        signNBT
                );


        Bukkit.getLogger().info(
                "[ModCheck] BlockEntityData Packet erstellt"
        );


        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor openEditor =
                new WrapperPlayServerOpenSignEditor(
                        pos,
                        true
                );


        Bukkit.getLogger().info(
                "[ModCheck] OpenSignEditor Packet erstellt"
        );


        var manager =
                PacketEvents.getAPI().getPlayerManager();


        /*
         * Block senden
         */

        Bukkit.getLogger().info(
                "[ModCheck] Sende BlockChange Packet"
        );

        manager.sendPacket(
                player,
                packet
        );


        /*
         * BlockEntity senden
         */

        Bukkit.getScheduler().runTaskLater(
                ShardZonePlugin.getInstance(),
                () -> {

                    Bukkit.getLogger().info(
                            "[ModCheck] Sende Mods:"
                    );

                    for(PendingCheck check : checks) {

                        Bukkit.getLogger().info(
                                "- "
                                        + check.getModName()
                                        + " | "
                                        + check.getTranslationKey()
                                        + " | "
                                        + check.getFallback()
                        );
                    }

                    manager.sendPacket(
                            player,
                            blockEntityData
                    );

                },
                1L
        );


        /*
         * Editor öffnen
         */

        Bukkit.getScheduler().runTaskLater(
                ShardZonePlugin.getInstance(),
                () -> {

                    Bukkit.getLogger().info(
                            "[ModCheck] Öffne Sign Editor"
                    );

                    manager.sendPacket(
                            player,
                            openEditor
                    );

                },
                3L
        );


        /*
         * Timeout
         */

        BukkitTask timeout =
                Bukkit.getScheduler().runTaskLater(
                        ShardZonePlugin.getInstance(),
                        () -> {

                            Bukkit.getLogger().info(
                                    "[ModCheck] Timeout für " + player.getName()
                            );
                            resetFakeSign(player);


                            pendingChecks.remove(uuid);
                            pendingTimeouts.remove(uuid);


                            Queue<List<PendingCheck>> queue =
                                    new LinkedList<>();
                            pendingQueue.put(uuid, queue);

                            if (queue == null) {

                                Bukkit.getLogger().info(
                                        "[ModCheck] Keine Queue vorhanden"
                                );

                                return;
                            }


                            List<PendingCheck> next =
                                    queue.poll();


                            if (next != null) {

                                Bukkit.getLogger().info(
                                        "[ModCheck] Starte nächsten Check"
                                );

                                sendModCheck(
                                        player,
                                        next
                                );

                            } else {

                                Bukkit.getLogger().info(
                                        "[ModCheck] Queue leer"
                                );

                                pendingQueue.remove(uuid);
                            }

                        },
                        300L
                );


        pendingTimeouts.put(
                uuid,
                timeout
        );


        Bukkit.getLogger().info(
                "[ModCheck] Methode erfolgreich beendet"
        );
    }

    private void fail(Player player, String modName) {

        resetFakeSign(player);
        UUID uuid = player.getUniqueId();


        pendingChecks.remove(uuid);


        BukkitTask task =
                pendingTimeouts.remove(uuid);


        if(task != null) {

            task.cancel();
        }


        pendingQueue.remove(uuid);



        ConfigurationSection action =
                ShardZonePlugin.getInstance()
                        .getConfig()
                        .getConfigurationSection("action");

        if (action == null) {
            return;
        }

        String type = action.getString("type", "kick").toLowerCase();
        List<String> content = action.getStringList("content");

        if (content.isEmpty()) {
            content = List.of("&cForbidden client modification detected (%mod%)");
        }

        if (type.equals("command")) {

            for (String cmd : content) {

                cmd = cmd
                        .replace("%player%", player.getName())
                        .replace("%mod%", modName);

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }

            return;
        }

        Component message = Component.empty();

        for (int i = 0; i < content.size(); i++) {

            message = message.append(
                    LegacyComponentSerializer
                            .legacyAmpersand()
                            .deserialize(
                                    content.get(i)
                                            .replace("%player%", player.getName())
                                            .replace("%mod%", modName)
                            )
            );

            if (i + 1 < content.size()) {
                message = message.append(Component.newline());
            }
        }

        player.kick(message);
    }
    private void resetFakeSign(Player player) {

        UUID uuid = player.getUniqueId();


        Vector3i pos =
                fakeSignPositions.remove(uuid);


        BlockData old =
                oldBlocks.remove(uuid);


        if(pos == null || old == null) {
            return;
        }


        WrappedBlockState state =
                SpigotConversionUtil.fromBukkitBlockData(old);


        int id =
                state.getGlobalId();



        WrapperPlayServerBlockChange packet =
                new WrapperPlayServerBlockChange(
                        pos,
                        id
                );


        PacketEvents.getAPI()
                .getPlayerManager()
                .sendPacket(
                        player,
                        packet
                );
    }
}