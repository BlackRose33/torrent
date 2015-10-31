package model;


public enum MsgType {

	// MessageType (MessageID - first bit, payload length - bytes)
	CHOKE         (0, 0),
	UNCHOKE       (1, 0),
	INTERESTED    (2, 0),
	UNINTERESTED  (3, 0),
	HAVE          (4, 4),
	BITFIELD      (5, 1),
	REQUEST       (6, 13),
	PIECE         (7, 9),
	CANCEL        (8, 13);

	private final byte id;
	private final int length;

	MsgType(int id, int length) {
		this.id = (byte) id;
		this.length = length;
	}

	public byte id()       { return id; }
	public int  length()   { return length; }

	public static void main(String[] args) {
		MsgType msgtype = MsgType.PIECE;

		System.out.println(msgtype.id() + " - " + msgtype.length());
	}
}