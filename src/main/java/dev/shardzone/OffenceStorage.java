package dev.shardzone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores and persists the number of Meteor Client detections per player UUID.
 * Saved to plugins/ShardZone/meteor_offences.json
 */
public class OffenceStorage {

    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, Integer> offences = new HashMap<>();

    public OffenceStorage(ShardZonePlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "meteor_offences.json");
        load();
    }

    public int getOffences(UUID uuid) {
        return offences.getOrDefault(uuid.toString(), 0);
    }

    public int incrementAndGet(UUID uuid) {
        int count = getOffences(uuid) + 1;
        offences.put(uuid.toString(), count);
        save();
        return count;
    }

    public void reset(UUID uuid) {
        offences.remove(uuid.toString());
        save();
    }

    private void load() {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) offences = loaded;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(offences, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
