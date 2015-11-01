import java.util.*;
import java.io.*;

public class DownloadAlgorithm {

	private int numberOfPieces;
	private int currentPiece = 0;

	public DownloadAlgorithm(int numberOfPieces) {
		this.numberOfPieces = numberOfPieces;
	}

	public int getNextPiece() {
		return (currentPiece < numberOfPieces) ? currentPiece++ : -1;
	}

	public static void main(String[] args) {
		DownloadAlgorithm algorithm = new DownloadAlgorithm(2);
		System.out.println(algorithm.getNextPiece());    // Should print 0
		System.out.println(algorithm.getNextPiece());	 // Should print 1
		System.out.println(algorithm.getNextPiece());    // Should print -1
	}
}