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
    public void onUnknownMessage(String message) { }

}