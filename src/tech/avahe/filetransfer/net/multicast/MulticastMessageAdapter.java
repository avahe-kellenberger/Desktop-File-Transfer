package tech.avahe.filetransfer.net.multicast;

/**
 * @author Avahe
 */
public class MulticastMessageAdapter implements MulticastMessageListener {

    @Override
    public void onIDShare(String nick, String ipAddress) { }

    @Override
    public void onIDRequest() { }

    @Override
    public void onSendRequest(String ipAddress) { }

    @Override
    public void onSendRequestAccepted(String ipAddress, int port) { }

    @Override
    public void onSendRequestRejected(String ipAddress) { }

    @Override
    public void onDisconnect(String ipAddress) { }

    @Override
    public void onUnknownMessage(String message) { }

    @Override
    public void onMessage(final String message) { }

}