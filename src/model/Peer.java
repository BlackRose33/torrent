package model;

import exceptions.PeerCommunicationException;
import utils.MsgUtils;
import utils.Utils;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.BitSet;

/** group 16
 * Created by nadiachepurko on 10/3/15.
 */
public class Peer {

    private static final int KEEP_ALIVE_INTERVAL_MILLISECONDS = 120000; //2 minutes
    private String id;
    private String ip;
    private int port;

    private boolean peerInterested;
    private boolean peerChoked;

    private boolean meInterested;
    private boolean meChoked;

    // peer bitfield
    private BitSet bitfield;

    private Socket socket;
    private DataOutputStream outDataStream;
    private DataInputStream inDataStream;
    //last time in ms when any message has been received from peer
    private long lastKeepAliveTime;

    public Peer(String peer_id, String ip, int port) {
        this.id = peer_id;
        this.ip = ip;
        this.port = port;
        lastKeepAliveTime = System.currentTimeMillis();
    }

    public Message receiveMessage() throws IOException, PeerCommunicationException {
        try {
            Utils.printLog("Start receiving message");
            byte[] bigendianLength = new byte[4];
            inDataStream.readFully(bigendianLength);
            int msgLength = MsgUtils.convertToInt(bigendianLength);
            Utils.printLog("=============> msgLength ==> " + msgLength);
            lastKeepAliveTime = System.currentTimeMillis();
            if (msgLength > 0) {
                byte[] msgBody = new byte[msgLength];
                inDataStream.readFully(msgBody);
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

    public boolean isConnectionAlive() {
        long diffInMillis = System.currentTimeMillis() - lastKeepAliveTime;
        return diffInMillis < KEEP_ALIVE_INTERVAL_MILLISECONDS;
    }

    public void sendMessage(byte[] msgBytes) throws IOException {
        this.getOutDataStream().write(msgBytes);
        this.getOutDataStream().flush();
    }

    public boolean amIDownloadingFromPeer() { return meInterested && !meChoked; }

    public boolean amIUploadingToPeer() {
        return peerInterested && !peerChoked;
    }

    public void openConnection() throws IOException {
        socket = new Socket(this.getIp(), this.getPort());
        socket.setSoTimeout(KEEP_ALIVE_INTERVAL_MILLISECONDS);
        outDataStream = new DataOutputStream(socket.getOutputStream());
        inDataStream = new DataInputStream(socket.getInputStream());
    }

    public void closeConnection() {
        closeResource(socket);
        closeResource(inDataStream);
        closeResource(outDataStream);
    }

    public void closeResource(Closeable closeable){
        if(closeable != null){
            try {
                closeable.close();
            } catch(Exception e) {
                Utils.printlnLog(e.getMessage());
            }
        }
    }

    public void markBitfieldDownloaded(int pieceIndex) {
        bitfield.set(MsgUtils.toBitSetIndex(pieceIndex, bitfield.size()));
    }

    /* perform a handshake with this remote peer */
    public void makeHandshake(String clientPeerId, byte[] infoHash) throws Exception {
      try{
        byte[] handshake = MsgUtils.buildHandshake(clientPeerId, infoHash);
        this.sendMessage(handshake);

        byte[] handshakeResponse = new byte[68];
        this.getInDataStream().readFully(handshakeResponse);

        Utils.printlnLog("Handshake response: " + new String(handshakeResponse));

        MsgUtils.verifyHandShakeResponse(handshakeResponse, handshake, this.getId());
      } catch (SocketTimeoutException ex) {
            Utils.printLog("####### SocketTimeoutException ######");
            throw new PeerCommunicationException("Connection with peer is not alive!");
      }
    }

    /* send interested message*/
    public void sendInterested() throws IOException {
        this.sendMessage(MsgUtils.buildInterested());
        this.meInterested = true;
    }

    /* send bitfield message */
    public void sendBitfield(TorrentStats torrentStats) throws IOException {
        if (!torrentStats.isFileEmpty()) {
            this.sendMessage(MsgUtils.buildBitfield(torrentStats));
        }
    }

    private DataOutputStream getOutDataStream() {
        return outDataStream;
    }

    private DataInputStream getInDataStream() {
        return inDataStream;
    }

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setPeerInterested(boolean peerInterested) {
        this.peerInterested = peerInterested;
    }

    public void setPeerChoked(boolean peerChoked) {
        this.peerChoked = peerChoked;
    }

    public void setMeInterested(boolean meInterested) {
        this.meInterested = meInterested;
    }

    public void setMeChoked(boolean meChoked) {
        this.meChoked = meChoked;
    }

    public void setBitfield(ByteBuffer bitfield) {
        this.bitfield = BitSet.valueOf(bitfield);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", peerInterested=" + peerInterested +
                ", peerChoked=" + peerChoked +
                ", meInterested=" + meInterested +
                ", meChoked=" + meChoked +
                '}';
    }
}
