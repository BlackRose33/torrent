package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**group 16
 * Created by nadiachepurko on 10/3/15.
 */
public class Utils {

    private static final String LINE_BREAK = "\n";

    public static void printlnLog(String logMessage) {
        System.out.println(LINE_BREAK + logMessage);
    }

    public static void printLog(String logMessage) {
        System.out.println(logMessage);
    }
    
    public static void printError(String logMessage) {
        System.err.println(logMessage);
    }

    public static byte[] generateHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(data);
        return digest.digest();
    }

    public static String toHex(byte[] info_hash) {
        String hash_hex = "";
      
        for (int i = 0; i < info_hash.length; i++) {
          hash_hex += "%" + String.format("%02X",info_hash[i]);
        }

        return hash_hex;
    }

    /** Convert from "%AB%CD" to byte array **/
 
    public static byte[] URLEncodedHexToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 3];
        for (int i = 0; i < len; i += 3) {
            data[i / 3] = (byte) ((Character.digit(s.charAt(i+1), 16) << 4)
                                 + Character.digit(s.charAt(i+2), 16));
        }
        return data;
    }



    /******* Bitset methods ********/

    public static void markPieceNotCompleted(BitSet bitfield, int pieceIndex) {
        bitfield.clear(toBitSetIndex(pieceIndex, bitfield));
    }

    public static void markPieceCompleted(BitSet bitfield, int pieceIndex) {
        bitfield.set(toBitSetIndex(pieceIndex, bitfield));
    }

    /*private int toBitSetIndex(int pieceIndex) {
        return toBitSetIndex(pieceIndex, piecesCompleted.size());
    }*/

    public static int toBitSetIndex(int index, BitSet bitfield) {
        return Math.abs(bitfield.size() - index - 1);
    }
}
