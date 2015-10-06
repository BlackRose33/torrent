import exceptions.PeerCommunicationException;
import model.*;
import utils.MsgUtils;
import utils.RandomAccessFileWriter;
import utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static utils.Utils.printLog;
import static utils.Utils.printlnLog;




/**
 * Created by nadiachepurko on 9/26/15.
 */

public class PeerCommunicator {

    private final static int THREAD_SLEEP_INTERVAL = 1000; /* 1 second */

    private RandomAccessFileWriter fileWriter;
    private TorrentStats torrentStats;
    private Peer peer;

    // blocks requests has been already sent to peer and waiting response
    private Set<Block> unansweredQueue = new HashSet<Block>();

    // all blocks of the file we are going to request from peers
    private Queue<Block> requestQueue = new LinkedList<Block>();

    public PeerCommunicator(Peer peer, TorrentStats torrentStats){
        this.torrentStats = torrentStats;
        this.peer = peer;
    }

    public void getFileFromPeer(Peer peer) throws Exception {

        if (peer == null) {
            throw new PeerCommunicationException("Peer is null");
        }

        try {
            fileWriter = new RandomAccessFileWriter(torrentStats.getOutFile());
            generateRequestsQueue();

            peer.openConnection();

            // send handshake and verify response
            peer.makeHandshake(torrentStats.getClientPeerId(), torrentStats.getTorrent().info_hash.array());

            // Bitfield message: A peer MUST send this message immediately after the handshake operation,
            // and MAY choose not to send it if it has no pieces at all.
            // This message MUST not be sent at any other time during the communication.
            peer.sendBitfield(torrentStats);

            // Interested message: This message has ID 2 and no payload.
            // A peer sends this message to a remote peer to inform the remote peer of its desire to request data.
            peer.sendInterested();

            // read peer bitfield message. Remote peer must send it after the handshake was performed
            Message bitfield = peer.receiveMessage();

            // if peer did not send bitfiled after handshake then it did not downloaded any pieces of file
            if (bitfield != null) {
                printlnLog("Peer bitfield : " + bitfield.toString());
                handleBitfield(bitfield);

                while (!torrentStats.isFileDownloaded()) {
                    Message message = peer.receiveMessage();
                    Utils.printLog("Received message : " + message);
                    updateConnectionState(message); //handle choke or unchoke messages

                    if (peer.amIDownloadingFromPeer()) {

                        if (message != null) {
                            switch (message.getType()) {
                                case MsgType.HAVE:
                                    handleHave(message);
                                    break;
                                case MsgType.REQUEST:
                                    handleRequest(message);
                                    break;
                                case MsgType.PIECE:
                                    handlePiece(message);
                                    break;
                                case MsgType.CANCEL:
                                    handleCancel(message);
                                    break;
                            }
                        }

                        boolean sent = sendRequest();
                        printLog("After pipping requests sent = " + sent);

                    } else {
                        Thread.sleep(THREAD_SLEEP_INTERVAL);
                    }

                }

            } else {
                printlnLog("Peer did not downloaded any piece of file yet so close connection");
            }

            printlnLog("Finished Peer Communicator!");

        } finally {
            peer.closeConnection();
            fileWriter.close();
        }

    }


    private boolean sendRequest() {
        printlnLog("Sending request : ");
        Block request = requestQueue.poll();
        return trySend(request);
    }

    /* try to request a block of data */
    private boolean trySend(Block request) {
        printLog("Try to send request => " + request);
        try {
            peer.sendMessage(MsgUtils.buildRequest(request));
            unansweredQueue.add(request);
            return true;
        } catch (Exception e) {
            requestQueue.add(request);
            unansweredQueue.remove(request);
        }
        return false;
    }

    /* create queue with all blocks for all pieces we want to request in the future */
    private void generateRequestsQueue() {
        for (int i = 0; i < torrentStats.getPieceNumber(); i++) {
            if (i == torrentStats.getPieceNumber() - 1) {
                if (torrentStats.getLastPieceBlockNumber() > 1) {
                    queueBlocks(i, torrentStats.getLastPieceBlockNumber() - 1);
                }
                queueBlock(i, torrentStats.getLastPieceBlockNumber() - 1, torrentStats.getLastBlockLength());
            } else {
                queueBlocks(i, torrentStats.getPieceBlockNumber());
            }
        }
    }

    /* queue all blocks within one piece */
    private void queueBlocks(int pieceIndex, int blockNumber) {
        for (int j = 0; j < blockNumber; j++) {
            queueBlock(pieceIndex, j, TorrentStats.BLOCK_LENGTH);
        }
    }

