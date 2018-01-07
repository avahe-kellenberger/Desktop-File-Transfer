package tech.avahe.filetransfer.net.peerdiscovery;

/**
 * @author Avahe
 */
public interface PeerListener {

    /**
     * Notifies when a peer has connected to the group.
     * @param ipAddress The IP address of the peer.
     * @param nickName The peer's nick name.
     */
    void onPeerConnected(final String ipAddress, final String nickName);

    /**
     * Notifies when a peer changes nick names.
     * @param ipAddress The IP address of the peer.
     * @param newNickName The peer's new nick name.
     * @param oldNickName The peer's previous nick name.
     */
    void onPeerNickNameChange(final String ipAddress, final String newNickName, final String oldNickName);

    /**
     * Notifies when a peer has disconnected from the group, or whose connection has timed out.
     * @param ipAddress The IP address of the peer.
     * @param nickName The peer's nick name.
     */
    void onPeerDisconnected(final String ipAddress, final String nickName);

}