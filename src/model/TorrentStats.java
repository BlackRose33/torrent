package model;

import utils.MsgUtils;
import utils.TorrentInfo;
import utils.Utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nadiachepurko on 10/2/15.
 */
public class TorrentStats {

    public static final int BLOCK_LENGTH = 16384; //TODO: check for correctness

    private String clientPeerId;            /* our local client peer Id */
    private File outFile;
    private BitSet piecesCompleted;
    private TorrentInfo torrentInfo;
    private int pieceBlockNumber;
    private int lastPieceLength;
    private int lastPieceBlockNumber;
    private int lastBlockLength;
    private Map<Integer, Piece> receivedPieceBlocks = new HashMap<Integer, Piece>();

    public TorrentStats(TorrentInfo torrentInfo, String clientPeerId, File outFile) {
        this.torrentInfo = torrentInfo;
        this.clientPeerId = clientPeerId;
        this.outFile = outFile;
        this.lastPieceLength = Math.abs(getFileLength() - (getPieceLength() * (getPieceNumber() - 1)));
        this.pieceBlockNumber = getPieceLength() / BLOCK_LENGTH;
        this.lastPieceBlockNumber = (int) Math.ceil((double) lastPieceLength / (double) BLOCK_LENGTH);
        this.lastBlockLength = lastPieceLength % BLOCK_LENGTH;
        this.piecesCompleted = new BitSet(getPieceNumber());
        initReceivedPieceBlocks();
    }

    /* Create empty blocks that will later be filled with data */
    private void initReceivedPieceBlocks() {
        this.receivedPieceBlocks = new HashMap<Integer, Piece>(getPieceNumber());
        for (int i = 0; i < getPieceNumber(); i++) {
            Piece piece;
            if (i == getPieceNumber() - 1) {
                piece = new Piece(i, lastPieceBlockNumber);
            } else {
                piece = new Piece(i, pieceBlockNumber);
            }
            receivedPieceBlocks.put(piece.getIndex(), piece);
        }
    }

    public boolean isFileDownloaded() {
        return piecesCompleted.cardinality() == getPieceNumber();
    }

    public boolean isPieceCompleted(int pieceIndex) {
        return piecesCompleted.get(toBitSetIndex(pieceIndex));
    }

    public boolean isFileEmpty() { return piecesCompleted.isEmpty(); }

    public void markPieceNotCompleted(int pieceIndex) {
        piecesCompleted.clear(toBitSetIndex(pieceIndex));
    }

    public void addBlock(Block block) {
        Piece piece = receivedPieceBlocks.get(block.getPieceIndex());
        if (piece != null) {
            piece.addBlock(block);
            if (piece.isCompleted()) {
                markPieceCompleted(piece.getIndex());
            }
        } else {
            Utils.printlnLog("Can't find piece downloaded blocks in cache. " +
                    "It seems it has been already saved to filesystem but we received this piece block again!");
        }
    }

    public Piece getPieceForSaving(int pieceIndex) {
        return receivedPieceBlocks.remove(pieceIndex);
    }

    private void markPieceCompleted(int pieceIndex) {
        piecesCompleted.set(toBitSetIndex(pieceIndex));
    }

    private int toBitSetIndex(int pieceIndex) {
        return MsgUtils.toBitSetIndex(pieceIndex, piecesCompleted.size());
    }

    /**
     * Returns SHA1 hash of piece with particular index
     * @param pieceIndex
     * @return
     */
    public ByteBuffer getPieceHash(int pieceIndex) {
        return torrentInfo.piece_hashes[pieceIndex];
    }

    public int getFileLength() {
        return torrentInfo.file_length;
    }


    /* getters and setters */

    /* return numbers of pieces in a file */
    public int getPieceNumber() {
        return torrentInfo.piece_hashes.length;
    }

    public int getPieceLength() {
        return torrentInfo.piece_length;
    }

    public int getLastPieceLength() {
        return lastPieceLength;
    }

    public int getPieceBlockNumber() {
        return pieceBlockNumber;
    }

    public int getLastPieceBlockNumber() {
        return lastPieceBlockNumber;
    }

    public byte[] getBitfield() {
        return piecesCompleted.toByteArray();
    }

    public int getLastBlockLength() {
        return lastBlockLength;
    }

    public TorrentInfo getTorrent() {
        return torrentInfo;
    }

    public String getClientPeerId() {
        return clientPeerId;
    }

    public File getOutFile() {
        return outFile;
    }


}
