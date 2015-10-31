import exceptions.*;
import model.*;
import java.util.*;
import java.util.BitSet;
import utils.*;

import java.security.NoSuchAlgorithmException;

import java.net.*;
import java.io.*;
import java.nio.*;

import static utils.Utils.printLog;
import static utils.Utils.printlnLog;

/**
 *  PeerCommunicator.java
 *  Logical representation of the communication between 2 peers (local and remote)
 *  Contains basic communication methods between peers, like handshake and messages
 */


public class PeerCommunicator {

    /** Defaults (Time measured in miliseconds) **/
    private final static int THREAD_SLEEP_INTERVAL = 1000;
    private static final int KEEP_ALIVE_INTERVAL = 120000;


    // Peer IDs
    private String localPeerID, remotePeerID;

    // Connection status
    private boolean localInterested, localChoked,
                    remoteInterested, remoteChoked;

    // FileManager to refer to
    private FileManager fm;

    // Connection's socket and I\O
    private Socket socket = null;
    private DataOutputStream out;
    private DataInputStream in;

    // For new connections
    private String remoteIP;
    private int    remotePort;

    // Last Keep-Alive time
    private long lastKeepAliveTime;


    /**  Constructors  **/

    public PeerCommunicator(String localPeerID, String remotePeerID, Socket socket) throws Exception {
        this.localPeerID = localPeerID;
        if (remotePeerID != "") this.remotePeerID = remotePeerID;
        this.socket = socket;
        openConnection();
    }

    public PeerCommunicator(String localPeerID, String remotePeerID, String remoteIP, int remotePort) throws Exception {
        this.localPeerID = localPeerID;
        this.remotePeerID = remotePeerID;
        this.remoteIP = remoteIP;
        this.remotePort = remotePort;

        // Open connection automatically
        openConnection();
    }

    /****** Methods to manage socket layer *******/

    public void openConnection() throws Exception {
        // Catch possible exceptions a socket might cause
        // Security exception not needed yet since we don't have a security manager
        // We'll assure proper arguments are passed to this class (assumption)
        if (this.socket == null) {
            try {
                this.socket = new Socket(remoteIP, remotePort);
            }
            catch (UnknownHostException e) {
                throw new PeerException("The host "+remoteIP+" is invalid or unreachable");
            }
            catch (IOException e) {
                throw new PeerException("Unable to assign I/O functionality to the socket to "+remoteIP);
            }
        }

        // Set timeout - give time to connection to establish
        socket.setSoTimeout(2000);

        // Get the IO streams from the socket
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        }
        catch (IOException e) {
            throw new PeerException("Unable to open IO streams for the socket to "+remoteIP);
        }

