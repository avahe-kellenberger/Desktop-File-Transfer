package tech.avahe.filetransfer.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * @author Avahe
 */
public class TCPClient {

    private Socket socket;

    private Thread listenerThread = new Thread(this::listen);
    private final Object listenerThreadLock = new Object();
    private boolean listening = false;
    private final CopyOnWriteArraySet<Consumer<String>> messageListeners = new CopyOnWriteArraySet<>();

    /**
     * Receives datagram packets from the MulticastSocket.
     */
    private void listen() {
        try (final InputStream inputStream = this.socket.getInputStream()) {
            final byte[] buffer = new byte[4096];
            do {
                final int numberOfBytesRead = inputStream.read(buffer);
                if (this.listening) {
                    this.clearThreadInterruptedState();
                    this.notifyMessageListeners(new String(buffer, 0, numberOfBytesRead));
                }
            } while (this.listening);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sets the listening thread's interrupted state to false.
     */
    private void clearThreadInterruptedState() {
        synchronized (this.listenerThreadLock) {
            if (this.listenerThread.isInterrupted()) {
                Thread.interrupted();
            }
        }
    }

    /**
     * Notifies the listeners with a message.
     * @param message The message to send to all the listeners.
     */
    private void notifyMessageListeners(final String message) {
        this.messageListeners.forEach(listener -> listener.accept(message));
    }

    /**
     * Tells the client to start listening for incoming packets.
     * @return If the client has started listening after this method call.port
     * This method will return false if it was already listening for packets.
     */
    public boolean startListening() {
        if (this.isListening()) {
            return false;
        }

        synchronized (this.listenerThreadLock) {
            this.listenerThread = new Thread(this::listen);
            this.listenerThread.start();
            this.listening = true;
        }
        return true;
    }

    /**
     * @return If the client is not closed and is listening for incoming messages.
     * @see MulticastClient#isClosed()
     */
    public boolean isListening() {
        if (this.isClosed()) {
            return false;
        }
        synchronized (this.listenerThreadLock) {
            return this.listenerThread != null && this.listenerThread.isAlive() && !this.listenerThread.isInterrupted() && this.listening;
        }
    }

    /**
     * Stops the client from listening to incoming packets.
     *
     * @return If the client was not listening for packets prior to this method being called.
     */
    public boolean stopListening() {
        synchronized (this.listenerThreadLock) {
            if (this.isListening()) {
                this.listenerThread.interrupt();
                this.listening = false;
                return true;
            }
            return false;
        }
    }

    /**
     * Attempts to send a message to the host.
     * @param message The message to send to the host to which this client is connected.
     * @throws IOException Thrown if the client is not connected to a host.
     */
    public void send(final String message) throws IOException {
        if (!this.isClosed()) {
            throw new IOException("Client is not connected.");
        }
        final byte[] messageBytes = message.getBytes();
        final int packetSize = 4096;
        final int messageSize = messageBytes.length;

        try (final OutputStream stream = this.socket.getOutputStream()) {
            for (int i = 0; i < messageSize; i += packetSize) {
                stream.write(messageBytes, i, Math.min(packetSize, messageSize - i));
            }
        }
    }

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

}