package model;

/** group 16
 * Created by nadiachepurko on 10/3/15.
 */
public interface MsgType {
    byte CHOKE = 0;
    byte UNCHOKE = 1;
    byte INTERESTED = 2;
    byte NOT_INTERESTED = 3;
    byte HAVE = 4;
    byte BITFIELD = 5;
    byte REQUEST = 6;
    byte PIECE = 7;
    byte CANCEL = 8;
}
