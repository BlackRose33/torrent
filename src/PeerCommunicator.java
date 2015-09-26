import exceptions.PeerCommunicationException;
import utils.TorrentInfo;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.*;

/**
 * Created by nadiachepurko on 9/26/15.
 */

public class PeerCommunicator {

    public final static ByteBuffer INTERVAL_KEY = ByteBuffer.wrap(new byte[]
            { 'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });

    public final static ByteBuffer PEERS_KEY = ByteBuffer.wrap(new byte[]
            { 'p', 'e', 'e', 'r', 's' });

    public final static ByteBuffer PEER_ID_KEY = ByteBuffer.wrap(new byte[]
            { 'p', 'e', 'e', 'r', ' ', 'i', 'd' });

    public final static ByteBuffer IP_KEY = ByteBuffer.wrap(new byte[]
            { 'i', 'p' });

    public final static ByteBuffer PORT_KEY = ByteBuffer.wrap(new byte[]
            { 'p', 'o', 'r', 't' });

    private Map<ByteBuffer,Object> response_dictionary;
    private TorrentInfo torrent;
    private Peer peer;
    private String peerId;

    public PeerCommunicator(Map<ByteBuffer,Object> response_dictionary, TorrentInfo torrent, String peerId){
        this.response_dictionary = response_dictionary;
        this.torrent = torrent;
        peer = new Peer();
        this.peerId = peerId;
    }

    public Peer getPeer(){
        List<Map> peers_list = (List<Map>) response_dictionary.get(PEERS_KEY);

        Iterator<Map> peers_iterator = peers_list.iterator();
        for (int i = 0; peers_iterator.hasNext(); i++) {

            Map<ByteBuffer, Object> peer_dictionary = peers_iterator.next();

            System.out.println(peer_dictionary);

            String peer_id = new String(((ByteBuffer) peer_dictionary.get(PEER_ID_KEY)).array());
            String peer_ip = new String(((ByteBuffer) peer_dictionary.get(IP_KEY)).array());
            Integer peer_port = (Integer)peer_dictionary.get(PORT_KEY);

            if(peer_id.contains("RU")){
                peer.setPeerId(peer_id);
                peer.setPeerIP(peer_ip);
                peer.setPeerPort(peer_port);
                return peer;
            }
        }
        return null;
    }

    public void getFileFromPeer(Peer peer) throws Exception{

        if(peer == null){
            throw new PeerCommunicationException("Peer is null");
        }

        Socket socket = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        DataOutputStream outData = null;
        DataInputStream inData = null;

        try {
            socket = new Socket(peer.getIp(), peer.getPort());
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            outData = new DataOutputStream(outputStream);
            inData = new DataInputStream(inputStream);

            byte[] handshake = buildHandShake();
            outData.write(handshake);
            outData.flush();

            socket.setSoTimeout(2000);

            byte[] handshakeResponse = new byte[68];
            inData.readFully(handshakeResponse);

            System.out.println("\nHandshake response: " + new String(handshakeResponse));

            verifyHandShakeResponse(handshakeResponse, handshake, peer.getId());

        }finally{
            closeDataInputStream(inData);
            closeDataOutputStream(outData);
            closeSocket(socket);
        }



    }


    public boolean verifyHandShakeResponse(byte[] handshakeResponse, byte[] handshake, String peerIdExpected) throws Exception{

        if (!Arrays.equals(Arrays.copyOf(handshake, 48), Arrays.copyOf(handshakeResponse, 48))) {
            throw new PeerCommunicationException("Handshake response was not verified");
        }
        String peerIdActual = new String(Arrays.copyOfRange(handshakeResponse, 48, 68));
        if(!peerIdActual.equals(peerIdExpected)){
            throw new PeerCommunicationException("Peer names do not match in a handshake response");
        }
        return true;
    }

    public byte[] buildHandShake(){

        byte[] handshake = new byte[68];
        handshake[0] = (byte) 19;
        byte [] btname = new byte [] {'B','i','t','T','o','r','r','e','n','t',' ','p','r','o','t','o','c','o','l'};
        System.arraycopy(btname, 0, handshake, 1, 19);
        System.arraycopy(torrent.info_hash.array(), 0, handshake, 28, 20);
        System.arraycopy(peerId.getBytes(), 0, handshake, 48, 20 );
        return handshake;
    }

    public void closeSocket(Socket socket){
        if(socket != null){
            try {
                socket.close();
            }catch(Exception e){
                System.out.println(e);
            }
        }
    }

    public void closeDataOutputStream(DataOutputStream dataOutputStream){
        if(dataOutputStream != null){
            try {
                dataOutputStream.close();
            }catch(Exception e){
                System.out.println(e);
            }
        }
    }

    public void closeDataInputStream(DataInputStream dataInputStream){
        if(dataInputStream != null){
            try {
                dataInputStream.close();
            }catch(Exception e){
                System.out.println(e);
            }
        }
    }

}
