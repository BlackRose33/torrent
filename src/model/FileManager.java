package model;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.regex.*;

import utils.*;

public class FileManager {
	public static final String DEFAULT_FILES_LIST = "list.txt";
	public static final String DEFAULT_FILE_HASH_PATTERN = "^.*:.*$";

	private String file_list;
	private String file_hash_pattern;

	private RandomAccessFile file;
	private Pattern pattern;

	private Map<String,String> file_hash_map;

	public FileManager() {
		this.file_list = FileManager.DEFAULT_FILES_LIST;
		this.file_hash_pattern = FileManager.DEFAULT_FILE_HASH_PATTERN;
		init();
	}

	public FileManager(String list_of_supported_files, String file_hash_pattern) {
		this.file_list = list_of_supported_files;
		this.file_hash_pattern = file_hash_pattern;
		init();
	}

	private void init() {
		// Open the file to use and pattern
		try {
			this.file = new RandomAccessFile(this.file_list, "r");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		this.pattern = Pattern.compile(this.file_hash_pattern);

		// Init the map
		this.file_hash_map = new HashMap();
		String line;
		try {
			while ((line = this.file.readLine()) != null) {
				// Check line has proper format (file_name:file_hash)
				if (!pattern.matcher(line).matches()) continue;

				String[] file_and_hash = line.split(":");

				// Load name-hash pair to map
				this.file_hash_map.put(file_and_hash[1], file_and_hash[0]);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getFileName(String file_hash) throws IOException {
		return this.file_hash_map.get(file_hash);
	}

	public void addMapping(String file_hash, String file_name) {
		this.file_hash_map.put(file_hash, file_name);
	}

	public byte[] readBytes(String file_hash, int count, int offset) throws IOException {
		byte[] result = new byte[count];
		String file_name = getFileName(file_hash);
		RandomAccessFile inFile = null;

		try {
			inFile = new RandomAccessFile(file_name, "r");

			if (offset >= inFile.length()) return null;
			if ((inFile.length() - offset) < count) {
				int diff = (int) inFile.length() - offset;
				result  = new byte[diff];
			}

			// Read the thing
			inFile.seek(offset);
			if (inFile.read(result) == -1) return null;
		}
		catch (Exception e) {
			return null;
		}

		inFile.close();

		return result;
	}

	/**
	 * Verify that file is available to be uploaded on request
	 * @param file_hash  Hash which identifies the file (String in Hex, or raw byte[])
	 * @return boolean   True if file can be uploaded, else false
	 */

	public boolean isFileReadyToBeShared(String file_hash) throws IOException {
		boolean result = true;

		// Find file in the mapping
		String file_name = getFileName(file_hash);
		if (file_name == null) result = false;

		// Check that file can be opened
		try {
			RandomAccessFile inFile = new RandomAccessFile(file_name, "r");
			inFile.close();
		}
		catch (Exception e) {
			result = false;
		}

		System.out.println("isFileReadyToBeShared? " + result);
		return result;
	}

	public boolean isFileReadyToBeShared(byte[] file_hash) throws IOException {
		// Encode file_hash in Hex
		return isFileReadyToBeShared(Utils.toHex(file_hash));
	}

	/**
	 *  Read a block of length "length" from the selected file, starting at "offset"
	 *  If distance from offset to EOF is less than length, read up to EOF and return that.
	 *  @param file_hash  Byte array (or String in Hex) which identifies the file
	 *  @param offset     Point where to start reading from the file
	 *  @param length     How many bytes to read
	 *  @return  byte[]   Returns byte array which contains the requested block
	 */

	public byte[] getBlock(byte[] file_hash, int offset, int length) throws IOException {
		return readBytes(Utils.toHex(file_hash), length, offset);
	}

	public byte[] getBlock(String file_hash, int offset, int length) throws IOException {
		return readBytes(file_hash, length, offset);
	}

	/**
	 *  Overrides
	 */

	@Override
	public String toString() {
		String result = "";
		Set set = this.file_hash_map.entrySet();
		Iterator iterator = set.iterator();

		while (iterator.hasNext()) {
			Map.Entry current = (Map.Entry) iterator.next();
			result += "Key: " + current.getKey() + " --- Value: " + current.getValue() + "\n";
		}
		return result;
	}


	// For simple testing - To formalize&remove

	public static void main(String[] args) throws  IOException {
		FileManager fm = new FileManager();

		fm.addMapping("1256", "list.txt");
		int count = 50;
		// Read file and re-save it with a different name
		RandomAccessFile outFile = new RandomAccessFile("resaved.txt", "rw");

		System.out.println(fm.toString());

		byte[] read = null;
		int offset = 0;
		while ((read = fm.readBytes("1256", count, offset)) != null) {
			System.out.println("Readed bytes: " + read);
			outFile.write(read);
			offset += count;
		}

		outFile.close();
	}
}