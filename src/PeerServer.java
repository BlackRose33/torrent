import java.util.*;
import java.io.*;
import java.net.*;

import utils.*;
import model.*;

/*public enum Status {
	RUNNING,
	STOPPED
}*/

/**
 *  PeerServer: Serve as the listener for incoming connection from other peers
 *  When a new connection is received, span a new thread for it
 *  Keep track of connections and kill them in case client suddenly exits.
 */

public class PeerServer {

	private String id;
	private int port;

	private ServerSocket listener;

	// Keep track of active peers
	private List<PeerCommunicator> activePeers;

	// Current status
	//private Status status; 

	// FileManager that handles available files to upload
	FileManager fm;

	// Constructors
	public PeerServer(String peer_id, int port) {
		this.id = peer_id;
		this.port = port;

		// Use a vector for now
		this.activePeers = new Vector<PeerCommunicator>(10);

		//this.status = Status.STOPPED;

		// New FileManager with default values
		this.fm = new FileManager();
	}


	/**
	 *  Initialize listener and enter loop for connections
	 */
	public void startListener() throws IOException {
		this.listener = new ServerSocket(this.port);

        Runnable serverThread = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Waiting for clients to connect...");

                    while (true) {
                    	// Wait for connections
                        Socket newSocket = listener.accept();

                        // Init new peer and add it to our list
                        PeerCommunicator newCommunication = new PeerCommunicator(id, "", newSocket);
                        activePeers.add(newCommunication);

                        // Span new thread
                        Thread clientThread = new Thread(new ReceivedConnection(newCommunication, fm));
                        clientThread.start();
                    }
                } catch (Exception e) {
                    System.err.println("Unable to process incoming client");
                    e.printStackTrace();
                }
            }
        };

        // Start it up
        (new Thread(serverThread)).start();

        // Once everything's good, mark server as running
        //this.status = Status.RUNNING;
	}

	/**
	 *  ReceivedConnection: Handle what will happen for each incoming connection
	 */

	private class ReceivedConnection implements Runnable {
		private final PeerCommunicator peerComm;
		private final FileManager fm;

		private ReceivedConnection(PeerCommunicator peerComm, FileManager fm) {
			this.peerComm = peerComm;
			this.fm = fm;
		}

		@Override
		public void run() {
			System.out.println("New connection received");

			try {
				// Enter the peer loop to handle incoming messages
				peerComm.respondMessages(this.fm);
			}
			catch (Exception e) {
				System.out.println("Exception from peer");
				e.printStackTrace();
			}
		}
	}



	public static void main(String[] args) throws Exception {
		new PeerServer("remote78901234567890", 4444).startListener();
	}
}