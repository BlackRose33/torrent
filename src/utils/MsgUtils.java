package utils;

import exceptions.PeerCommunicationException;
import model.Block;
import model.Message;
import model.MsgType;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.*;

/** group 16
 * Created by nadiachepurko on 10/3/15.
 */
public class MsgUtils {

    private static final int MSG_LENGTH_BYTE_LENGTH = 4;
    private static final int MSG_ID_BYTE_LENGTH = 1;

    public static final byte[] HANDSHAKE_HEAD = new byte[] {(byte) 19,'B','i','t','T','o','r','r','e','n','t',' ','p','r','o','t','o','c','o','l'};

    /**
     * @param msgBody bytes of message without first big endian length
     * @return Message
     */
    public static Message parseMessage(ByteBuffer msgBody) {
        if (msgBody.hasRemaining()) {
            byte msgType = msgBody.get();
            ByteBuffer payload;

            switch (MsgType.getID(msgType)) {
                case HAVE:
                    payload = ByteBuffer.wrap(Arrays.copyOfRange(msgBody.array(), msgBody.position(), msgBody.capacity()));
                    int pieceIndex = payload.getInt();
                    return new Message(msgType, pieceIndex);
                case REQUEST:
                case PIECE:
                case CANCEL:
                    payload = ByteBuffer.wrap(Arrays.copyOfRange(msgBody.array(), msgBody.position(), msgBody.capacity()));
                    Block block = parseBlock(msgType, payload);
                    return new Message(msgType, block);
                case BITFIELD:
                    payload = ByteBuffer.wrap(Arrays.copyOfRange(msgBody.array(), msgBody.position(), msgBody.capacity()));
                    return new Message(msgType, payload);
                case UNCHOKE:
                case CHOKE:
                case INTERESTED:
                case UNINTERESTED:
                    return new Message(msgType);
                default:
                    return null;
            }
        }
        return null;
    }

    public static Block parseBlock(byte msgType, ByteBuffer msgPayload) {
        int pieceIndex = msgPayload.getInt();
        // second big endian integer
        int offset = msgPayload.getInt();

        switch (MsgType.getID(msgType)) {
            case PIECE:
                // starts after two big endian integers
                byte[] data = Arrays.copyOfRange(msgPayload.array(), msgPayload.position(), msgPayload.capacity());
                return new Block(pieceIndex, offset, data);
            case REQUEST:
            case CANCEL:
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

    public static byte[] buildKeepAlive() {
        ByteBuffer request = ByteBuffer.allocate(4);
        request.putInt(0);
        return request.array();
    }

    public static ByteBuffer build(MsgType msgType, int variableLength) {
        // Build head of packet
        // 4 bytes for biп endian msg length integer + 1 byte id + payload length + variable
        int totalLength = 1 + msgType.length() + variableLength;
        ByteBuffer message = ByteBuffer.allocate(MSG_LENGTH_BYTE_LENGTH + totalLength);
        message.putInt(totalLength);
        message.put(msgType.id());

        return message;
    }

    public static byte[] buildChoke() { return build(MsgType.CHOKE, 0).array(); }
    public static byte[] buildUnChoke() { return build(MsgType.UNCHOKE, 0).array(); }
    public static byte[] buildInterested() { return build(MsgType.INTERESTED, 0).array(); }
    public static byte[] buildUninterested() { return build(MsgType.UNINTERESTED, 0).array(); }

    public static byte[] buildHave(int pieceIndex) {
        ByteBuffer have = build(MsgType.HAVE, 0);
        have.putInt(pieceIndex);
        return have.array();
    }

    public static byte[] buildBitfield(BitSet bitfield, int length) {
        ByteBuffer message = build(MsgType.BITFIELD, length/8);
        message.put(bitfield.toByteArray());
        return message.array();
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
        ByteBuffer request = build(MsgType.REQUEST, 0);
        request.putInt(block.getPieceIndex());
        request.putInt(block.getOffset());
        request.putInt(block.getLength());
        return request.array();
    }

    public static byte[] buildRequest(int index, int offset, int length) {
        ByteBuffer request = build(MsgType.REQUEST, 0);
        request.putInt(index);
        request.putInt(offset);
        request.putInt(length);
        return request.array();
    }

    /**
     * Piece Message contains the piece as payload
     * It has this format
     * <index><begin><block>
     */

    public static byte[] buildPiece(Block block) {
        ByteBuffer message = build(MsgType.PIECE, block.getLength());
        message.putInt(block.getPieceIndex());
        message.putInt(block.getOffset());
        message.put(block.getData());
        return message.array();
    }

    public static byte[] buildPiece(int length, int index, int offset, byte[] data) {
        ByteBuffer message = build(MsgType.PIECE, length);
        message.putInt(index);
        message.putInt(offset);
        message.put(data);
        return message.array();
    }


    public static void verifyHandShakeResponse(byte[] handshakeResponse, byte[] handshake, String peerIdExpected) throws Exception{
        // Fail if both handshakes are completely the same
        if (!Arrays.equals(Arrays.copyOf(handshake, 48), Arrays.copyOf(handshakeResponse, 48))) {
            throw new PeerCommunicationException("Handshakes from both ends are completely the same");
        }
/*
        // Check format of the response handshake (verify is a valid handshake and peer is not trolling us)
        // First 48 bytes should be the same in both handshakes
        if (!Arrays.equals(Arrays.copyOfRange(handshake, 0, 48), Arrays.copyOfRange(handshakeResponse, 0, 48))) {
            throw new PeerCommunicationException("Response handshake has a non-valid format");
        }
*/
        // Last 20 bytes should be the remote peer's ID, verify it
        String peerIdActual = new String(Arrays.copyOfRange(handshakeResponse, 48, 68));
        if(!peerIdActual.equals(peerIdExpected)){
            throw new PeerCommunicationException("Expected PeerID does not match the one received.");
        }
    }



    public static void main(String[] args) {
        ByteBuffer message = ByteBuffer.wrap(buildPiece(5, 10, 20, "abcde".getBytes()));

        int length = message.getInt();
        byte id    = message.get();
        int  piece = message.getInt();
        int  offset = message.getInt();
        byte[] data_bytes = new byte[5];
        message.get(data_bytes);
        String data = new String(data_bytes);

        System.out.println("Length: " + length + " - ID: " + id + " - Piece: " + piece);
        System.out.println("Offset: " + offset + " - Data: " + data);
    }

}
