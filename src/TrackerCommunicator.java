import exceptions.TrackerCommunicatorException;
import model.Peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**group 16
 */

public class TrackerCommunicator {

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

	private String baseURL;

	private Map<String, String> parameters = null;

	private List<Peer> peers;
	private int interval;

	public TrackerCommunicator(String baseURL, String parameters) throws MalformedURLException {
		this.baseURL = baseURL + "?" +  parameters;
	}

	public TrackerCommunicator(String baseURL, Map<String, String> parameters)
			throws MalformedURLException, TrackerCommunicatorException, UnsupportedEncodingException {

		this.parameters = parameters;
		this.baseURL = baseURL;
	}

	/* HTTP GET request to a tracker. Returns response dictionary with information about peers
	Dictionary consist of the following keys:
	'failure reason' optional, 'interval', 'complete' optional, 'incomplete' optional, 'peers' (: peer id, ip, port)
	*/
	public byte[] get() throws Exception {
   try{
		// Build the url
		String buffer = this.baseURL + "?";

		// Get only the parameters used in an HTTP GET request for the tracker
		// Any other key-value pairs passed will be ignored
		for (String key : PARAMETER_KEYS) {
			// If one of these returns null, throw exception (except for event)
			if (this.parameters.get(key) == null)
				throw new TrackerCommunicatorException("One of the required GET parameters is NULL");

			buffer += key + "=" + this.parameters.get(key) + "&";
		}

		// Remove the trailing '&'
		buffer = buffer.replaceAll("&?$", "");

		URL url = new URL(buffer);

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		// optional default is GET
		connection.setRequestMethod("GET");

		int responseCode = connection.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		// Read and keep response in bytes
		byte[] response = getByteArray(connection);

		return response;
   } catch(SocketException e) {
    throw new TrackerCommunicatorException("Tracker connection error => " + e.getMessage());
   }
	}

    // Get list of peers from a dictionary response from the tracker
    public List<Peer> getListOfPeers(Map<ByteBuffer,Object> dictionary) throws Exception {
        // Get the list of peers (dictionaries)
        List<Map> peers_list = (List<Map>) dictionary.get(PEERS_KEY);

        interval = Integer.parseInt(dictionary.get(INTERVAL_KEY).toString());
        System.out.println("Intervals: " + interval);

        // Decode dictionaries and print info
        peers = new ArrayList<Peer>();
        Iterator<Map> peers_iterator = peers_list.iterator();
        for (int i = 0; peers_iterator.hasNext(); i++) {

            Map<ByteBuffer, Object> peer_dictionary = peers_iterator.next();

            String peer_id = new String(((ByteBuffer) peer_dictionary.get(PEER_ID_KEY)).array(), "ASCII");
            String peer_ip = new String(((ByteBuffer) peer_dictionary.get(IP_KEY)).array(), "ASCII");
            Integer peer_port = (Integer)peer_dictionary.get(PORT_KEY);

            addPeer(peer_id, peer_ip, peer_port);
        }

        return peers;
    }

    // Communicate to tracker that this download has just started
    // Left is the amount of bytes left to download
    public byte[] announceStarted(String left) throws Exception {
   		setParameter("event", "started");
		setParameter("left", left);
		return get();
    }

    // Communicate to tracker that download is complete
    public void announceCompleted() throws Exception {
    	setParameter("event", "completed");
		setParameter("left", "0");
		get();
    }

	/* add only specific peers to the list of peers */
	/* for Phase1 we must use peer with -RU prefix*/
	private void addPeer(String peer_id, String peer_ip, Integer peer_port) {
		if(peer_id.contains("RU")){
			Peer peer = new Peer(peer_id, peer_ip, peer_port);
			peers.add(peer);
		}
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

	public int getInterval() {
		return interval;
	}

	public List<Peer> getPeers() {
		return peers;
	}

	public void setParameter(String key, String value) {
		parameters.put(key, value);
	}
}


