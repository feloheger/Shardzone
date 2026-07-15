package dev.shardzone;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameTagUpdateListener implements Listener {

    private final ShardZonePlugin plugin;

    public NameTagUpdateListener(ShardZonePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateNameTag(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Beim Verlassen säubern wir das Team des Spielers im Scoreboard
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(event.getPlayer().getName());
        if (team != null) {
            team.unregister();
        }
    }

    /**
     * Diese Methode aktualisiert das Nametag über dem Kopf des Spielers bombenfest.
     */
    public void updateNameTag(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> updateNameTag(player));
            return;
        }

        String rawTeam = "%economy_team%";
        String rawMoney = "%economy_nicestMoney%";

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            rawTeam = PlaceholderAPI.setPlaceholders(player, "%economy_team%");
            rawMoney = PlaceholderAPI.setPlaceholders(player, "%economy_nicestMoney%");
        } else {
            rawTeam = "Kein Team";
            rawMoney = "0";
        }

        String cleanTeam = rawTeam.replace("[", "").replace("]", "");

        // Hol das Haupt-Scoreboard des Servers
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Erstelle ein einzigartiges Scoreboard-Team für diesen Spieler, falls es noch nicht existiert
        Team scoreboardTeam = scoreboard.getTeam(player.getName());
        if (scoreboardTeam == null) {
            scoreboardTeam = scoreboard.registerNewTeam(player.getName());
        }

        // Spieler dem Team zuweisen
        scoreboardTeam.addEntry(player.getName());

        // Das Suffix, das direkt hinter dem Spielernamen angezeigt wird (wie im Screenshot rechts)
        // §e⛁ = Gelbe Münzen | §8| = Trenner | §b👥 = Blaues Team
        String suffix = "§e$ §e" + rawMoney + " §8| §b👥 §b" + cleanTeam;;

        scoreboardTeam.setSuffix(suffix);
    }
}