    /* create an object of type Block and add to the queue */
    private void queueBlock(int pieceIndex, int index, int blockLength) {
        int offset = index * TorrentStats.BLOCK_LENGTH;
        Block block = new Block(pieceIndex, offset, blockLength);
        requestQueue.add(block);
    }

    /* check if remote peer chokes or unchokes */
    private void updateConnectionState(Message message) {
        if (message != null) {
            switch (message.getType()) {
                case MsgType.CHOKE:
                    handleChoke();
                    break;
                case MsgType.UNCHOKE:
                    handleUnchoke();
                    break;
            }
        }
    }

    /* handle message of type Have */
    private void handleHave(Message haveMsg) throws IOException, PeerCommunicationException {
        printlnLog("Handling have message : " + haveMsg);
        if (!verifyHave(haveMsg)) {
            throw new PeerCommunicationException("Have message is not verified!");
        }
        peer.markBitfieldDownloaded(haveMsg.getPieceIndex());
        if(!torrentStats.isPieceCompleted(haveMsg.getPieceIndex())) {
            peer.sendInterested();
        }
        printlnLog("Handled have message : " + haveMsg);
    }

    /* verify message of Have type */
    private boolean verifyHave(Message haveMsg) {
        if (haveMsg.getPieceIndex() >= 0
                && haveMsg.getPieceIndex() < torrentStats.getPieceNumber()) {
            return true;
        }
        return false;
    }

    // TODO: do I need it?
    private void handleCancel(Message cancelMsg) {
        printlnLog("Input cancel messages are not supported");
    }

    // TODO: do I need it?
    private void handleRequest(Message requestMsg) {
        printlnLog("Input request messages are not supported");
    }

    /* handle Piece message */
    private void handlePiece(Message pieceMsg) throws NoSuchAlgorithmException, IOException {
        printlnLog("Handling piece message : " + pieceMsg);
        unansweredQueue.remove(pieceMsg.getBlock());
        torrentStats.addBlock(pieceMsg.getBlock());
        // if piece completed - save it
        if (torrentStats.isPieceCompleted(pieceMsg.getPieceIndex())) {
            Piece piece = torrentStats.getPieceForSaving(pieceMsg.getPieceIndex());
            if (verifyHash(piece)) {
                savePiece(piece);
                peer.sendMessage(MsgUtils.buildHave(piece.getIndex()));
                printLog("Piece hash has been verified : " + piece);
            } else {
                reDownload(piece);
                printLog("Piece hash does not equal hash from torrent meta file : " + piece);
            }
        }
        printLog("Handled piece message : " + pieceMsg);
    }

    /* re-download the piece in case of failure or hash mismatch */
    private void reDownload(Piece piece) {
        torrentStats.markPieceNotCompleted(piece.getIndex());
        int blockNumber = (piece.getIndex() == (torrentStats.getPieceNumber() - 1)) ?
                torrentStats.getLastPieceBlockNumber() : torrentStats.getPieceBlockNumber();
        queueBlocks(piece.getIndex(), blockNumber);
    }

    /* verify hash of received piece */
    private boolean verifyHash(Piece piece) throws NoSuchAlgorithmException {
        ByteBuffer expectedHash = torrentStats.getPieceHash(piece.getIndex());
        byte[] actualHash = Utils.generateHash(piece.getData());
        return Arrays.equals(expectedHash.array(), actualHash);
    }

    // TODO: save piece to file in particular position with piece (block) offset
    private void savePiece(Piece piece) throws IOException {
        printLog("Saving piece : " + piece);
        int pieceDataOffset = piece.getIndex() * torrentStats.getPieceLength();
        fileWriter.write(pieceDataOffset, piece.getData());
    }

    private void handleBitfield(Message bitfieldMsg) {
        printlnLog("Handling bitfield message");
        peer.setBitfield(bitfieldMsg.getPayload());
        printLog("Handled bitfield message");
    }

    private void handleChoke() {
        printlnLog("Handling choke message");
        peer.setMeChoked(true);
        // disregard all unanswered requests after choke and schedule to send it again
        requestQueue.addAll(unansweredQueue);
        unansweredQueue.clear();
        printLog("Handled choke message");
    }

    private void handleUnchoke() {
        printlnLog("Handling unchoke message");
        peer.setMeChoked(false);
        printLog("Handled unchoke message");
    }
}
