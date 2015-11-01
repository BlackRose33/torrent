package model;
import utils.MsgUtils;

import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;
import java.nio.*;
import utils.*;


public class Piece {

    public int index;
    public int blockNumber;       /* number of blocks per piece */
    private Set<Block> blocks;
    private BitSet blocksCompleted;
    public ByteBuffer expectedHash;

    public byte[] data;

    // If this is the last piece, then it has different length
    public boolean lastPiece = false;
    public int lastPieceLength;

    public Piece(int index, int blockNumber) {
        this.index = index;
        this.blockNumber = blockNumber;
        this.blocks = new TreeSet<Block>();
        this.blocksCompleted = new BitSet(blockNumber);
    }

    public Piece(int index, byte[] data) {
        this.index = index;
        this.data = data;
    }

    public int getIndex() {
        return index;
    }

    public void isLastPiece(int length) { 
        this.lastPiece = true;
        this.lastPieceLength = length;
    }

    public byte[] getData() {
        byte[] data = new byte[getDataLength()];
        for (Block block : blocks) {
            System.arraycopy(block.getData(), 0, data, block.getOffset(), block.getLength());
        }
        return data;
    }

    private int getDataLength() {
        int length = 0;
        for (Block block : blocks) {
            length += block.getLength();
        }
        return length;
    }

    public void addBlock(Block block) {
        blocks.add(block);
        blocksCompleted.set(Utils.toBitSetIndex(block.getIndex(), blocksCompleted));
    }

    public boolean isCompleted() {
        return blocksCompleted.cardinality() == blockNumber;
    }

    @Override
    public String toString() {
        return "Piece{" +
                "index=" + index +
                ", blocks=" + blocks +
                ", blocksCompleted=" + blocksCompleted +
                '}';
    }

}
