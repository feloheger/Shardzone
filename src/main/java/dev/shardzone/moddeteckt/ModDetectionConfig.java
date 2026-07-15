package dev.shardzone.moddeteckt;

import dev.shardzone.moddeteckt.DetectionAction;
import dev.shardzone.moddeteckt.TranslationCheck;

import java.util.List;

public class ModDetectionConfig {


    private final List<TranslationCheck> checks;

    private final DetectionAction action;



    public ModDetectionConfig(
            List<TranslationCheck> checks,
            DetectionAction action
    ) {

        this.checks = checks;

        this.action = action;
    }



    public List<TranslationCheck> getChecks() {

        return checks;
    }



    public DetectionAction getAction() {

        return action;
    }
}