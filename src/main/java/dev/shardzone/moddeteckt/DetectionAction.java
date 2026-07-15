package dev.shardzone.moddeteckt;



public class DetectionAction {


    private final ActionType type;

    private final String content;



    public DetectionAction(
            ActionType type,
            String content
    ) {

        this.type = type;
        this.content = content;
    }



    public ActionType getType() {

        return type;
    }



    public String getContent() {

        return content;
    }
}