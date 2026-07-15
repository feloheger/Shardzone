package dev.shardzone.moddeteckt;


public class SignSide {


    private final SignComponent[] lines;


    private SignColor color;


    private boolean glowing;



    public SignSide() {


        this.lines = new SignComponent[] {

                null,
                null,
                null,
                null
        };


        this.color = SignColor.BLACK;


        this.glowing = false;
    }



    public void setLine(
            int index,
            SignComponent component
    ) {


        checkIndex(index);


        lines[index] = component;
    }



    public SignComponent getLine(
            int index
    ) {


        checkIndex(index);


        return lines[index];
    }



    public SignComponent[] getLines() {

        return lines;
    }



    /**
     * Gibt die 4 Zeilen als JSON zurück.
     * Nicht gesetzte Zeilen werden leer gesetzt.
     */
    public String[] buildJsonLines() {


        String[] result =
                new String[4];



        for(int i = 0; i < 4; i++) {


            if(lines[i] == null) {

                result[i] =
                        "{\"text\":\"\"}";

            } else {

                result[i] =
                        lines[i].toJson();
            }
        }


        return result;
    }




    public SignColor getColor() {

        return color;
    }



    public void setColor(
            SignColor color
    ) {

        this.color = color;
    }




    public boolean isGlowing() {

        return glowing;
    }



    public void setGlowing(
            boolean glowing
    ) {

        this.glowing = glowing;
    }





    private void checkIndex(
            int index
    ) {


        if(index < 0 || index > 3) {


            throw new IllegalArgumentException(
                    "Sign line must be between 0 and 3"
            );
        }
    }
}