package model;
import java.nio.ByteBuffer;

/**
 * Created by nadiachepurko on 10/3/15.
 */

/*
All integer members in PWP messages are encoded as a 4-byte big-endian number.
Furthermore, all index and offset members in PWP messages are zero-based.

A PWP message has the following structure:

-----------------------------------------
| Message Length | Message ID | Payload |
-----------------------------------------
Message Length:
This is an integer which denotes the length of the message, excluding the length part itself.
If a message has no payload, its size is 1. Messages of size 0 MAY be sent periodically as keep-alive messages.

Message ID:
This is a one byte value, indicating the type of the message.

Payload:
The payload is a variable length stream of bytes.

*/

public class Message {
    private byte type;
    private Integer pieceIndex;
    private Block block;
    private ByteBuffer payload;

    public Message(byte type) {
        this.type = type;
    }

    public Message(byte type, ByteBuffer payload) {
        this.type = type;
        this.payload = payload;
    }

    public Message(byte type, Integer pieceIndex) {
        this.type = type;
        this.pieceIndex = pieceIndex;
    }

    public Message(byte type, Block block) {
        this.type = type;
        this.block = block;
        this.pieceIndex = block.getPieceIndex();
    }

    public byte getType() {
        return type;
    }

    public Integer getPieceIndex() {
        return pieceIndex;
    }

    public Block getBlock() {
        return block;
    }

    public ByteBuffer getPayload() { return payload; }

    public void setPayload(ByteBuffer payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;

        Message message = (Message) o;

        if (type != message.type) return false;
        if (block != null ? !block.equals(message.block) : message.block != null) return false;
        if (pieceIndex != null ? !pieceIndex.equals(message.pieceIndex) : message.pieceIndex != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type;
        result = 31 * result + (pieceIndex != null ? pieceIndex.hashCode() : 0);
        result = 31 * result + (block != null ? block.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", pieceIndex=" + pieceIndex +
                ", block=" + block +
                ", payload=" + payload +
                '}';
    }
}
