import java.util.*;
import java.io.*;

import exceptions.*;
import model.*;
import utils.*;

public class FileSaver {

	private String fileName;
	private int totalNumberOfPieces;
	private SynchronousListOfPieces downloaded;
	private int piece_size;

	private int savedPieces;
	private RandomAccessFile outFile;

	public FileSaver(String fileName, int totalNumberOfPieces, 
			SynchronousListOfPieces downloaded, int piece_size) throws FileNotFoundException {
		this.fileName = fileName;
		this.totalNumberOfPieces = totalNumberOfPieces;
		this.downloaded = downloaded;
		this.piece_size = piece_size;

		this.savedPieces = 0;

		// Open outFile
		outFile = new RandomAccessFile(fileName, "rw");
	}

	// Save all file
	// Pieces are saved in order
	public void saveFile() throws Exception {
		Piece piece = null;

		while (savedPieces < totalNumberOfPieces) {
			System.out.println("Next Piece to save: " + savedPieces);
			// Check we're getting pieces in order
			int i = 0;
			boolean foundNextPiece = false;
			do {
				try {
					piece = downloaded.getIndex(i);
				}
				// This only hits if queue is completely empty
				// Only wait until something is inserted
				catch (IndexOutOfBoundsException e) {
					System.out.println("Waiting pieces to save, 1 second");
					Thread.sleep(1000);
				}
				
				if (piece == null)
					continue;
				
				System.out.println("Piece found: " + piece.index);

				// If found the right piece, break
				if (piece.getIndex() == savedPieces) {
					foundNextPiece = true;
					System.out.println("Found it");
					break;
				}

				i++;
			}
			while (i < downloaded.size());

			// If piece was not found, wait 1 second and repeat
			if (!foundNextPiece) {
				System.out.println("Not found, waiting 50 ms for it");
				Thread.sleep(50);
				continue;
			}

			// Else, save the piece
			savePiece(piece, piece.index * piece_size);
			System.out.println("Saved this piece: " + piece.getData().length);
			downloaded.remove(i);
			savedPieces++;
		}

		// Close the file
		outFile.close();
	}

    // save piece to file in particular position with piece (block) offset
    private void savePiece(Piece piece, int offset) throws IOException {
        outFile.seek(offset);
        outFile.write(piece.getData());
    }


    public static void main(String[] args) throws FileNotFoundException, Exception {
    	Piece piece0 = new Piece(0, new byte[] {'a', 'b', 'c', 'd', 'e'});
    	Piece piece1 = new Piece(1, new byte[] {'0', '1', '2', '3', '4'});
    	final Piece piece2 = new Piece(2, new byte[] {'f', 'g', 'h', 'i', 'j'});
    	Piece piece3 = new Piece(3, new byte[] {'5', '6', '7', '8', '9'});
    	Piece piece4 = new Piece(4, new byte[] {'z', 'y', 'x', 'w', 'v'});

    	final SynchronousListOfPieces list = new SynchronousListOfPieces(new LinkedList<Piece>());

    	list.add(piece0);
    	list.add(piece4);
    	list.add(piece3);
    	list.add(piece1);
    	//list.add(piece2);

    	int numberofpieces = 5;

    	FileSaver fs = new FileSaver("testsave.txt", numberofpieces, list, 5);

    	Runnable delayInput = new Runnable() {
    		@Override
    		public void run() {
    			try {
    				Thread.sleep(3000);
    				list.add(piece2);
    			}
    			catch (Exception e) {
    				System.out.println("bad");
    			}
    		}
    	};

    	(new Thread(delayInput)).start();

    	fs.saveFile();
    }
}