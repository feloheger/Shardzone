package dev.shardzone.moddeteckt;



import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.papermc.paper.math.BlockPosition;

public class WrapperPlayServerOpenSignEditor extends PacketWrapper<WrapperPlayServerOpenSignEditor> {

    private BlockPosition position;
    private boolean frontSide = true;

    // ----------------------------
    // Konstruktoren
    // ----------------------------

    public WrapperPlayServerOpenSignEditor(PacketSendEvent event) {
        super(event);
    }

    public WrapperPlayServerOpenSignEditor(BlockPosition position, boolean frontSide) {
        super(PacketType.Play.Server.OPEN_SIGN_EDITOR);

        this.position = position;
        this.frontSide = frontSide;
    }

    // ----------------------------
    // Hier kommt read()
    // ----------------------------

    @Override
    public void read() {
        this.position = getPosition();
        this.frontSide = readBoolean();
    }

    // ----------------------------
    // Hier kommt write()
    // ----------------------------

    @Override
    public void write() {
        setPosition(position);
        writeBoolean(frontSide);
    }

    // ----------------------------
    // Getter / Setter
    // ----------------------------

    public BlockPosition getPosition() {
        return position;
    }

    public void setPosition(BlockPosition position) {
        this.position = position;
    }

    public boolean isFrontSide() {
        return frontSide;
    }

    public void setFrontSide(boolean frontSide) {
        this.frontSide = frontSide;
    }
    public WrapperPlayServerOpenSignEditor copy(){
        return new WrapperPlayServerOpenSignEditor(position, frontSide);
    }
    @Override
    public String toString() {
        return "WrapperPlayServerOpenSignEditor{" +
                "position=" + position +
                ", frontSide=" + frontSide +
                '}';
    }

    public PacketTypeCommon getPacketType() {
        return PacketType.Play.Server.OPEN_SIGN_EDITOR;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof WrapperPlayServerOpenSignEditor other)) {
            return false;
        }

        if (frontSide != other.frontSide) {
            return false;
        }

        return position.equals(other.position);
    }
    @Override
    public int hashCode() {
        int result = position.hashCode();
        result = 31 * result + (frontSide ? 1 : 0);
        return result;
    }
    public boolean hasPosition() {
        return position != null;
    }
    public WrapperPlayServerOpenSignEditor(BlockPosition position) {
        this(position, true);
    }

    public static WrapperPlayServerOpenSignEditor openFront(BlockPosition position) {
        return new WrapperPlayServerOpenSignEditor(position, true);
    }

    public static WrapperPlayServerOpenSignEditor openBack(BlockPosition position) {
        return new WrapperPlayServerOpenSignEditor(position, false);
    }
    public void openFront() {
        this.frontSide = true;
    }

    public void openBack() {
        this.frontSide = false;
    }
    public void reset() {
        this.position = null;
        this.frontSide = true;
    }

}