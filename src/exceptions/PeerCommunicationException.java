package exceptions;

/**
 * Created by nadiachepurko on 9/26/15.
 */
public class PeerCommunicationException extends Exception {

    public PeerCommunicationException(String message) {
        super("Error from PeerCommunication: " + message);
    }
}
