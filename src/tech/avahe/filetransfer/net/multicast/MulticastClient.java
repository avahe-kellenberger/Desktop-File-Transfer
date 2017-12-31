package tech.avahe.filetransfer.net.multicast;


import java.io.IOException;
import java.net.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Avahe
 */
public class MulticastClient {

    private static final String GROUP_ADDRESS = "224.0.0.17";
    private static final int PORT = 7899;
    private static final InetAddress INET_ADDRESS;

    // Initialize the InetAddress.
    static {
        InetAddress tempAddress = null;
        try {
            tempAddress = InetAddress.getByName(MulticastClient.GROUP_ADDRESS);
        } catch (UnknownHostException ex) {
            // If the host cannot be resolved, exit the program, as multicasting will not be accessible.
            ex.printStackTrace();
            System.exit(-1);
        }
        INET_ADDRESS = tempAddress;
    }

	private final MulticastSocket multicastSocket;
	private final CopyOnWriteArraySet<MulticastMessageListener> messageListeners;
	private Thread receiveThread;
	private final Object receiveThreadLock = new Object();
	private boolean listening = false;

    /**
     * Creates a new multicast client, which automatically joins
     * the multicast group {@link MulticastClient#PORT} and binds to port {@link MulticastClient#GROUP_ADDRESS}.
     * @throws IOException Thrown if an I/O exception occurs while creating the underlying MulticastSocket.
     */
    public MulticastClient() throws IOException {
		this.messageListeners = new CopyOnWriteArraySet<>();
		this.multicastSocket = new MulticastSocket(MulticastClient.PORT);
		this.multicastSocket.joinGroup(InetAddress.getByName(MulticastClient.GROUP_ADDRESS));
    }

    /**
     * Tells the client to start listening for incoming packets.
     * @return If the client has started listening after this method call.port
     * This method will return false if it was already listening for packets.
     */
    public boolean listen() {
        synchronized (this.receiveThreadLock) {
            if (this.receiveThread == null || !this.receiveThread.isAlive()) {
                this.listening = true;
                // Create a thread which listens for packets while the socket is open.
                this.receiveThread = new Thread(this::receive);
                // Start listening for messages.
                this.receiveThread.start();
            } else if (this.listening) {
                    return false;
            } else {
                this.listening = true;
            }
        }
        return true;
    }

	/**
	 * Receives datagram packets from the MulticastSocket.
	 */
	private void receive() {
		try {
            final byte[] buffer = new byte[4096];
			final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			do {
				this.multicastSocket.receive(packet);

				// Don't dispatch trailing packets if the thread was interrupted and null.
				synchronized (this.receiveThreadLock) {
					if (this.listening && this.receiveThread.isInterrupted()) {
						Thread.interrupted();
					}
				}
				if (this.listening) {
					// Notify the listeners of the incoming message.
					final String message = new String(packet.getData(), packet.getOffset(), packet.getLength());
                    MulticastMessage.notifyListeners(message, this.messageListeners);
				}
			} while (this.listening);
		} catch (IOException ex) {
			// Silently ignore the exception, as the loop will exit if the connection drops.
		} finally {
			this.receiveThread = null;
			this.listening = false;
		}
	}

    /**
     * Stops the client from listening to incoming packets.
	 *
	 * <p>This method will return false if the client was not listening for packets
	 * prior to this method being called.</p>
	 *
     * @return If the client successfully stopped listening.
     */
    public boolean stopListening() {
    	synchronized (this.receiveThreadLock) {
			if (this.receiveThread != null && this.receiveThread.isAlive() && this.listening) {
				this.receiveThread.interrupt();
				this.listening = false;
				return true;
			}
			return false;
		}
    }

    /**
     * Sends a message from the locally connected socket.
     * @param message The message to send.
     * @throws IOException Thrown if there is an error sending the message.
	 *
	 * @see DatagramSocket#send(DatagramPacket)
     */
    public void send(final String message) throws IOException {
    	final byte[] buffer = message.getBytes();
		this.multicastSocket.send(new DatagramPacket(buffer, buffer.length, MulticastClient.INET_ADDRESS, MulticastClient.PORT));
    }

    /**
     * Closes the client's connection.
	 * <p>This method will return false if the client was closed prior to this method being called.</p>
     * @return If the client was closed successfully.
     */
    public boolean close() {
		final boolean stoppedListening = this.stopListening();
		final boolean closedMulticast = !this.multicastSocket.isClosed();
		if (closedMulticast) {
			this.multicastSocket.close();
		}
		return stoppedListening || closedMulticast;
    }

	/**
	 * @return Whether the client is closed or not.
	 * @see MulticastSocket#isClosed()
	 */
	public boolean isClosed() {
    	return this.multicastSocket == null || this.multicastSocket.isClosed();
	}

	/**
	 * @return If the client is not closed and is listening for incoming messages.
	 * @see MulticastClient#isClosed()
	 */
	public boolean isListening() {
		if (this.isClosed()) {
			return false;
		}
		synchronized (this.receiveThreadLock) {
			return this.receiveThread != null && this.receiveThread.isAlive() && !this.receiveThread.isInterrupted();
		}
	}

    /**
     * Disables or enables datagrams from looping back to the local socket.
     * @param disable If the loopback mode should be disabled.
     * @throws SocketException Thrown if there is an error setting the socket flag.
	 *
	 * @see MulticastSocket#setLoopbackMode(boolean)
     */
    public void setLoopbackMode(final boolean disable) throws SocketException {
		this.multicastSocket.setLoopbackMode(disable);
    }

    /**
     * Adds a listener to the client, which is notified when a packet is received.
	 * <p>This method will return false if the listener existed before this method was called.</p>
     * @param listener The listener to add.
     * @return If the listener was added successfully.
     */
    public boolean addMessageListener(final MulticastMessageListener listener) {
    	return this.messageListeners.add(listener);
    }

    /**
     * Checks if a message listener has been added to the client.
	 * <p>This method will return true if the listener existed before this method was called.</p>
     * @param listener The listener to check for.
     * @return If the client contains the listener.
     */
    public boolean containsMessageListener(final MulticastMessageListener listener) {
    	return this.messageListeners.contains(listener);
    }

    /**
     * Removes a message listener from the client.
	 * <p>This method will return false if the listener did not exist before this method was called.</p>
     * @param listener The listener to remove.
     * @return If the listener was removed successfully.
     */
    public boolean removeMessageListener(final MulticastMessageListener listener) {
    	return this.messageListeners.remove(listener);
    }

}