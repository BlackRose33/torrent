package utils;

import exceptions.PeerCommunicationException;
import model.Block;
import model.Message;
import model.MsgType;
import model.TorrentStats;

import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

import static utils.Utils.printLog;
import static utils.Utils.printlnLog;

/**
 * Created by nadiachepurko on 10/3/15.
 */
public class MsgUtils {

    private static final int MSG_LENGTH_BYTE_LENGTH = 4;
    private static final int MSG_ID_BYTE_LENGTH = 1;

    public static void main(String[] args) throws Exception, MalformedURLException {
        byte first = (byte) 0b00101000;
        byte second = (byte) 0b11111111;
        byte[] bytes = new byte[]{first, second};
        BitSet bitSet = BitSet.valueOf(bytes);

        byte[] bytes2 = bitSet.toByteArray();

        printlnLog("Input bitset  : " + bitSet);
        printlnLog("Input bitset cardinality  : " + bitSet.isEmpty());
        printlnLog("Input bitset size  : " + bitSet.size());
        printlnLog("Input bitset length  : " + bitSet.size());
        printlnLog("Input   : " + bytes.toString());
        printlnLog("Output  : " + bytes2.toString());
        printlnLog("Equals  : " + Arrays.equals(bytes, bytes2));

        String helloOrig = new String("hello!");
        byte[] bytesData = helloOrig.getBytes();
        String helloRestored = new String(bytesData);

        printLog("helloOrig         : " + helloOrig);
        printLog("helloRestored     : " + helloRestored);
        printLog("helloOrig equals helloRestored  : " + helloOrig.equals(helloRestored));

        ByteBuffer buffer = ByteBuffer.wrap(bytesData);
        String helloRestoredFromBuffer = new String(buffer.array());
        printLog("helloRestoredFromBuffer  : " + helloRestoredFromBuffer);

    }

    /**
     * @param msgBody bytes of message without first big endian length
     * @return Message
     */
    public static Message parseMessage(ByteBuffer msgBody) {
        if (msgBody.hasRemaining()) {
            byte msgType = msgBody.get();
            ByteBuffer payload;

            switch (msgType) {
                case MsgType.HAVE:
                    payload = ByteBuffer.wrap(Arrays.copyOfRange(msgBody.array(), msgBody.position(), msgBody.capacity()));
                    int pieceIndex = payload.getInt();
                    return new Message(msgType, pieceIndex);
                case MsgType.REQUEST:
                case MsgType.PIECE:
                case MsgType.CANCEL:
                    payload = ByteBuffer.wrap(Arrays.copyOfRange(msgBody.array(), msgBody.position(), msgBody.capacity()));
                    Block block = parseBlock(msgType, payload);
                    return new Message(msgType, block);
                case MsgType.BITFIELD:
                    payload = ByteBuffer.wrap(Arrays.copyOfRange(msgBody.array(), msgBody.position(), msgBody.capacity()));
                    return new Message(msgType, payload);
                case MsgType.UNCHOKE:
                case MsgType.CHOKE:
                case MsgType.INTERESTED:
                case MsgType.NOT_INTERESTED:
                    return new Message(msgType);
                default:
                    return null;
            }
        }
        return null;
    }

    public static Block parseBlock(int msgType, ByteBuffer msgPayload) {
        int pieceIndex = msgPayload.getInt();
        // second big endian integer
        int offset = msgPayload.getInt();

        switch (msgType) {
            case MsgType.PIECE:
                // starts after two big endian integers
                byte[] data = Arrays.copyOfRange(msgPayload.array(), msgPayload.position(), msgPayload.capacity());
                return new Block(pieceIndex, offset, data);
            case MsgType.REQUEST:
            case MsgType.CANCEL:
                // starts after two big endian integers
                int length = msgPayload.getInt();
                return new Block(pieceIndex, offset, length);
            default:
                return null;
        }
    }

    public static int convertToInt(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return bb.getInt();
    }

