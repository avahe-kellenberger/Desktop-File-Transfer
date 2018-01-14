package tech.avahe.filetransfer.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class TCPServer {

    private ServerSocket serverSocket;
    private Thread connectionAccepterThread;
    private final Object connectionAccepterLock = new Object();
    private boolean isAcceptingConnections = false;
    private final HashSet<Socket> connectedClients = new HashSet<>();

    private final CopyOnWriteArraySet<ConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();

    /**
     * Creates a new server which accepts incoming connections.
     * Once this server has been closed, it may not be reopened.
     *
     * @param listeningPort The port on which the server will be listening for incoming connections.
     * @throws IOException Thrown if there is an error opening a socket on the given port.
     */
    public TCPServer(final int listeningPort) throws IOException {
        this.serverSocket = new ServerSocket(listeningPort);
    }

    /**
     * Accepts incoming connections.
     * @return If the client was not already accepting incoming connections.
     */
    public boolean acceptIncomingConnections() {
        if (this.isAcceptingIncomingConnections()) {
            return false;
        }

        synchronized (this.connectionAccepterLock) {
            this.connectionAccepterThread = new Thread(this::incomingConnectionAccepter);
            this.connectionAccepterThread.start();
            this.isAcceptingConnections = true;
        }
        return true;
    }

    /**
     * Accept incoming client connections.
     */
    private void incomingConnectionAccepter() {
        try {
            do {
                synchronized (this.connectionAccepterLock) {
                    if (this.isAcceptingConnections && this.connectionAccepterThread.isInterrupted()) {
                        Thread.interrupted();
                    }
                }
                if (this.isAcceptingConnections) {
                    final Socket socket = this.serverSocket.accept();
                    this.connectedClients.add(socket);
                    this.notifyConnectionListeners(socket);
                }
            } while (this.isAcceptingConnections);
        } catch (IOException ex) {
            // Silently ignore the exception, as the loop will exit if the connection drops.
        } finally {
            this.connectionAccepterThread = null;
            this.isAcceptingConnections = false;
        }
    }

    /**
     * Notifies all connection listeners of newly connected sockets.
     * @param socket The newly connected socket.
     */
    private void notifyConnectionListeners(final Socket socket) {
        this.connectionListeners.forEach(connectionListener -> connectionListener.onConnectionEstablished(socket));
    }

    /**
     * @return If the server is currently accepting incoming connections.
     */
    public boolean isAcceptingIncomingConnections() {
        if (this.isClosed()) {
            return false;
        }
        synchronized (this.connectionAccepterLock) {
            return this.connectionAccepterThread != null &&
                    this.connectionAccepterThread.isAlive() &&
                    !this.connectionAccepterThread.isInterrupted() &&
                    this.isAcceptingConnections;
        }
    }

    /**
     * Stops the server from accepting incoming connections.
     *
     * @return If the server was not accepting incoming connections when the method was called.
     */
    public boolean stopAcceptingIncomingConnections() {
        synchronized (this.connectionAccepterLock) {
            if (this.isAcceptingIncomingConnections()) {
                this.connectionAccepterThread.interrupt();
                this.isAcceptingConnections = false;
                return true;
            }
            return false;
        }
    }

    /**
     * @return The clients which are currently connected to this client.
     */
    public Set<Socket> getConnectedClients() {
        return this.connectedClients;
    }

    /**
     * @return If the server has been closed.
     * @see ServerSocket#isClosed()
     */
    public boolean isClosed() {
        return this.serverSocket == null || this.serverSocket.isClosed();
    }

    /**
     * Closes the server connection.
     * @return If the server was closed prior to this method being called.
     * @throws IOException Thrown if the server is busy when closed.
     */
    public boolean close() throws IOException {
        this.stopAcceptingIncomingConnections();
        if (!this.isClosed()) {
            this.serverSocket.close();
            return true;
        }
        return false;
    }

    /**
     * Adds a listener to the server, which is notified when a new connection is establish with the server.
     * @param listener The listener to add.
     * @return If the listener was added successfully.
     */
    public boolean addConnectionListener(final ConnectionListener listener) {
        return this.connectionListeners.add(listener);
    }

    /**
     * Checks if a listener exists on the server, which is notified when a new connection is establish with the server.
     * @param listener The listener to check for.
     * @return If the client contains the listener.
     */
    public boolean containsConnectionListener(final ConnectionListener listener) {
        return this.connectionListeners.contains(listener);
    }

    /**
     * Removes a listener to the server, which is notified when a new connection is establish with the server.
     * @param listener The listener to remove.
     * @return If the listener was removed successfully.
     */
    public boolean removeConnectionListener(final ConnectionListener listener) {
        return this.connectionListeners.remove(listener);
    }

}