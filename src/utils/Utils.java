package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
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

    public static byte[] generateHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(data);
        return digest.digest();
    }
}
