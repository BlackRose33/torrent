import exceptions.TrackerCommunicatorException;
import model.Peer;
import model.TorrentStats;
import utils.BencodingException;
import utils.TorrentInfo;
import utils.Utils;
import utils.Bencoder2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import static utils.Utils.printlnLog;

/** group 16
 */

public class RUBTClient {


	// The peer id of this client
	//private static final String PEER_ID_PREFIX = "nc409RUgmsohbx_";
	//private static final String PEER_ID = PEER_ID_PREFIX + System.currentTimeMillis();
	private static String peer_id;

	// The ports this client might use (range from lowest to highest)
	private static final String LOWEST_PORT_TO_USE = "6881";
	private static final String HIGHES_PORT_TO_USE = "6889";

	// How much this client has uploaded or downloaded to/from another peer
	private static String amount_uploaded = "0";
	private static String amount_downloaded = "0";

	// Amount left to download
	private static String amount_left = "0";

	// Event to report to tracker (default value is started)
	private static String event = "started";

	public static void main(String[] args) throws BencodingException, IOException, TrackerCommunicatorException, Exception {
   try{
		if(args.length != 2){
			printlnLog("Please, provide 2 arguments: torrent file and the file to save the data to");
			return;
		}

		String torrentFile = args[0];
		String toFile = args[1];

		byte[] data = readTorrentFile(torrentFile);
		File outFile = getFile(toFile);

		// Create torrent file parser object to get info from it
		TorrentInfo torrent = new TorrentInfo(data);
    // Generate peerID
    peer_id = generateClientID();
  
    // Set the amount of bytes left to download (report this to the tracker)
    amount_left = Integer.toString(torrent.file_length);

		// Convert the SHA1 hash to HEX  (need this to send it to the tracker)
	    String hash_hex = toHex(torrent.info_hash.array());
      printTorrentInfo(torrent, hash_hex);

      printlnLog("----------- Operations with tracker ----------");

      // HashMap to store GET parameters as key-value pairs
      HashMap<String, String> parameters = buildTrackerParameters(hash_hex);
   
      // Create new TrackerCommunicator
      TrackerCommunicator tracker = new TrackerCommunicator(torrent.announce_url.toString(), parameters);
   
      printlnLog("Starting download - Notify tracker and retrieve peers dictionary");
      Map<ByteBuffer,Object> peers_dictionary = (Map<ByteBuffer,Object>) Bencoder2.decode(tracker.announceStarted(amount_left));
   
      List<Peer> peers = tracker.getListOfPeers(peers_dictionary);
   
      // temporary use first peer from the list of chosen peers
      Peer peer = peers.get(0);
      printlnLog("First peer : " + peer);
    
      TorrentStats torrentStats = new TorrentStats(torrent, peer_id, outFile);
      PeerCommunicator pc = new PeerCommunicator(peer, torrentStats);
      pc.getFileFromPeer();
      // Notify tracker that we have completed the download
      // Once at this point, the file download has completed without issues, so we can notify 0 bytes left
      tracker.announceCompleted();
   } catch (Exception e) {
     Utils.printError("Error occurs : " + e.getMessage());
   }
  }


	static byte[] readTorrentFile(String torrentFile) throws IOException {
    try {
      Path path = Paths.get(torrentFile);
      return Files.readAllBytes(path);
    } catch (Exception e) {
      throw new RuntimeException("Can't read file torrent file : " + torrentFile);
    }
  }

  private static HashMap<String, String> buildTrackerParameters(String hash_hex) {
    HashMap<String, String> parameters = new HashMap<String, String>();

    // Parameters values
    String[] values = new String[]{
      hash_hex,                            // SHA1 Hash (in HEX and URL encoded)
      peer_id,                            // Peer ID of this client
    // PHASE 2: Change port to use after a port verification has been done
      LOWEST_PORT_TO_USE,                  // For now, port is irrelevant since we're only downloading
      amount_uploaded,
      amount_downloaded,
      amount_left,
      event
    };
    // Add key-value pairs to parameters hashmap
    for (int i = 0; i < values.length; i++)
      parameters.put(TrackerCommunicator.PARAMETER_KEYS[i], values[i]);
    return parameters;
  }
  
  private static String toHex(byte[] info_hash) {
    String hash_hex = "";
  
    for (int i = 0; i < info_hash.length; i++) {
      hash_hex += "%" + String.format("%02X",info_hash[i]);
    }
    return hash_hex;
  }

    /* create output file if possible */
	public static File getFile(String toFile) throws IOException{
			Path outPath = Paths.get(toFile);
			boolean exists = Files.exists(outPath);
			if (exists) {
				Utils.printLog("File already exists so overwrite : " + toFile);
				Files.delete(outPath);
			}
			Files.createFile(outPath);
			return outPath.toFile();
	}

	/* helper methods for testing */
	public static void printTorrentInfo(TorrentInfo torrent, String hash_hex){
		System.out.println("\n-------------- Operations with torrent file ----------\n");
		System.out.println(
				"URL of the tracker: " + torrent.announce_url.toString()
						+ "\nPiece length: " + torrent.piece_length
						+ "\nFile name: " + torrent.file_name
						+ "\nFile length: " + torrent.file_length
						+ "\npiece_hashes array length " + torrent.piece_hashes.length
						+ "\nSHA-1 hash value (in hex): " + hash_hex);
	}

	// Generate a random 20-byte string to use as ID
	// The result is an alphabetic string for simplicity
	public static String generateClientID() {
		String id = "";
		int id_length = 20;
		Random randomizer = new Random();

		for (int i = 0; i < 20; i++)
			id += (char) (randomizer.nextInt(25) + 97);      // 97 is 'a' is ASCII

		return id;
	}
}
