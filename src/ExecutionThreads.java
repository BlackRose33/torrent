import model.*;
import utils.*;
import exceptions.*;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.net.*;

public class ExecutionThreads {

    /**
     * Class to be instantiated per thread of PeerCommunication
     */

    public static class PeerCommunicationThread implements Runnable {
        private final PeerCommunicator peerComm;
        private final SynchronousListOfPieces toDownload;
        private final SynchronousListOfPieces downloaded;
        private final String file_hash;

        // This value represents if the download algorithm is still pumping data into toDownload queue
        private final Boolean waitWhenToDownloadIsEmpty;

        public PeerCommunicationThread(
            PeerCommunicator peerComm, SynchronousListOfPieces toDownload,
            SynchronousListOfPieces downloaded, Boolean waitWhenToDownloadIsEmpty, String file_hash) {
            this.peerComm = peerComm;
            this.toDownload = toDownload;
            this.downloaded = downloaded;
            this.waitWhenToDownloadIsEmpty = waitWhenToDownloadIsEmpty;
            this.file_hash = file_hash;
        }

        @Override
        public void run() {
          try {
            System.out.println("Starting thread to peer: " + peerComm.remoteIP);

            // Perform handshake
            System.out.println("Initiating handshake");
			peerComm.initiateHandshake(Utils.URLEncodedHexToByteArray(file_hash));
			System.out.println("Handshake was a success");

			System.out.println("Starting download");
            // Keep looping until list is empty
            // If more blocks are incoming, wait until they all are loaded
            while (toDownload.size() > 0 || waitWhenToDownloadIsEmpty) {
                // If list is empty right now, wait a few milliseconds and try again
                if (toDownload.size() == 0) {
                    Thread.sleep(100);
                    System.out.println("Sleep 100ms");
                    continue;
                }

                // Pull the head of the list
                Piece piece = toDownload.poll();
                System.out.println("Pulled piece #" + piece.index);

                // Download the full piece
                if (downloadPiece(piece))
                    downloaded.add(piece);
                // If download fails, re-add it to head of list
                else
                    toDownload.addToHead(piece);
            }
          }
          catch (Exception e) {
          	 e.printStackTrace();
          }
        }

        private boolean downloadPiece(Piece piece) throws Exception {
            int block_length;

            // Request blocks for this piece
            for (int i = 0; i < piece.blockNumber; i++) {

                // If this is the last piece, last block might have a different length
                if (piece.lastPiece && (i == piece.blockNumber - 1))
                    block_length = piece.lastPieceLength % Block.BLOCK_LENGTH;
                else
                    block_length = Block.BLOCK_LENGTH;


                // Send Request
                System.out.println("Request sent for block# " + i);
                peerComm.sendRequest(piece.index, i * Block.BLOCK_LENGTH, block_length);
                
                // Receive block
                Message pieceMessage = peerComm.receiveMessage();
                if (pieceMessage == null) {
                    System.out.println("Received null instead of piece's block");
                }
                Block block = pieceMessage.getBlock();

                System.out.println(block);

                // Add it to the piece
                piece.addBlock(block);
            }

            // Once piece is complete, verify it
            byte[] actualHash = Utils.generateHash(piece.getData());
            /*if (Arrays.equals(piece.expectedHash, actualHash))
                // If equal, return success
                return true;
            else
                return false;*/

            peerComm.sendHave(piece.index);
            return true;
        }
    }


    /**
     * Class to wrap FileSaver execution
     */

    public static class FileSaverThread implements Runnable {

    	private FileSaver fs;

    	public FileSaverThread(FileSaver fs) {
    		this.fs = fs;
    	}

    	@Override
    	public void run() {
            try {
    		  fs.saveFile();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
    	}
    }


    /**
     * Class to wrap download algorithm execution
     */

    public static class DownloadAlgorithmThread implements Runnable {

    	private DownloadAlgorithm downloadAlgorithm;
    	private SynchronousListOfPieces toDownload;

    	// Details for the pieces
    	private int numberOfBlocks;

    	public DownloadAlgorithmThread(
    		DownloadAlgorithm downloadAlgorithm,
    		SynchronousListOfPieces toDownload,
    		int numberOfBlocks) {
    		this.downloadAlgorithm = downloadAlgorithm;
    		this.toDownload = toDownload;
    	}


    	@Override
    	public void run() {
    		// Load all pieces into the list
    		int nextPiece;

            System.out.println("Start loading pieces");
    		// End of pieces is marked as -1
    		while ((nextPiece = downloadAlgorithm.getNextPiece()) != -1) {
    			toDownload.add(new Piece(nextPiece, numberOfBlocks));
                System.out.println("Added piece index " + nextPiece);
    		}
    	}
    }


    public static void main(String[] args) throws Exception {
    	SynchronousListOfPieces toDownload = new SynchronousListOfPieces(new LinkedList<Piece>());

    	DownloadAlgorithmThread da_thread = new DownloadAlgorithmThread(
    		new DownloadAlgorithm(5), toDownload, 3);

    	Thread thread = new Thread(da_thread);
        thread.start();
    	thread.join();

    	System.out.println(toDownload);



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

        FileSaver fs = new FileSaver("testsave.txt", numberofpieces, list);

        new Thread(new FileSaverThread(fs)).start();

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
    }
}