package model;
import utils.MsgUtils;

import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;

/** group 16
 * Created by nadiachepurko on 10/3/15.
 */
public class Piece {

    private int index;
    private int blockNumber; /* number of blocks per piece */
    private Set<Block> blocks;
    private BitSet blocksCompleted;

    public Piece(int index, int blockNumber) {
        this.index = index;
        this.blockNumber = blockNumber;
        this.blocks = new TreeSet<Block>();
        this.blocksCompleted = new BitSet(blockNumber);
    }

    public int getIndex() {
        return index;
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
        blocksCompleted.set(MsgUtils.toBitSetIndex(block.getIndex(), blocksCompleted.size()));
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
