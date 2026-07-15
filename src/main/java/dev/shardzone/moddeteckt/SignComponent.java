package dev.shardzone.moddeteckt;


public class SignComponent {


    private final String translationKey;

    private final String fallback;



    public SignComponent(
            String translationKey,
            String fallback
    ) {

        this.translationKey = translationKey;

        this.fallback = fallback;
    }



    public static SignComponent translation(
            String key,
            String fallback
    ) {

        return new SignComponent(
                key,
                fallback
        );
    }



    public String getTranslationKey() {

        return translationKey;
    }



    public String getFallback() {

        return fallback;
    }



    public String toJson() {


        return "{"
                + "\"translate\":\""
                + escape(translationKey)
                + "\","
                + "\"fallback\":\""
                + escape(fallback)
                + "\""
                + "}";
    }



    private String escape(
            String text
    ) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }



    @Override
    public String toString() {

        return toJson();
    }
}