package model;
import java.util.*;
import java.io.*;
import java.net.*;

import utils.*;

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
	private List<Peer> activePeers;

	// Current status
	//private Status status; 

	// Constructors
	public PeerServer(String peer_id, int port) {
		this.id = peer_id;
		this.port = port;

		// Use a vector for now
		this.activePeers = new Vector<Peer>(10);

		//this.status = Status.STOPPED;
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
                        Peer newPeer = new Peer(newSocket);
                        activePeers.add(newPeer);

                        // Span new thread
                        Thread clientThread = new Thread(new ReceivedConnection(newPeer));
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
		private final Peer remotePeer;

		private ReceivedConnection(Peer remotePeer) {
			this.remotePeer = remotePeer;
		}

		@Override
		public void run() {
			System.out.println("New connection received");

			try {
				// Enter the peer loop to handle incoming messages
				remotePeer.respondMessages();
			}
			catch (IOException e) {
				System.out.println("Exception from peer");
			}
		}
	}



	public static void main(String[] args) throws Exception {
		new PeerServer("id", 4444).startListener();
	}
}