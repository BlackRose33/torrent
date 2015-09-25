import java.util.*;

public class TrackerCommunicatorException extends Exception {
	
	public TrackerCommunicatorException(String message) {
		super("Error from TrackerCommunicator: " + message);
	}
}