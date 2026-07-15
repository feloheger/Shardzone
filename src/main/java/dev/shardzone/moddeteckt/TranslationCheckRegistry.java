package dev.shardzone.moddeteckt;

import dev.shardzone.moddeteckt.TranslationCheck;

import java.util.ArrayList;
import java.util.List;

public class TranslationCheckRegistry {


    private final List<TranslationCheck> checks;
    public TranslationCheck getById(String id) {

        for(TranslationCheck check : checks) {

            if(check.getId().equalsIgnoreCase(id)) {

                return check;
            }
        }

        return null;
    }

    public TranslationCheckRegistry() {

        this.checks = new ArrayList<>();
    }


    public void add(
            TranslationCheck check
    ) {

        checks.add(check);
    }


    public List<TranslationCheck> getChecks() {

        return checks;
    }
}