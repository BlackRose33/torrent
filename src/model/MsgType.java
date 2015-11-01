package model;


public enum MsgType {

	// MessageType (MessageID - first bit, payload length - bytes)
	CHOKE         (0, 0),
	UNCHOKE       (1, 0),
	INTERESTED    (2, 0),
	UNINTERESTED  (3, 0),
	HAVE          (4, 4),
	BITFIELD      (5, 0),
	REQUEST       (6, 12),
	PIECE         (7, 8),
	CANCEL        (8, 12);

	private final byte id;
	private final int length;

	MsgType(int id, int length) {
		this.id = (byte) id;
		this.length = length;
	}

	public byte id()       { return id; }
	public int  length()   { return length; }

	public static MsgType getID(byte id) {
		MsgType msgtype;
		switch(id) {
			case 0:
				msgtype = CHOKE;
				break;
			case 1:
				msgtype = UNCHOKE;
				break;
			case 2:
				msgtype = INTERESTED;
				break;
			case 3:
				msgtype = UNINTERESTED;
				break;
			case 4:
				msgtype = HAVE;
				break;
			case 5:
				msgtype = BITFIELD;
				break;
			case 6:
				msgtype = REQUEST;
				break;
			case 7:
				msgtype = PIECE;
				break;
			case 8:
				msgtype = CANCEL;
				break;
			default:
				msgtype = CHOKE;
		}

		return msgtype;
	}

	public static void main(String[] args) {
		MsgType msgtype = MsgType.PIECE;

		System.out.println(msgtype.id() + " - " + msgtype.length());
	}
}