package model;


public enum MsgType {

	// MessageType (MessageID - first bit, Payload length - bytes)
	// If Payload length is -x, means it's of variable length (x + ?)
	CHOKE         (0, 0),
	UNCHOKE       (1, 0),
	INTERESTED    (2, 0),
	UNINTERESTED  (3, 0),
	HAVE          (4, 4),
	BITFIELD      (5, -1),
	REQUEST       (6, 13),
	PIECE         (7, -9),
	CANCEL        (8, 13);

	private final byte id;
	private final int payloadLength;

	MsgType(int id, int payloadLength) {
		this.id = (byte) id;
		this.payloadLength = payloadLength;
	}

	private byte id()              { return id; }
	private int  payloadLength()   { return payloadLength; }

	public static void main(String[] args) {
		MsgType msgtype = MsgType.PIECE;

		System.out.println(msgtype.id() + " - " + msgtype.payloadLength());
	}
}