import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;

import utils.*;
import model.*;

public class DownloadManager {

    private String localID;
    private String torrentName;
    private String fileName;

    private TorrentInfo torrent;
    //private TrackerCommunicator tracker;

    // Keep track of the status of each piece
    private BitSet bitfield;

    // Queues of pieces
    private List<Piece> toDownload;
    private List<Piece> downloaded;

    // Arguments to keep track of
    // How much this client has uploaded or downloaded to/from another peer
    private static String amount_uploaded = "0";
    private static String amount_downloaded = "0";

    // Amount left to download
    private static String amount_left = "0";

    /*public DownloadManager(String localID, String torrentFile, String fileName) {
        this.torrentName = torrentFile;
        this.fileName = fileName;
        this.localID = localID;

        initTorrentAndTracker(torrentFile);
    }

    public void initTorrentAndTracker(String torrentFile) {
        // Init torrent
        try {
            this.torrent = new TorrentInfo(Files.readAllBytes(Paths.get(torrentFile)));
        } catch (Exception e) {
            throw new RuntimeException("Can't read file torrent file : " + torrentFile);
        }

        // Init tracker parameters
        HashMap<String, String> parameters = new HashMap<String, String>();

        // Parameters values
        String[] values = new String[]{
          Utils.toHex(torrent.info_hash.array()),                            // SHA1 Hash (in HEX and URL encoded)
          localID,                            // Peer ID of this client
          "6881",
          amount_uploaded,
          amount_downloaded,
          amount_left,
          "start"
        };

        // Add key-value pairs to parameters hashmap
        for (int i = 0; i < values.length; i++)
          parameters.put(TrackerCommunicator.PARAMETER_KEYS[i], values[i]);

        // Init tracker
        tracker = new TrackerCommunicator(torrent.announce_url.toString(), parameters);
    }*/


    /**  Principal method to download something  **/
/*
    public void getFileFromPeer() throws Exception {

        if (peer == null) {
            throw new PeerCommunicationException("Peer is null");
        }

        try {
            fileWriter = new RandomAccessFileWriter(torrentStats.getOutFile());
            generateRequestsQueue();

            peer.openConnection();

            // send handshake and verify response
            peer.initiateHandshake(torrentStats.getClientPeerId(), torrentStats.getTorrent().info_hash.array());

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
                  if(peer.isConnectionAlive()){  
                    Message message = peer.receiveMessage();
                    //Utils.printLog("Received message : " + message);
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
                  } else {
                    throw new PeerCommunicationException("Connection with peer is not alive!");
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
*/


    /* handle message of type Have */
    /*private void handleHave(Message haveMsg) throws IOException, PeerCommunicationException, Exception {
        printlnLog("Handling have message : " + haveMsg);
        if (!verifyHave(haveMsg)) {
            throw new PeerCommunicationException("Have message is not verified!");
        }
        peer.markBitfieldDownloaded(haveMsg.getPieceIndex());
        if(!torrentStats.isPieceCompleted(haveMsg.getPieceIndex())) {
            peer.sendInterested();
        }
        //printlnLog("Handled have message : " + haveMsg);
    }*/

    /* verify message of Have type */
    /*private boolean verifyHave(Message haveMsg) {
        if (haveMsg.getPieceIndex() >= 0
                && haveMsg.getPieceIndex() < torrentStats.getPieceNumber()) {
            return true;
        }
        return false;
    }*/


    /* handle Piece message */
    /*private void handlePiece(Message pieceMsg) throws NoSuchAlgorithmException, IOException, Exception {
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
        //printLog("Handled piece message : " + pieceMsg);
    }*/


    public void markBitfieldDownloaded(int pieceIndex) {
        // Since MsgUtils is setting the final bit to use, no need to catch exception here
        bitfield.set(Utils.toBitSetIndex(pieceIndex, bitfield));
    }

    public void setBitfield(ByteBuffer bitfield) {
        this.bitfield = BitSet.valueOf(bitfield);
    }


    public static void main(String[] args) throws Exception {
        // Start a peer server
        new PeerServer("remote78901234567890", 4444).startListener();

        String file_hash = "%A7%D3%D5%C5%4F%A6%38%A5%3F%28%0B%C9%41%10%60%EF%26%2D%FE%B6";
        int port = 4444;

        Socket socket1 = new Socket("127.0.0.1", port);
        Socket socket2 = new Socket("127.0.0.1", port);
        Socket socket3 = new Socket("127.0.0.1", port);

        SynchronousListOfPieces toDownload = new SynchronousListOfPieces(new LinkedList<Piece>());
        SynchronousListOfPieces downloaded = new SynchronousListOfPieces(new LinkedList<Piece>());

        PeerCommunicator peerComm1 =  new PeerCommunicator("local543210987654321", "remote78901234567890", socket1);
        PeerCommunicator peerComm2 =  new PeerCommunicator("local543210987654322", "remote78901234567890", socket2);
        PeerCommunicator peerComm3 =  new PeerCommunicator("local543210987654323", "remote78901234567890", socket3);
        peerComm1.piece_size = 780;
        peerComm2.piece_size = 780;
        peerComm3.piece_size = 780;

        int total_file_size = 58887;
        String file_name = "testfull.txt";


        // Download algorithm
        ExecutionThreads.DownloadAlgorithmThread da_thread = new ExecutionThreads.DownloadAlgorithmThread(
            new DownloadAlgorithm(total_file_size / peerComm1.piece_size),
            toDownload, peerComm1.piece_size, total_file_size
        );

        // FileSaver
        ExecutionThreads.FileSaverThread fs_thread = new ExecutionThreads.FileSaverThread(
            new FileSaver(file_name, total_file_size / peerComm1.piece_size + 1, downloaded, peerComm1.piece_size)
        );

        Thread algothread = new Thread(da_thread);
        Thread fsthread = new Thread(fs_thread);

        ExecutionThreads.PeerCommunicationThread peer_thread1 = new ExecutionThreads.PeerCommunicationThread(
            peerComm1, toDownload, downloaded, algothread, file_hash
        );

        ExecutionThreads.PeerCommunicationThread peer_thread2 = new ExecutionThreads.PeerCommunicationThread(
            peerComm2, toDownload, downloaded, algothread, file_hash
        );

        ExecutionThreads.PeerCommunicationThread peer_thread3 = new ExecutionThreads.PeerCommunicationThread(
            peerComm3, toDownload, downloaded, algothread, file_hash
        );

        Thread peerthread1 = new Thread(peer_thread1);
        Thread peerthread2 = new Thread(peer_thread2);
        Thread peerthread3 = new Thread(peer_thread3);
        peerthread1.start();
        peerthread2.start();
        peerthread3.start();

        // Start peers first ('cause they have to handshake first)
        algothread.start();
        fsthread.start();

        algothread.join();
        System.out.println("Download algorithm completed");

        peerthread1.join();
        peerthread2.join();
        peerthread3.join();
        fsthread.join();
        System.out.println("FileSaver thread completed");

        System.out.println("Success !");
    }
}