package tech.avahe.filetransfer.net.peerdiscovery;

import tech.avahe.filetransfer.common.Environment;
import tech.avahe.filetransfer.net.MulticastClient;
import tech.avahe.filetransfer.threading.ThreadSignaller;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Avahe
 */
public class PeerDiscoveryClient {

    private static final String GROUP_ADDRESS = "224.0.0.17";
    private static final int PORT = 7899;

    private final MulticastClient client;
    private Thread pingThread;
    private final ThreadSignaller pingThreadSignaller = new ThreadSignaller();

    private String nickName;

    private final Map<String, String> peers;
    private final CopyOnWriteArraySet<PeerListener> peerListeners;


    /**
     * Creates a new client which communicates with peers about their statuses.
     * This client keeps track of peers on the network, and broadcasts its own connection status.
     * @param nickName The client's nick name, to be used for messaging.
     * @throws IOException Thrown if the connection cannot be established.
     * @see MulticastClient#MulticastClient(String, int)
     */
    public PeerDiscoveryClient(final String nickName) throws IOException {
        this.nickName = nickName;
        this.peers = new HashMap<>();
        this.peerListeners = new CopyOnWriteArraySet<>();

        this.client = new MulticastClient(GROUP_ADDRESS, PORT);
        // Disable the loopback mode so the program will not receive its own messages.
        this.client.setLoopbackMode(true);
        this.client.addMessageListener(this::messageHandler);
        this.client.listen();
        this.startPinging();
    }

    /**
     * Sets the client's nick name.
     * This nick name is only used for sending messages to peers, and is not saved to the file system.
     *
     * @param nickName The client's new nick name.
     * @return If the nick name was changed.
     * This will return false if the parameterized name was the same as the current nick name.
     */
    public boolean setNickName(final String nickName) {
        if (!this.nickName.equals(nickName)) {
            this.nickName = nickName;
            return true;
        }
        return false;
    }

    /**
     * Updates the list of peers, and notifies all <code>PeerListeners</code>, based on received messages.
     * @param message The received message.
     */
    private void messageHandler(final String message) {
        final PeerMessage peerMessage = PeerMessage.parseFormattedMessage(message);
        final String ipAddress = peerMessage.getIpAddress();
        final String receivedNickName = peerMessage.getNickName();
        final String cachedNickName = this.peers.get(ipAddress);

        switch (peerMessage.getMessageType()) {
            case PING:
                if (cachedNickName == null) {
                    this.peerListeners.forEach(listener -> listener.onPeerConnected(ipAddress, receivedNickName));
                } else if (!cachedNickName.equals(receivedNickName)) {
                    this.peerListeners.forEach(listener -> listener.onPeerNickNameChange(ipAddress, receivedNickName, cachedNickName));
                }
                this.peers.put(ipAddress, receivedNickName);
                break;

            case DISCONNECT:
                if (cachedNickName != null) {
                    this.peerListeners.forEach(listener -> listener.onPeerDisconnected(ipAddress, receivedNickName));
                    this.peers.remove(ipAddress);
                }
                break;
        }
    }

    /**
     * Begin actively pinging the group.
     * @return If the client was not already sending ping messages.
     * @see PeerDiscoveryClient#pingContinuously()
     */
    private boolean startPinging() {
        if (this.isPinging()) {
            return false;
        }
        synchronized (this.pingThreadSignaller) {
            this.pingThread = new Thread(this::pingContinuously);
            this.pingThread.start();
        }
        return true;
    }

    /**
     * @return If the client is actively pinging.
     */
    private boolean isPinging() {
        synchronized (this.pingThreadSignaller) {
            return this.pingThread != null && this.pingThread.isAlive() && !this.pingThread.isInterrupted();
        }
    }

    /**
     * Continuously sends ping messages.
     */
    private void pingContinuously() {
        try {
            final String pingMessage = PeerMessage.createFormattedMessage(PeerMessage.MessageType.PING, Environment.LOCAL_ADDRESS, this.nickName);
            while (this.isPinging()) {
                this.client.send(pingMessage);
                this.pingThreadSignaller.waitForTimeout(1000);
            }
        } catch (Exception ex) {
            // Silently ignore the exception, as the loop will exit if the connection drops.
        } finally {
            synchronized (this.pingThreadSignaller) {
                this.pingThread = null;
            }
        }
    }

    /**
     * Stops the client from pinging the group.
     * @return If the client was pinging at the time of the method call.
     */
    private boolean stopPinging() {
        synchronized (this.pingThreadSignaller) {
            if (this.isPinging()) {
                this.pingThread.interrupt();
                this.pingThreadSignaller.set();
                this.pingThread = null;
                return true;
            }
            return false;
        }
    }

    /**
     * Closes the client's connection.
     * @return If the client was already closed at the time of this method call.
     * @see MulticastClient#close()
     */
    public boolean close() {
        if (!this.client.isClosed()) {
            this.stopPinging();
            // Notify the group that the client is disconnecting.
            final String disconnectMessage = PeerMessage.createFormattedMessage(PeerMessage.MessageType.DISCONNECT, Environment.LOCAL_ADDRESS, this.nickName);
            for (int i = 0; i < 3; i++) {
                try {
                    this.client.send(disconnectMessage);
                } catch (IOException ex) {
                    // Silently ignore issues sending close messages.
                }
            }
            this.peers.clear();
            return this.client.close();
        }
        return false;
    }

    /**
     * Disables or enables datagrams from looping back to the local socket.
     * Note: This is disabled by default, and should not be enabled except for testing purposes.
     *
     * @param disable If the loopback mode should be disabled.
     * @throws SocketException Thrown if there is an error setting the socket flag.
     *
     * @see MulticastClient#setLoopbackMode(boolean)
     */
    public void setLoopbackMode(final boolean disable) throws SocketException {
        this.client.setLoopbackMode(disable);
    }

    /**
     * Adds a <code>PeerListener</code> to the client.
     * @param listener The listener to add.
     * @return If the client did not already contain the listener.
     */
    public boolean addPeerListener(final PeerListener listener) {
        return this.peerListeners.add(listener);
    }

    /**
     * Returns if the client contains the <code>PeerListener</code>.
     * @param listener The listener being checked for.
     * @return If the client contains the listener.
     */
    public boolean containsPeerListener(final PeerListener listener) {
        return this.peerListeners.contains(listener);
    }

    /**
     * Removes a <code>PeerListener</code> from the client.
     * @param listener The listener being removed.
     * @return If the client contained the listener.
     */
    public boolean removePeerListener(final PeerListener listener) {
        return  this.peerListeners.remove(listener);
    }

    /**
     * Retrieves the currently active peers on the network as an unmodifiable map.
     *
     * <p>This map has the peer's IP address as the keys, and nick names as the values.
     * Note that this will always be empty if the <code>PeerDiscoveryListener</code> is null or has been closed.</p>
     *
     * @return The peers currently on the local network.
     */
    public Map<String, String> getPeersOnNetwork() {
        return Collections.unmodifiableMap(this.peers);
    }

}