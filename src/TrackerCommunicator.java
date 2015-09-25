import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;

public class TrackerCommunicator {

	public static void main(String[] args) throws Exception, MalformedURLException {

		HashMap<String, String> parameters = new HashMap<String, String>();

		// Add parameters (hardcoded for now)
		String[] values = {"%56%D3%D5%C5%4F%A6%38%A5%3F%28%0F%C9%41%10%60%EF%26%2D%FE%B1", "0123456789abcdefghij", "6881","0", "0", "100", "started"};

		for (int i = 0; i < values.length; i++)
			parameters.put(TrackerCommunicator.PARAMETER_KEYS[i], values[i]);

		TrackerCommunicator tracker = 
			new TrackerCommunicator("http://128.6.171.130:6969/announce", parameters);

		System.out.println("Testing 1 - Send Http GET request");
		tracker.get();

	}

	public static final String[] PARAMETER_KEYS = {"info_hash", "peer_id", "port", "uploaded", "downloaded", "left", "event"};

	private final URL url;

	private Map<String, String> parameters = null;

	public TrackerCommunicator(String baseURL, String parameters) throws MalformedURLException {
		this.url = new URL(baseURL + "?" +  parameters);
	}

	public TrackerCommunicator(String baseURL, Map<String, String> parameters) 
			throws MalformedURLException, TrackerCommunicatorException {

		this.parameters = parameters;

		// Build the url
		String buffer = baseURL + "?";

		// Get only the parameters used in an HTTP GET request for the tracker
		// Any other key-value pairs passed will be ignored
		for (String key : PARAMETER_KEYS) {
			// If one of these returns null, throw exception (except for event)
			if (parameters.get(key) == null)
				throw new TrackerCommunicatorException("One of the required GET parameters is NULL");

			buffer += key + "=" + parameters.get(key) + "&";
		}

		// Remove the trailing '&'
		buffer = buffer.replaceAll(" &$", "");

		this.url = new URL(buffer);
	}

	// HTTP GET request
	public void get() throws Exception {

		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		System.out.println(response.toString());

	}
}