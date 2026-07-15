package dev.shardzone;


import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import dev.shardzone.ShardZonePlugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private final Map<UUID, String> pendingMod = new ConcurrentHashMap<>();
    private final Map<UUID, List<Map.Entry<String, String>>> pendingQueue = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> pendingTimeouts = new ConcurrentHashMap<>();

    public PlayerListener() {
        registerPacketListener();
    }

    private void registerPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new com.github.retrooper.packetevents.event.SimplePacketListenerAbstract() {
            @Override
            public void onPacketPlayReceive(com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
                    Player player = (Player) event.getPlayer();
                    if (player == null) return;

                    com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign packet =
                            new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign(event);

                    String[] lines = packet.getTextLines();
                    if (lines == null || lines.length == 0) return;

                    UUID uuid = player.getUniqueId();
                    String expectedFallback = pendingMod.get(uuid);

                    if (expectedFallback != null) {
                        String receivedText = lines[0];

                        // Wenn der empfangene Text NICHT der erwartete Fallback ist -> Mod detected!
                        if (!receivedText.equals(expectedFallback)) {
                            // Im originalen Bytecode wird der Mod-Name dynamisch aus dem Check übergeben
                            String modName = "Meteor Client";
                            Bukkit.getScheduler().runTask(ShardZonePlugin.getInstance(), () -> fail(player, modName));
                        }

                        pendingMod.remove(uuid);
                        BukkitTask task = pendingTimeouts.remove(uuid);
                        if (task != null) {
                            task.cancel();
                        }

                        List<Map.Entry<String, String>> queue = pendingQueue.get(uuid);
                        if (queue != null && !queue.isEmpty()) {
                            Map.Entry<String, String> nextCheck = queue.remove(0);
                            sendModCheck(player, nextCheck.getKey(), nextCheck.getValue());
                        } else {
                            pendingQueue.remove(uuid);
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    public void on(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().startsWith("*") || FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            return;
        }

        ConfigurationSection disallowedSection = ShardZonePlugin.getInstance().getConfig().getConfigurationSection("disallowed-mods");
        if (disallowedSection == null) return;

        List<Map.Entry<String, String>> checks = new ArrayList<>();
        for (Map.Entry<String, Object> entry : disallowedSection.getValues(false).entrySet()) {
            String modName = entry.getKey();
            String translationKey = disallowedSection.getString(modName + ".translation-key");
            String fallback = disallowedSection.getString(modName + ".fallback", "fallback");
            if (translationKey != null) {
                checks.add(new AbstractMap.SimpleEntry<>(translationKey, fallback));
            }
        }

        if (!checks.isEmpty()) {
            UUID uuid = player.getUniqueId();
            Map.Entry<String, String> firstCheck = checks.remove(0);
            if (!checks.isEmpty()) {
                pendingQueue.put(uuid, checks);
            }
            sendModCheck(player, firstCheck.getKey(), firstCheck.getValue());
        }
    }

    private void sendModCheck(Player player, String translationKey, String fallbackText) {
        UUID uuid = player.getUniqueId();
        pendingMod.put(uuid, fallbackText);

        Location loc = player.getLocation().clone();
        loc.setY(loc.getY() - 5);
        Vector3i peLocation = new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        // ID 31105 (Beispiel für moderne Versionen ab 1.20, ggf. an deine Serverversion anpassen)
        WrappedBlockState oakSign = WrappedBlockState.getByGlobalId(31105);
        WrapperPlayServerBlockChange blockChange = new WrapperPlayServerBlockChange(peLocation, oakSign.getGlobalId());

        NBTCompound signNBT = new NBTCompound();
        NBTCompound translationMessage = new NBTCompound();
        translationMessage.setTag("translate", new NBTString(translationKey));
        translationMessage.setTag("fallback", new NBTString(fallbackText));

        NBTList<NBTCompound> messagesList = new NBTList<>(NBTType.COMPOUND);
        messagesList.addTag(translationMessage);
        messagesList.addTag(new NBTCompound());
        messagesList.addTag(new NBTCompound());
        messagesList.addTag(new NBTCompound());

        NBTCompound frontText = new NBTCompound();
        frontText.setTag("messages", messagesList);
        frontText.setTag("color", new NBTString("black"));
        frontText.setTag("has_glowing_text", new NBTByte((byte) 0));

        signNBT.setTag("front_text", frontText);
        signNBT.setTag("is_waxed", new NBTByte((byte) 0));

        WrapperPlayServerBlockEntityData blockEntityData = new WrapperPlayServerBlockEntityData(
                peLocation, BlockEntityTypes.SIGN, signNBT
        );

        WrapperPlayServerOpenSignEditor openSign = new WrapperPlayServerOpenSignEditor(peLocation, true);
        WrapperPlayServerCloseWindow closeWindow = new WrapperPlayServerCloseWindow(0);

        var playerManager = PacketEvents.getAPI().getPlayerManager();
        playerManager.sendPacket(player, blockChange);
        playerManager.sendPacket(player, blockEntityData);
        playerManager.sendPacket(player, openSign);
        playerManager.sendPacket(player, closeWindow);

        // Timeout (Falls keine Antwort kommt, weil der Client das Paket blockiert)
        BukkitTask task = Bukkit.getScheduler().runTaskLater(ShardZonePlugin.getInstance(), () -> {
            if (pendingMod.remove(uuid) != null) {
                pendingQueue.remove(uuid);
                pendingTimeouts.remove(uuid);
                fail(player, "Timeout-No-Response");
            }
        }, 60L);

        pendingTimeouts.put(uuid, task);
    }

    private void fail(Player player, String modName) {
        // Füge einfach das Wörtchen "final" ganz am Anfang hinzu:
        final List<String> rawLines;

        ConfigurationSection config = ShardZonePlugin.getInstance().getConfig();
        String actionType = config.getString("action.type", "kick").toLowerCase();

        List<String> tempLines = config.getStringList("action.content");
        if (tempLines.isEmpty()) {
            rawLines = Collections.singletonList("&cYou were kicked for using verbotene Mods (%mod%)!");
        } else {
            rawLines = tempLines;
        }

        if (actionType.equals("command")) {
            // Wenn der Typ "command" ist, führen wir jede Zeile in der Liste einzeln aus
            Bukkit.getScheduler().runTask(ShardZonePlugin.getInstance(), () -> {
                for (String commandLine : rawLines) {
                    String formattedCommand = commandLine
                            .replace("%mod%", modName)
                            .replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                }
            });
        } else {
            // Wenn der Typ "kick" oder "message" ist, bauen wir eine mehrzeilige Nachricht mit Adventure
            Component kickMessage = Component.empty();
            for (int i = 0; i < rawLines.size(); i++) {
                String line = rawLines.get(i)
                        .replace("%mod%", modName)
                        .replace("%player%", player.getName());

                kickMessage = kickMessage.append(Parser.color(line));
                if (i < rawLines.size() - 1) {
                    kickMessage = kickMessage.append(Component.newline());
                }
            }
            player.kick(kickMessage);
        }
    }
}