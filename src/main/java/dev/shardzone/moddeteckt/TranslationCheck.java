package dev.shardzone.moddeteckt;

public class TranslationCheck {

    private final String id;

    private final String displayName;

    private final String translationKey;

    private final String fallback;


    public TranslationCheck(
            String id,
            String displayName,
            String translationKey,
            String fallback
    ) {

        this.id = id;
        this.displayName = displayName;
        this.translationKey = translationKey;
        this.fallback = fallback;
    }


    public String getId() {

        return id;
    }


    public String getDisplayName() {

        return displayName;
    }


    public String getTranslationKey() {

        return translationKey;
    }


    public String getFallback() {

        return fallback;
    }
}