        // All communications start choked and not interested
        this.remoteChoked = true;
        this.remoteInterested = false;
        this.localChoked = true;
        this.localInterested = false;
    }

    public boolean closeConnection() {
        try {
            in.close();
            out.close();
            socket.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    /***** Methods to check connection's logical status *********/

    public boolean isConnectionAlive() {
        long diffInMillis = System.currentTimeMillis() - lastKeepAliveTime;
        return diffInMillis < KEEP_ALIVE_INTERVAL;
    }

    public boolean amIDownloadingFromPeer() { return localInterested && !localChoked; }

    public boolean amIUploadingToPeer() { return remoteInterested && !remoteChoked; }


    /****** Handshake *******/

    /* perform a handshake with this remote peer */
    public void initiateHandshake(byte[] infoHash) throws Exception {
      try{
        byte[] handshake = MsgUtils.buildHandshake(localPeerID, infoHash);
        System.out.println("length: " + handshake.length);
        sendMessage(handshake);

        byte[] handshakeResponse = new byte[68];
        try {
            in.readFully(handshakeResponse);
        }
        catch (EOFException e) {
            throw new PeerException("The remote peer did not send a valid reply-handshake (bytes are missing)");
        }

        Utils.printlnLog("Handshake response: " + new String(handshakeResponse));

        MsgUtils.verifyHandShakeResponse(handshakeResponse, handshake, remotePeerID);
      } catch (SocketTimeoutException ex) {
            Utils.printLog("####### SocketTimeoutException ######");
            throw new PeerCommunicationException("Connection with peer is not alive!");
      }
    }

    /**
     * Respond to an incoming handshake message and complete
     * @return boolean     True if handshake was successful, else false
     */

    public boolean replyHandshake() throws Exception {
        // Read incoming handshake from remote peer
        byte[] incoming_handshake = new byte[68];
        try {
            in.readFully(incoming_handshake);
        }
        catch (EOFException e) {
            System.out.println("PeerException thrown");
            throw new PeerException("The remote peer did not send a valid initial-handshake (bytes are missing)");
        }

        Utils.printlnLog("Initial Handshake: " + new String(incoming_handshake));
        byte[] file_hash = Arrays.copyOfRange(incoming_handshake, 28, 48);

        // If incoming handshake is not valid, return false
        if (!Arrays.equals(Arrays.copyOfRange(incoming_handshake, 0, 20), MsgUtils.HANDSHAKE_HEAD)    // Verify first 20 bytes
            || !fm.isFileReadyToBeShared(file_hash)) {       // Verify file's hash is available at local
            System.out.println("Validation not passed");
            return false;
        }

        Utils.printlnLog("Handshake was good, sending reply");

        // Handshake was good, so set this remote peer's ID
        this.remotePeerID = new String(Arrays.copyOfRange(incoming_handshake, 48, 68));

        // Send reply handshake
        try {
            sendMessage(MsgUtils.buildHandshake(localPeerID, file_hash));
        }
        catch (Exception e) {
            throw new PeerCommunicationException("Connection with remote peer is dead");
        }

        return true;
    }


    /******* Receive and reply from remote *******/

    public Message receiveMessage() throws IOException, PeerCommunicationException {

        try {
            Utils.printLog("Start receiving message");
            byte[] bigendianLength = new byte[4];
            in.readFully(bigendianLength);
            int msgLength = MsgUtils.convertToInt(bigendianLength);
            Utils.printLog("=============> msgLength ==> " + msgLength);
            lastKeepAliveTime = System.currentTimeMillis();
            if (msgLength > 0) {
                byte[] msgBody = new byte[msgLength];
                in.readFully(msgBody);
                Utils.printLog("=============> msgType ==> " + msgBody[0]);
                return MsgUtils.parseMessage(ByteBuffer.wrap(msgBody));
            } else {
                Utils.printLog("####### KeepAlive message received! #######");
            }
        } catch (SocketTimeoutException ex) {
            Utils.printLog("####### SocketTimeoutException ######");
            throw new PeerCommunicationException("Connection with peer is not alive!");
        }
        return null;
    }


    /** Respond incoming messages **/
    public void respondMessages(FileManager fm) throws Exception {
        this.fm = fm;

        Message received_message;

        // Perform good handshake before anything else
        // Drop connection if handshake failed
        if (!replyHandshake())
            Utils.printlnLog("Connection to incoming peer dropped");

        /* Loop and keep replying any messages
        while ((received_message = peer.receiveMessage()) != null) {
            switch (received_message.getType()) {
                case MsgType.
            }
        }*/
    }


    /******* Send to remote *******/

    public void sendMessage(byte[] msgBytes) throws PeerException {
        try {
            this.out.write(msgBytes);
            this.out.flush();
        }
        catch (IOException e) {
            throw new PeerException("Error ocurred while trying to send message from peer" + remotePeerID);
        }
    }

    public void choke() throws Exception { 
        this.sendMessage(MsgUtils.buildChoke());
        this.remoteChoked = true;
    }

    public void unchoke() throws Exception { 
        this.sendMessage(MsgUtils.buildUnChoke());
        this.remoteChoked = false;
    }

    public void interested() throws Exception { 
        this.sendMessage(MsgUtils.buildInterested());
        this.localInterested = true;
    }

    public void uninterested() throws Exception { 
        this.sendMessage(MsgUtils.buildUninterested());
        this.localInterested = false;
    }

    public void sendBitfield(BitSet bitfield, int length) throws IOException, PeerException {
        this.sendMessage(MsgUtils.buildBitfield(bitfield, length));
    }

    public void sendRequest(Block block) throws Exception {
        this.sendMessage(MsgUtils.buildRequest(block));
    }

    public void sendRequest(int index, int offset, int length) throws Exception {
        this.sendMessage(MsgUtils.buildRequest(index, offset, length));
    }   

    public void sendPiece(Block block) throws Exception {
        this.sendMessage(MsgUtils.buildPiece(block));
    }

    public void sendPiece(int length, int index, int offset, byte[] data)throws Exception {
        this.sendMessage(MsgUtils.buildPiece(length, index, offset, data));
    }

    /**
     *  Getters, Setters and basic overrides
     */

    public void setLocalChoked(boolean localChoked) {
        this.localChoked = localChoked;
    }

    public void setRemoteInterested(boolean remoteInterested) {
        this.remoteInterested = remoteInterested;
    }


    @Override
    public String toString() {
        return "Peer{" +
                "id='" + remotePeerID + '\'' +
                ", ip='" + remoteIP + '\'' +
                ", port=" + remotePort +
                ", localInterested=" + localInterested +
                ", localChoked=" + localChoked +
                ", remoteInterested=" + remoteInterested +
                ", remoteChoked=" + remoteChoked +
                '}';
    }
}
