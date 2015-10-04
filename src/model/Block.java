package model;

import java.util.Arrays;
/**
 * Created by nadiachepurko on 10/2/15.
 */
public class Block implements Comparable<Block>{

    private Integer index;
    private int pieceIndex;
    private int offset;
    private int length;
    private byte[] data;

    @Override
    public int compareTo(Block that) {
        return this.index.compareTo(that.index);
    }

    public Block(int pieceIndex, int offset, int length) {
        this.pieceIndex = pieceIndex;
        this.offset = offset;
        this.length = length;
        this.index = offset / TorrentStats.BLOCK_LENGTH;
    }

    public Block(int pieceIndex, int offset, byte[] data) {
        this(pieceIndex, offset, data.length);
        this.data = data;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Block)) return false;

        Block block = (Block) o;

        if (offset != block.offset) return false;
        if (pieceIndex != block.pieceIndex) return false;

        return true;
    }

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
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
