import java.util.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URI;
import java.net.URLEncoder;
import java.io.*;

public class Client {

	public static void main(String[] args) throws BencodingException, UnsupportedEncodingException {

		byte[] data = null;
		String path_to_file = argv[0];

		try {
			Path path = Paths.get(new URI(path_to_file));
			data = Files.readAllBytes(path);
		}

		catch (Exception e) {
			System.out.println("Bad");
		}

		TorrentInfo torrent = new TorrentInfo(data);
		byte[] info_hash = torrent.info_hash.array();

		System.out.println("URL of the tracker: " + torrent.announce_url.toString()
						 + "\nPiece length: " + torrent.piece_length
						 + "\nFile name: " + torrent.file_name
						 + "\nFile length: " + torrent.file_length
						 + "\nSHA-1 hash: " + torrent.info_hash.array()
						 + "\npiece_hashes array length " + torrent.piece_hashes.length);

		String hash_hex = "";


		for (int i = 0; i < info_hash.length; i++) {
			//Int single_byte = (int) info_hash[i];
			//if (!  (single_byte >= 65 && single_byte <= 90)
			//	|| (single_byte >= 97 )
			//hash_hex += "/" + String.format("%02X",info_hash[i]);
		}

		System.out.println();
		System.out.println(Client.toHexString(info_hash));
	}

	/** The Constant HEX_CHARS. */
	public static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * To hex string.
	 * 
	 * Converts a byte array to a hex string
	 *
	 * @param bytes the bytes
	 * @return the string
	 */
	public static String toHexString(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		
		if (bytes.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder(bytes.length * 3);

		for (byte b : bytes) {
			byte hi = (byte) ((b >> 4) & 0x0f);
			byte lo = (byte) (b & 0x0f);

			sb.append('%').append(HEX_CHARS[hi]).append(HEX_CHARS[lo]);
		}
		return sb.toString();
	}
}