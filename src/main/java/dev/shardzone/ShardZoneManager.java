package dev.shardzone;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages the core features, tasks, and configurations of the ShardZone.
 */
public class ShardZoneManager {

    private final ShardZonePlugin plugin;
    private final Map<UUID, Integer> accumulatedSeconds;
    private final File progressFile;
    private YamlConfiguration progressConfig;
    private BukkitTask task;

    private String worldName;
    private double centerX;
    private double centerZ;
    private double radius;
    private int shardsPerMinute;
    private String shardCommand;
    private String actionBarInZone;
    private String actionBarOutZone;

    /**
     * Initializes the ShardZone manager and loads config.
     *
     * @param plugin the plugin instance
     */
    public ShardZoneManager(ShardZonePlugin plugin) {
        this.plugin = plugin;
        this.accumulatedSeconds = new HashMap<>();
        this.progressFile = new File(plugin.getDataFolder(), "progress.yml");
        loadConfig();
        loadProgress();
    }

    /**
     * Loads configuration values from the main config file.
     */
    public void loadConfig() {
        FileConfiguration config = this.plugin.getConfig();
        this.worldName = config.getString("zone.spawn", "spawn");
        this.centerX = config.getDouble("zone.center.x", 0.0);
        this.centerZ = config.getDouble("zone.center.z", 0.0);
        this.radius = config.getDouble("zone.radius", 60.0);
        this.shardsPerMinute = config.getInt("shards-per-minute", 1);
        this.shardCommand = config.getString("shard-command",
                "shardmanager add %player% %amount%");
        this.actionBarInZone = config.getString("messages.actionbar-in-zone",
                "§d❤ Gem Zone §7| §fNext gem in §d%seconds%s");
        this.actionBarOutZone = config.getString("messages.actionbar-out-zone", "");
    }

    /**
     * Loads saved progress from the progress.yml file.
     */
    public void loadProgress() {
        if (!this.progressFile.exists()) {
            this.progressConfig = new YamlConfiguration();
            return;
        }
        this.progressConfig = YamlConfiguration.loadConfiguration(this.progressFile);
        for (String key : this.progressConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int seconds = this.progressConfig.getInt(key);
                this.accumulatedSeconds.put(uuid, seconds);
            } catch (IllegalArgumentException e) {
                // Ignored invalid UUID key
            }
        }
    }

    /**
     * Saves progress for all currently cached players.
     */
    public void saveAllProgress() {
        for (Map.Entry<UUID, Integer> entry : this.accumulatedSeconds.entrySet()) {
            this.progressConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            this.progressConfig.save(this.progressFile);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save progress: " + e.getMessage());
        }
    }

    /**
     * Starts the continuous task ticking every 20 ticks (1 second).
     */
    public void startTask() {
        this.plugin.getLogger().info("Task started. Zone: center="
                + this.centerX + "," + this.centerZ + " radius=" + this.radius);
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
    }

    /**
     * Ticking method run every second. Processes players within the zone.
     */
    public void tick() {
        World world = Bukkit.getWorld(this.worldName);
        if (world == null) {
            this.plugin.getLogger().warning("World '" + this.worldName + "' not found!");
            return;
        }

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (!player.getWorld().getName().equals(this.worldName)) {
                continue;
            }

            double distanceX = player.getLocation().getX() - this.centerX;
            double distanceZ = player.getLocation().getZ() - this.centerZ;
            double distanceSquared = (distanceX * distanceX) + (distanceZ * distanceZ);

            if (distanceSquared <= (this.radius * this.radius)) {
                UUID uuid = player.getUniqueId();
                int seconds = this.accumulatedSeconds.getOrDefault(uuid, 0) + 1;

                if (seconds >= 60) {
                    seconds = 0;
                    giveShards(player, this.shardsPerMinute);
                }

                this.accumulatedSeconds.put(uuid, seconds);

                if (this.actionBarInZone != null && !this.actionBarInZone.isEmpty()) {
                    sendActionBar(player, this.actionBarInZone.replace("%seconds%",
                            String.valueOf(60 - seconds)));
                }
            } else {
                if (this.actionBarOutZone != null && !this.actionBarOutZone.isEmpty()) {
                    sendActionBar(player, this.actionBarOutZone);
                }
            }
        }
    }

    /**
     * Saves progress for a specific player UUID.
     *
     * @param uuid the player's unique ID
     */
    public void savePlayer(UUID uuid) {
        if (this.accumulatedSeconds.containsKey(uuid)) {
            this.progressConfig.set(uuid.toString(), this.accumulatedSeconds.get(uuid));
            try {
                this.progressConfig.save(this.progressFile);
            } catch (IOException e) {
                this.plugin.getLogger().warning("Could not save player progress: " + e.getMessage());
            }
        }
    }

    /**
     * Executes the shard command to grant players their rewards via the console.
     *
     * @param player the player receiving shards
     * @param amount the amount of shards
     */

    private void giveShards(Player player, int amount) {
        String cmd = this.shardCommand
                .replace("%player%", player.getName())
                .replace("%amount%", String.valueOf(amount));

        this.plugin.getLogger().info("Giving shards via console: " + cmd);

        try {
            // Führt den Befehl sicher als Server-Konsole aus
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } catch (Exception e) {
            this.plugin.getLogger().warning("Console command failed: " + e.getMessage());
        }

        player.sendMessage("§d+" + amount + " Gems");
    }

    /**
     * Helper method to dispatch action bar messages cleanly across platforms.
     *
     * @param player  the receiver
     * @param message the text to display
     */
    private void sendActionBar(Player player, String message) {
        try {
            // Übersetzt alle '&' Farbcodes in echte Minecraft-Farben
            String coloredMessage = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);

            Class<?> chatTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object actionBarType = chatTypeClass.getField("ACTION_BAR").get(null);

            Method fromLegacyTextMethod = textComponentClass.getMethod("fromLegacyText", String.class);
            Object[] components = (Object[]) fromLegacyTextMethod.invoke(null, coloredMessage);

            player.spigot().sendMessage((net.md_5.bungee.api.ChatMessageType) actionBarType,
                    (net.md_5.bungee.api.chat.BaseComponent[]) components);
        } catch (Exception e) {
            this.plugin.getLogger().warning("ActionBar failed: " + e.getMessage());
        }
    }
}