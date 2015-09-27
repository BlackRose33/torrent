/**
 * Created by nadiachepurko on 9/26/15.
 */
public class Peer {

    private String id = "";
    private String ip = "";
    private int port = 0;

    public Peer(String peer_id, String ip, int port){
        this.id = peer_id;
        this.ip = ip;
        this.port = port;
    }

    public Peer(){
        super();
    }

    public void setPeerId(String peer_id){
        this.id = peer_id;
    }

    public void setPeerIP(String ip){
        this.ip = ip;
    }

    public void setPeerPort(int port){
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void printPeerData(){
        System.out.println("\n" + id + "\n" + ip + "\n" + port);
    }
}
