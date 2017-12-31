package tech.avahe.filetransfer.net.multicast;

/**
 * @author Avahe
 *
 * This interface is designed as a listener pair with the enum values of {@link MulticastMessage}.
 */
public interface MulticastMessageListener {

    /**
     * Called when a {@link MulticastMessage#ID_SHARE} message is received.
     * @param nick The nick name of the client which sent the message.
     * @param ipAddress The ip address of the client which sent the message.
     */
    void onIDShare(final String nick, final String ipAddress);

    /**
     * Called whenever a {@link MulticastMessage#ID_REQUEST} message is received.
     */
    void onIDRequest();

    /**
     * Called when the local client receives a {@link MulticastMessage#SEND_REQUEST} message.
     * @param ipAddress The ip address of the client wishing to send files.
     */
    void onSendRequest(final String ipAddress);

    /**
     * Called when the local client receives a {@link MulticastMessage#SEND_REQUEST_ACCEPTED} message
     * from a remote client, which was sent in reply to a {@link MulticastMessage#SEND_REQUEST} message.
     * @param ipAddress The ip address of the remote client.
     * @param port The port number of the remote client to connect to.
     */
    void onSendRequestAccepted(final String ipAddress, final int port);

    /**
     * Called when the local client receives a {@link MulticastMessage#SEND_REQUEST_REJECTED} message
     * from a remote client, which was sent in reply to a {@link MulticastMessage#SEND_REQUEST} message.
     * @param ipAddress The ip address of the remote client.
     */
    void onSendRequestRejected(final String ipAddress);

    /**
     * Called when a message is received that is not of any of the predefined {@link MulticastMessage} types.
     * @param message The raw message that was received.
     */
    void onUnknownMessage(final String message);

}