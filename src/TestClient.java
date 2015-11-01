import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

import model.*;
import utils.*;

public class TestClient {

    public static void main(String[] args)  throws Exception {
        int port = Integer.parseInt(args[0]);

    	try {
    		Socket socket = new Socket("127.0.0.1", port);
    		PeerCommunicator peer = new PeerCommunicator("local543210987654321", "remote78901234567890", socket);

			PrintWriter out =
				new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in =
				new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			BufferedReader stdIn =
				new BufferedReader(
					new InputStreamReader(System.in));

			String userInput;

			System.out.println("Initiating handshake");
			peer.initiateHandshake(Utils.URLEncodedHexToByteArray("%A7%D3%D5%C5%4F%A6%38%A5%3F%28%0B%C9%41%10%60%EF%26%2D%FE%B6"));
			System.out.println("Handshake was a SUCCESS!");

			while ((userInput = stdIn.readLine()) != null) {
				//if (userInput == "handshake\n") {
					System.out.println("Sending request");
					//peer.sendPiece(10, 20, 30, "abcdefghij".getBytes());
					peer.sendRequest(0, 0, 5);
					System.out.println(peer.receiveMessage().getBlock());
					
					peer.sendRequest(0, 5, 5);
					System.out.println(peer.receiveMessage().getBlock());
				/*}
				else {
				    out.println(userInput);
			    	System.out.println("echo: " + in.readLine());
			    }*/
			}
    	}

    	catch (IOException e) {
    		System.out.println("NOOOO x.x");
    	}


    }
}