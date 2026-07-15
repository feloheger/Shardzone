package dev.shardzone;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BalTop {

    private final JavaPlugin plugin;
    private final Economy economy;
    private List<OfflinePlayer> cachedTop = new ArrayList<>();

    public BalTop(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        // Startet den asynchronen Scheduler: 0L Delay, danach alle 200 Ticks (10 Sekunden)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::update, 0L, 20L * 10L);
    }

    private void update() {
        List<OfflinePlayer> sorted = new ArrayList<>();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (economy.hasAccount(p)) {
                sorted.add(p);
            }
        }
        sorted.sort(Comparator.comparingDouble((OfflinePlayer p) -> economy.getBalance(p)).reversed());
        cachedTop = sorted;

        List<String> top3Formatted = new ArrayList<>();
        int limit = Math.min(3, cachedTop.size());

        for (int i = 0; i < limit; i++) {
            OfflinePlayer player = cachedTop.get(i);
            String name = player.getName();

            // Falls der Name null ist (z.B. Fehler bei Offline-Spielern), überspringen wir das Skin-Update sicherheitshalber
            if (name == null) {
                continue;
            }

            double balance = economy.getBalance(player);
            top3Formatted.add((i + 1) + ". " + name + " <gray>(</gray><green>" + balance + "$</green><gray>)</gray>");

            // Bestimme den NPC-Namen anhand der Platzierung
            String npcName = switch (i) {
                case 0 -> "TOP2"; // 1. Platz
                case 1 -> "TOP1"; // 2. Platz
                case 2 -> "TOP3"; // 3. Platz
                default -> null;
            };

            // Führe den Befehl synchron auf dem Haupt-Thread aus
            if (npcName != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String command = "npc skin " + npcName + " " + name;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                });
            }
        }

        // Konsolen-Print für dich zur Übersicht
        printListToConsole(top3Formatted);
    }

    public OfflinePlayer getPlace(int place) {
        if (cachedTop.isEmpty() || cachedTop.size() < place) return null;
        return cachedTop.get(place - 1);
    }

    public List<OfflinePlayer> getTop(int limit) {
        return cachedTop.stream().limit(limit).toList();
    }

    public void printListToConsole(List<String> meineListe) {
        Bukkit.getConsoleSender().sendMessage(
                MiniMessage.miniMessage().deserialize("<color:#1C90FD><bold>[ShardZone]</bold> Top 3 reichste Spieler (NPC-Skins aktualisiert):</color>")
        );

        for (String eintrag : meineListe) {
            Bukkit.getConsoleSender().sendMessage(
                    MiniMessage.miniMessage().deserialize(" <gray>- </gray><yellow>" + eintrag + "</yellow>")
            );
        }
    }
}