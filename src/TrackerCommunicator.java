import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.nio.ByteBuffer;
import java.io.*;

import exceptions.TrackerCommunicatorException;
import utils.Bencoder2;

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

    public final static ByteBuffer INTERVAL_KEY = ByteBuffer.wrap(new byte[]
    { 'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });

    public final static ByteBuffer PEERS_KEY = ByteBuffer.wrap(new byte[]
    { 'p', 'e', 'e', 'r', 's' });

    public final static ByteBuffer PEER_ID_KEY = ByteBuffer.wrap(new byte[]
    { 'p', 'e', 'e', 'r', ' ', 'i', 'd' });

    public final static ByteBuffer IP_KEY = ByteBuffer.wrap(new byte[]
    { 'i', 'p' });

    public final static ByteBuffer PORT_KEY = ByteBuffer.wrap(new byte[]
    { 'p', 'o', 'r', 't' });

	public static final String[] PARAMETER_KEYS = {"info_hash", "peer_id", "port", "uploaded", "downloaded", "left", "event"};

	private final URL url;

	private Map<String, String> parameters = null;

	public TrackerCommunicator(String baseURL, String parameters) throws MalformedURLException {
		this.url = new URL(baseURL + "?" +  parameters);
	}

	public TrackerCommunicator(String baseURL, Map<String, String> parameters) 
			throws MalformedURLException, TrackerCommunicatorException, UnsupportedEncodingException {

		this.parameters = parameters;

		// Build the url
		String buffer = baseURL + "?";

		// Get only the parameters used in an HTTP GET request for the tracker
		// Any other key-value pairs passed will be ignored
		for (String key : PARAMETER_KEYS) {
			// If one of these returns null, throw exception (except for event)
			if (parameters.get(key) == null)
				throw new TrackerCommunicatorException("One of the required GET parameters is NULL");

			buffer += key + "=" + parameters.get(key) + "&"; //TODO: encode URL properly
		}

		// Remove the trailing '&'
		buffer = buffer.replaceAll("&?$", "");

		this.url = new URL(buffer);
	}

	// HTTP GET request
	public Map<ByteBuffer,Object> get() throws Exception {

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		// optional default is GET
		connection.setRequestMethod("GET");

		int responseCode = connection.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		// Read and keep response in bytes
		byte[] response = getByteArray(connection);

		Map<ByteBuffer,Object> response_dictionary = (Map<ByteBuffer,Object>) Bencoder2.decode(response);

		// Get the list of peers (dictionaries)
		List<Map> peers_list = (List<Map>) response_dictionary.get(PEERS_KEY);

		System.out.println("Intervals: " + response_dictionary.get(INTERVAL_KEY));

		// Decode dictionaries and print info
		Iterator<Map> peers_iterator = peers_list.iterator();
		for (int i = 0; peers_iterator.hasNext(); i++) {

			Map<ByteBuffer, Object> peer_dictionary = peers_iterator.next();

			System.out.println(peer_dictionary);

			String peer_id = new String(((ByteBuffer) peer_dictionary.get(PEER_ID_KEY)).array(), "ASCII");
			String peer_ip = new String(((ByteBuffer) peer_dictionary.get(IP_KEY)).array(), "ASCII");

			System.out.println("Peer #" + i + ": ID - " + peer_id
							+ "  ,  IP - " + peer_ip + "  ,  PORT - " + peer_dictionary.get(PORT_KEY));
		}

		return response_dictionary;
	}

	public static byte[] getByteArray(HttpURLConnection connection) throws IOException{

		InputStream is = connection.getInputStream();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];  	//TODO: Is it sufficiently enough of memory?

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();

		return buffer.toByteArray();
	}
}


