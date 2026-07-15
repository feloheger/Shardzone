package dev.shardzone.moddeteckt;

import io.papermc.paper.math.BlockPosition;

import java.util.List;


public class SignCheckBuilder {


    public static WrapperPlayServerSignData create(
            List<TranslationCheck> checks,
            BlockPosition position
    ) {


        WrapperPlayServerSignData sign =
                new WrapperPlayServerSignData(position);



        int line = 0;


        for(TranslationCheck check : checks) {


            if(line >= 4) {

                break;
            }


            SignComponent component =
                    SignComponent.translation(
                            check.getTranslationKey(),
                            check.getFallback()
                    );


            sign.getFrontSide()
                    .setLine(
                            line,
                            component
                    );


            line++;
        }


        return sign;
    }
}