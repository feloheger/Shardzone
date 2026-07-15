package dev.shardzone.moddeteckt;

import dev.shardzone.moddeteckt.ActionType;
import dev.shardzone.moddeteckt.DetectionAction;
import dev.shardzone.moddeteckt.ModDetectionConfig;
import dev.shardzone.moddeteckt.TranslationCheck;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ModConfigLoader {


    public static ModDetectionConfig load(
            FileConfiguration config
    ) {


        List<TranslationCheck> checks =
                new ArrayList<>();


        /*
         * Alle Mod-Einträge durchgehen
         */
        for(String key : config.getKeys(false)) {


            if(key.equalsIgnoreCase("action")) {

                continue;
            }


            ConfigurationSection section =
                    config.getConfigurationSection(key);


            if(section == null) {

                continue;
            }


            String name =
                    section.getString(
                            "name",
                            key
                    );


            String translationKey =
                    section.getString(
                            "translation-key"
                    );


            String fallback =
                    section.getString(
                            "fallback",
                            ""
                    );


            if(translationKey == null) {

                continue;
            }


            checks.add(
                    new TranslationCheck(
                            key,
                            name,
                            translationKey,
                            fallback
                    )
            );
        }



        /*
         * Action laden
         */

        String type =
                config.getString(
                        "action.type",
                        "kick"
                );


        String content =
                config.getString(
                        "action.content",
                        "Unerlaubte Clientmodifikation (%mod%)"
                );



        DetectionAction action =
                new DetectionAction(
                        ActionType.valueOf(
                                type.toUpperCase()
                        ),
                        content
                );



        return new ModDetectionConfig(
                checks,
                action
        );
    }
}