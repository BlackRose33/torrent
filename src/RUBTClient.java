import java.util.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URI;	
import java.net.URLEncoder;
import java.io.*;

public class RUBTClient {

	// The peer id of this client
	private static final String PEER_ID = "0123456789abcdefghij";

	// The ports this client might use (range from lowest to highest)
	private static final String LOWEST_PORT_TO_USE = "6881";
	private static final String HIGHES_PORT_TO_USE = "6889";

	// How much this client has uploaded or downloaded to/from another peer
	// Not making uploaded amount a final value because I think it'll be used in later phases
	private static String amount_uploaded = "0";
	private static String amount_downloaded = "0";

	// Amount left to download
	private static String amount_left = "0";

	// Event to report to tracker (default value is started)
	private static String event = "started";

	public static void main(String[] args) throws BencodingException, IOException, TrackerCommunicatorException, Exception {

		byte[] data = null;
		String path_to_file = args[0];

		try {
			// Create new file object for the input
			File input_file = new File(path_to_file);

			// Load the byte of the file into an array
			data = Files.readAllBytes(input_file.toPath());
		}

		catch (Exception e) {
			System.out.println("Bad");
		}

		// Create torrent file parser object to get info from it
		TorrentInfo torrent = new TorrentInfo(data);
		byte[] info_hash = torrent.info_hash.array();

		// Convert the SHA1 hash to HEX  (need this to send it to the tracker)
		String hash_hex = "";

		for (int i = 0; i < info_hash.length; i++) {
			hash_hex += "%" + String.format("%02X",info_hash[i]);
		}

		System.out.println("\n-------------- Operations with torrent file ----------\n");
		System.out.println(
			"URL of the tracker: " + torrent.announce_url.toString()
		  + "\nPiece length: " + torrent.piece_length
		  + "\nFile name: " + torrent.file_name
		  + "\nFile length: " + torrent.file_length
		  + "\npiece_hashes array length " + torrent.piece_hashes.length
		  + "\nSHA-1 hash value (in hex): " + hash_hex);


		/**
		 *  TRACKER OPERATIONS
		 */
		System.out.println("\n-------------- Operations with tracker ----------\n");

		// HashMap to store GET parameters as key-value pairs
		HashMap<String, String> parameters = new HashMap<String, String>();

		// Parameters values
		String[] values = {
			hash_hex,							// SHA1 Hash (in HEX and URL encoded)
			PEER_ID,							// Peer ID of this client
			// TO DO: Change port to use after a port verification has been done
			LOWEST_PORT_TO_USE,					// Port this client is/will using/use
			amount_uploaded.toString(),
			amount_downloaded.toString(),
			amount_left.toString(),
			event
		};

		// Add key-value pairs to parameters hashmap
		for (int i = 0; i < values.length; i++)
			parameters.put(TrackerCommunicator.PARAMETER_KEYS[i], values[i]);

		// Create new TrackerCommunicator
		TrackerCommunicator tracker = new TrackerCommunicator(torrent.announce_url.toString(), parameters);

		// Test this shit
		System.out.println("Testing 1 - Send Http GET request");
		tracker.get();


		System.out.println();
	}
}