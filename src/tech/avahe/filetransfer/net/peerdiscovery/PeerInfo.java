package tech.avahe.filetransfer.net.peerdiscovery;

/**
 * @author Avahe
 */
public class PeerInfo {

    public final String nickName;
    public final String ipAddress;
    public final int port;

    /**
     * Creates a data structure for a peer.
     * @param nickName The peer's nick name.
     * @param ipAddress The peer's IP address.
     * @param port The port number used to communicate with the peer.
     */
    public PeerInfo(final String nickName, final String ipAddress, final int port) {
       this.nickName = nickName;
       this.ipAddress = ipAddress;
       this.port = port;
    }

}