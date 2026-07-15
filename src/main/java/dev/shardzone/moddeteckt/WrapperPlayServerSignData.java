package dev.shardzone.moddeteckt;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.papermc.paper.math.BlockPosition;


public class WrapperPlayServerSignData extends PacketWrapper {


    private BlockPosition position;


    private SignSide frontSide;


    private SignSide backSide;
    private int blockEntityType = 9;



    // ----------------------------
    // Konstruktoren
    // ----------------------------


    public WrapperPlayServerSignData(
            PacketSendEvent event
    ) {

        super(event);
    }



    public WrapperPlayServerSignData(
            BlockPosition position
    ) {

        super(PacketType.Play.Server.BLOCK_ENTITY_DATA);


        this.position = position;

        this.frontSide = new SignSide();

        this.backSide = new SignSide();
    }



    // ----------------------------
    // read()
    // ----------------------------


    @Override
    public void read() {

        this.position = getPosition();

        /*
         * Die eigentlichen NBT / SignText Daten
         * kommen später hier rein.
         */
    }



    // ----------------------------
    // write()
    // ----------------------------


    @Override
    public void write() {

        setPosition(position);


        /*
         * Hier kommt später das NBT:
         *
         * FrontText
         * BackText
         *
         * Beispiel:
         *
         * frontSide.getLine(0)
         *
         * enthält:
         *
         * translation-key
         * fallback
         */


    }



    // ----------------------------
    // Getter / Setter
    // ----------------------------


    public BlockPosition getPosition() {

        return position;
    }


    public void setPosition(
            BlockPosition position
    ) {

        this.position = position;
    }



    public SignSide getFrontSide() {

        return frontSide;
    }


    public SignSide getBackSide() {

        return backSide;
    }



    public void setFrontSide(
            SignSide frontSide
    ) {

        this.frontSide = frontSide;
    }


    public void setBackSide(
            SignSide backSide
    ) {

        this.backSide = backSide;
    }



    public boolean hasPosition() {

        return position != null;
    }



    public WrapperPlayServerSignData copy() {

        WrapperPlayServerSignData copy =
                new WrapperPlayServerSignData(
                        position
                );


        copy.setFrontSide(
                frontSide
        );


        copy.setBackSide(
                backSide
        );


        return copy;
    }



    public PacketTypeCommon getPacketType() {

        return PacketType.Play.Server.BLOCK_ENTITY_DATA;
    }



    @Override
    public String toString() {

        return "WrapperPlayServerSignData{" +
                "position=" + position +
                ", frontSide=" + frontSide +
                ", backSide=" + backSide +
                '}';
    }



    public void reset() {

        this.position = null;

        this.frontSide = new SignSide();

        this.backSide = new SignSide();
    }

}