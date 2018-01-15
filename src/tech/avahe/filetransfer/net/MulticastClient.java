package tech.avahe.filetransfer.net;


import java.io.IOException;
import java.net.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * @author Avahe
 */
public class MulticastClient {

	private final InetAddress address;
	private final int port;
	private final MulticastSocket multicastSocket;

	private Thread listenerThread;
	private final Object listenerThreadLock = new Object();
	private boolean listening = false;
	private final CopyOnWriteArraySet<Consumer<String>> messageListeners;

    /**
     * Creates a new client, which automatically joins the given group address at the given port number.
     * @throws IOException Thrown if an I/O exception occurs while creating the underlying MulticastSocket.
     */
    public MulticastClient(final String groupAddress, final int port) throws IOException {
    	this.address = InetAddress.getByName(groupAddress);
    	this.port = port;
		this.messageListeners = new CopyOnWriteArraySet<>();
		this.multicastSocket = new MulticastSocket(this.port);
		this.multicastSocket.joinGroup(this.address);
    }

	/**
	 * Receives datagram packets from the MulticastSocket.
	 */
	private void listenForMessages() {
		try {
			final byte[] buffer = new byte[4096];
			final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			do {
				this.multicastSocket.receive(packet);
				if (this.listening) {
					this.clearThreadInterruptedState();
					this.notifyMessageListeners(new String(packet.getData(), packet.getOffset(), packet.getLength()));
				}
			} while (this.listening);
		} catch (IOException ex) {
			// Silently ignore the exception, as the loop will exit if the connection drops.
		} finally {
			this.listenerThread = null;
			this.listening = false;
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
			this.listenerThread = new Thread(this::listenForMessages);
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
     * Sends a message from the locally connected socket.
     * @param message The message to send.
     * @throws IOException Thrown if there is an error sending the message.
	 *
	 * @see DatagramSocket#send(DatagramPacket)
     */
    public void send(final String message) throws IOException {
    	final byte[] buffer = message.getBytes();
		this.multicastSocket.send(new DatagramPacket(buffer, buffer.length, this.address, this.port));
    }

    /**
     * Closes the client's connection.
     * @return If the client was closed prior to this method being called.
     */
    public boolean close() {
		this.stopListening();
		if (!this.multicastSocket.isClosed()) {
			this.multicastSocket.close();
			return true;
		}
		return false;
    }

	/**
	 * @return Whether the client is closed or not.
	 * @see MulticastSocket#isClosed()
	 */
	public boolean isClosed() {
    	return this.multicastSocket == null || this.multicastSocket.isClosed();
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
     * @param listener The listener to add.
     * @return If the listener was added successfully.
     */
    public boolean addMessageListener(final Consumer<String> listener) {
    	return this.messageListeners.add(listener);
    }

    /**
     * Checks if a message listener has been added to the client.
     * @param listener The listener to check for.
     * @return If the client contains the listener.
     */
    public boolean containsMessageListener(final Consumer<String> listener) {
    	return this.messageListeners.contains(listener);
    }

    /**
     * Removes a message listener from the client.
     * @param listener The listener to remove.
     * @return If the listener was removed successfully.
     */
    public boolean removeMessageListener(final Consumer<String> listener) {
    	return this.messageListeners.remove(listener);
    }

}