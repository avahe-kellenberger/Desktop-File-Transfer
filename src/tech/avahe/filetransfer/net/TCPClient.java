package tech.avahe.filetransfer.net;

import tech.avahe.filetransfer.net.peerdiscovery.PeerInfo;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.BiConsumer;

/**
 * @author Avahe
 */
public class TCPClient {

    private Socket socket;
    private NetworkListener networkListener;

    /**
     * Attempts to send a message to the host.
     * @param message The message to send to the host to which this client is connected.
     * @throws IOException Thrown if the client is not connected to a host.
     */
    public void send(final String message) throws IOException {
        this.send(ByteBuffer.wrap(message.getBytes()));
    }

    /**
     * Sends data from the ByteBuffer to the connected server.
     * @param buffer The ByteBuffer of data to send.
     * @throws IOException Thrown if there is no connection.
     */
    public void send(final ByteBuffer buffer) throws IOException {
        if (!this.isClosed()) {
            throw new IOException("Client is not connected.");
        }
        this.socket.getChannel().write(buffer);
    }

    /**
     * Attempts to connect to a host at the given address and port.
     *
     * @param peerInfo The information of the peer to connect to.
     * @throws IOException If the connection was unsuccessful due to either an unknown host,
     * a preexisting connection, or if the host being connected to rejects the connection.
     */
    public void connect(final PeerInfo peerInfo) throws IOException {
        this.socket = new Socket(peerInfo.ipAddress, peerInfo.port);
        // TODO Channel is probably null because you need to use SocketChannel.open instead of Socket()
        final SocketChannel channel = this.socket.getChannel();
        this.networkListener = new NetworkListener() {
            protected void prepare() throws IOException {
                // TODO
            }
            protected SocketAddress read(ByteBuffer buffer) throws IOException {
                channel.read(buffer);
                return channel.getRemoteAddress();
            }
        };
    }

    /**
     * @return If the client is connected.
     */
    public boolean isClosed() {
        return this.socket != null && this.socket.isClosed();
    }

    /**
     * Closes the client's connection.
     * @throws IOException Thrown if the client is busy when closed.
     */
    public void close() throws IOException {
        if (this.socket != null) {
            this.socket.close();
        }
    }

    //region NetworkListener Delegation

    /**
     * Tells the client to start listening for incoming packets.
     * @param timeout The time (in milliseconds) to wait for the client to start listening for messages.
     * @return If the client has started listening after this method call.
     * This method will return false if it was already listening for packets.
     */
    public boolean startListening(final long timeout) throws InterruptedException {
        return this.networkListener.startListening(timeout);
    }

    /**
     * @return If the client is listening for incoming messages.
     */
    public boolean isListening() {
        return this.networkListener.isListening();
    }

    /**
     * Stops the client from listening to incoming data.
     */
    public void stopListening() {
        this.networkListener.stopListening();
    }

    /**
     * Stops the client from listening and waits for the listen thread to die.
     * @param timeout The time (in milliseconds) to wait for the listen thread to die.
     * @return Whether the listen thread died within the timeout.
     */
    public boolean stopListening(final int timeout) throws InterruptedException {
        return this.networkListener.stopListening(timeout);
    }

    /**
     * Adds a listener to the client, which is notified when data is received.
     * @param listener The listener to add.
     * @return If the listener was added successfully.
     */
    public boolean addDataListener(final BiConsumer<SocketAddress, ByteBuffer> listener) {
        return this.networkListener.addDataListener(listener);
    }

    /**
     * Checks if a message listener has been added to the client.
     * @param listener The listener to check for.
     * @return If the client contains the listener.
     */
    public boolean containsDataListener(final BiConsumer<SocketAddress, ByteBuffer> listener) {
        return this.networkListener.containsDataListener(listener);
    }

    /**
     * Removes a message listener from the client.
     * @param listener The listener to remove.
     * @return If the listener was removed successfully.
     */
    public boolean removeDataListener(final BiConsumer<SocketAddress, ByteBuffer> listener) {
        return this.networkListener.removeDataListener(listener);
    }

    //endregion

}