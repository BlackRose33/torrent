import java.util.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URI;	
import java.net.URLEncoder;
import java.io.*;

public class RUBTClient {

	public static void main(String[] args) throws BencodingException, IOException {

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

		TorrentInfo torrent = new TorrentInfo(data);
		byte[] info_hash = torrent.info_hash.array();

		System.out.println("URL of the tracker: " + torrent.announce_url.toString()
						 + "\nPiece length: " + torrent.piece_length
						 + "\nFile name: " + torrent.file_name
						 + "\nFile length: " + torrent.file_length
						 + "\npiece_hashes array length " + torrent.piece_hashes.length);

		String hash_hex = "";


		for (int i = 0; i < info_hash.length; i++) {
			hash_hex += "%" + String.format("%02X",info_hash[i]);
		}

		System.out.println();
		System.out.println(hash_hex);
	}
}