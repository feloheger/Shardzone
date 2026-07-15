package dev.shardzone;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Parser {

    // Serializer für Paragraphen (§) und Und-Zeichen (&)
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Regex-Patterns aus dem originalen Bytecode
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern COLOR_TAG_PATTERN = Pattern.compile("<(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white|#[0-9a-fA-F]{6})>");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().build();

    // Privater Konstruktor, um Instanziierung zu verhindern (Utility-Klasse)
    private Parser() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Konvertiert Hex-Farbcodes im Format &#ffffff oder Legacy-Hex-Strukturen
     */
    public static String convertHexToLegacy(String message) {
        if (message == null) {
            return null;
        }

        // Zuerst Standard-Bungee-Farbcodes übersetzen (& -> §)
        message = ChatColor.translateAlternateColorCodes('&', message);

        // Verarbeitet Adventure/Bungee Hex-Farbcodes (§x§f§f§f§f§f§f -> <#ffffff>)
        Matcher matcher = Pattern.compile("§x§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])").matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "<#" + matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4) + matcher.group(5) + matcher.group(6) + ">");
        }
        matcher.appendTail(buffer);
        message = buffer.toString();

        // Ersetzt klassische Farb-Tags mit MiniMessage-kompatiblen Werten
        message = message.replace("§0", "<black>")
                .replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>")
                .replace("§3", "<dark_aqua>")
                .replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>")
                .replace("§6", "<gold>")
                .replace("§7", "<gray>")
                .replace("§8", "<dark_gray>")
                .replace("§9", "<blue>")
                .replace("§a", "<green>")
                .replace("§b", "<aqua>")
                .replace("§c", "<red>")
                .replace("§d", "<light_purple>")
                .replace("§e", "<yellow>")
                .replace("§f", "<white>")
                .replace("§n", "<underlined>")
                .replace("§m", "<strikethrough>")
                .replace("§k", "<obfuscated>")
                .replace("§o", "<italic>")
                .replace("§l", "<bold>")
                .replace("§r", "<reset>");

        // Fügt an jeden Farb-Tag einen Reset an, um Verschachtelungen zu korrigieren
        Matcher colorMatcher = COLOR_TAG_PATTERN.matcher(message);
        StringBuilder resultBuffer = new StringBuilder();
        while (colorMatcher.find()) {
            colorMatcher.appendReplacement(resultBuffer, "<reset>" + colorMatcher.group(0));
        }
        colorMatcher.appendTail(resultBuffer);

        return resultBuffer.toString();
    }

    /**
     * Macht aus einem String ein formatiertes Adventure Component
     */
    public static Component color(String message) {
        if (message == null) {
            return Component.empty();
        }
        String legacy = convertHexToLegacy(message);
        return MINI_MESSAGE.deserialize(legacy);
    }

    /**
     * Verbindet mehrere Components zu einem einzigen Component
     */
    public static Component join(Component... components) {
        if (components == null || components.length == 0) {
            return Component.empty();
        }
        Component result = Component.empty();
        for (Component comp : components) {
            result = result.append(comp);
        }
        return result;
    }

    /**
     * Konvertiert ein Component zurück in einen Legacy-String (§-Format)
     */
    public static String serialise(Component component) {
        if (component == null) {
            return "";
        }
        return SECTION.serialize(component);
    }

    /**
     * Entfernt sämtliche Formatierungen und Farben aus dem Component (Plain Text)
     */
    public static String strip(Component component) {
        if (component == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Formatiert Lebenswerte grau (z. B. für Spieler-Lebensanzeigen)
     */
    public static String convertHealthGray(double health, String decimalColor) {
        String formatted = String.format("%.1f", health);
        // Ersetzt Punkt/Komma, um den Nachkommawert farblich grau einzufärben
        return formatted.replaceAll("([.,]\\d+)", decimalColor + "$1");
    }
}