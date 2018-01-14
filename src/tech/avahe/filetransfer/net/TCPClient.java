package tech.avahe.filetransfer.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author Avahe
 */
public class TCPClient {

    private Socket socket;

    /**
     * Attempts to connect to a host at the given address and port.
     *
     * @param ipAddress The IP address of the client to connect to.
     * @param port The port number to connect to.
     * @throws IOException If the connection was unsuccessful due to either an unknown host,
     * a preexisting connection, or if the host being connected to rejects the connection.
     */
    public void connect(final String ipAddress, final int port) throws IOException {
        this.socket = new Socket(ipAddress, port);
    }

    /**
     * @return If the client is connected.
     */
    public boolean isConnected() {
        return this.socket != null && this.socket.isConnected();
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

    /**
     * Attempts to send a message to the host.
     * @param message The message to send to the host to which this client is connected.
     * @throws IOException Thrown if the client is not connected to a host.
     */
    public void send(final String message) throws IOException {
        if (!this.isConnected()) {
            throw new IOException("Client is not connected.");
        }
        final byte[] messageBytes = message.getBytes();
        final int packetSize = 4096;
        final int messageSize = messageBytes.length;

        try (final OutputStream stream = this.socket.getOutputStream()) {
            for (int i = 0; i < messageSize; i += packetSize) {
                // TODO: Math is correct but there should be a better way of doing this.
                final int currentPacketSize = i + packetSize > messageSize ? messageSize - i : packetSize;
                stream.write(messageBytes, i, currentPacketSize);
            }
        }
    }

}