    /* build handshake byte array */
    public static byte[] buildHandshake(String peerId, byte[] infoHash) {
        byte[] handshake = new byte[68];
        handshake[0] = (byte) 19;
        byte [] btname = new byte [] {'B','i','t','T','o','r','r','e','n','t',' ','p','r','o','t','o','c','o','l'};
        System.arraycopy(btname, 0, handshake, 1, 19);
        System.arraycopy(infoHash, 0, handshake, 28, 20);
        System.arraycopy(peerId.getBytes(), 0, handshake, 48, 20);
        return handshake;
    }

    public static byte[] buildInterested() {
        return buildLengthAndId(0, MsgType.INTERESTED).array();
    }

    /*
    * Request message has ID 6 and a payload of length 12. The payload is 3 integer values indicating a block
    * within a piece that the sender is interested in downloading from the recipient.
    * The recipient MUST only send piece messages to a sender that has already requested it,
    * and only in accordance to the rules given above about the choke and interested states.
    * The payload has the following structure:
        ---------------------------------------------
        | Piece Index | Block Offset | Block Length |
        ---------------------------------------------
    * */
    public static byte[] buildRequest(Block block) {
        ByteBuffer request = buildLengthAndId(12, MsgType.REQUEST);
        request.putInt(block.getPieceIndex());
        request.putInt(block.getOffset());
        request.putInt(block.getLength());
        return request.array();
    }

    public static byte[] buildKeepAlive() {
        ByteBuffer request = ByteBuffer.allocate(4);
        request.putInt(0);
        return request.array();
    }

    public static byte[] buildHave(int pieceIndex) {
        ByteBuffer have = buildLengthAndId(4, MsgType.HAVE);
        have.putInt(pieceIndex);
        return have.array();
    }

    // TODO: send only needed amount of bytes but not all from BitSet
    public static byte[] buildBitfield(TorrentStats torrentStats) {
        ByteBuffer bitfield = buildLengthAndId(torrentStats.getPieceNumber(), MsgType.BITFIELD);
        bitfield.put(torrentStats.getBitfield());
        return bitfield.array();
    }

    /**
     * Piece Message contains the piece as payload
     * It has this format
     * <index><begin><block>
     */

    public static byte[] buildPiece(Block block) {
        ByteBuffer message = buildLengthAndId(8 + block.getLength(), MsgType.PIECE);
        message.putInt(block.getPieceIndex());
        message.putInt(block.getOffset());
        message.put(block.getData());
    }

    /* build length and id part of a message*/
    private static ByteBuffer buildLengthAndId(int payloadLength, byte id) {
        // payload + one byte for id
        int msgLength = payloadLength + MSG_ID_BYTE_LENGTH;
        // 4 bytes for biп endian msg length integer + itself msg length
        ByteBuffer bitfield = ByteBuffer.allocate(msgLength + MSG_LENGTH_BYTE_LENGTH);
        bitfield.putInt(msgLength);
        bitfield.put(id);
        return bitfield;
    }

    public static int toBitSetIndex(int index, int bitSetLength) {
        return Math.abs(bitSetLength - index - 1);
    }

    public static void verifyHandShakeResponse(byte[] handshakeResponse, byte[] handshake, String peerIdExpected) throws Exception{
        // Fail if both handshakes are completely the same
        if (!Arrays.equals(Arrays.copyOf(handshake, 48), Arrays.copyOf(handshakeResponse, 48))) {
            throw new PeerCommunicationException("Handshakes from both ends are completely the same");
        }

        // Check format of the response handshake (verify is a valid handshake and peer is not trolling us)
        // First 48 bytes should be the same in both handshakes
        if (!Arrays.equals(Arrays.copyOfRange(hanshake, 0, 48), Arrays.copyOfRange(handshakeResponse, 0, 48))) {
            throw new PeerCommunicationException("Response handshake has a non-valid format");
        }

        // Last 20 bytes should be the remote peer's ID, verify it
        String peerIdActual = new String(Arrays.copyOfRange(handshakeResponse, 48, 68));
        if(!peerIdActual.equals(peerIdExpected)){
            throw new PeerCommunicationException("Expected PeerID does not match the one received.");
        }
    }

}
