package model;

import java.util.Arrays;
/** group 16
 * Created by nadiachepurko on 10/2/15.
 */
public class Block implements Comparable<Block>{

    public static final int BLOCK_LENGTH = 6;//16384;

    // Index represents the index of the block within the piece
    // So, it's calculated using offset / block's length
    public Integer index;

    public int pieceIndex;
    public int offset;
    
    private int length;
    private byte[] data = null;


    // Compares blocks using the index value
    @Override
    public int compareTo(Block that) {
        return this.index.compareTo(that.index);
    }

    public Block(int pieceIndex, int offset, int length) {
        this.pieceIndex = pieceIndex;
        this.offset = offset;
        this.length = length;
        this.index = offset / BLOCK_LENGTH;
    }

    public Block(int pieceIndex, int offset, byte[] data) {
        this(pieceIndex, offset, data.length);
        this.data = data;
    }


    // Equality is determined by pieceindex and offset, not length of the block
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Block)) return false;

        Block block = (Block) o;

        if (offset != block.offset) return false;
        if (pieceIndex != block.pieceIndex) return false;

        return true;
    }


    // This is not being used anywhere....
    @Override
    public int hashCode() {
        int result = pieceIndex;
        result = 31 * result + offset;
        return result;
    }



    /* ------- getters and setters ------- */
    public Integer getIndex() {
        return index;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Block{" +
                "pieceIndex=" + pieceIndex +
                ", index=" + index +
                ", offset=" + offset +
                ", length=" + length +
                ((data != null) ? ", data=" + Arrays.toString(data) : ", data={}") +
                '}';
    }
}
