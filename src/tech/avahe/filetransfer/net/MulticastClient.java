package tech.avahe.filetransfer.net;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;


/**
 * @author Avahe
 */
public class MulticastClient {

	private static final String GROUP_ADDRESS = "224.0.0.17";
	private static final int PORT = 7899;
	private static final InetAddress INET_ADDRESS;

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
	private final CopyOnWriteArraySet<Consumer<DatagramPacket>> packetListeners;

	private final Object receiveThreadLock = new Object();
	private Thread receiveThread;

    /**
     * Creates a new multicast client, which automatically joins 
     * the multicast group {@link MulticastClient#PORT} and binds to port {@link MulticastClient#GROUP_ADDRESS}.
     * @throws IOException Thrown if an I/O exception occurs while creating the underlying MulticastSocket.
     */
    public MulticastClient() throws IOException {
		this.multicastSocket = new MulticastSocket(MulticastClient.PORT);
		this.multicastSocket.joinGroup(InetAddress.getByName(MulticastClient.GROUP_ADDRESS));
    	this.packetListeners = new CopyOnWriteArraySet<>();
    }

    /*
     * TODO: Create system for peer discovery.
     */
    
    /**
     * Tells the client to start listening for incoming packets.
     * @return If the client has started listening after this method call.port
     * This method will return false if it was already listening for packets.
     */
    public boolean listen() {
    	synchronized (this.receiveThreadLock) {
			if (this.receiveThread != null && this.receiveThread.isAlive() && !this.receiveThread.isInterrupted()) {
				return false;
			}
			// Create a thread which listens for packets while the socket is open.
			this.receiveThread = new Thread(this::receive);
			// Start listening for messages.
			this.receiveThread.start();
		}
    	return true;
    }

	/**
	 * Receives datagram packets from the MulticastSocket.
	 */
	private void receive() {
		try {
			boolean isReceiveThread = true;
			while (isReceiveThread) {
				final byte[] buffer = new byte[4096];
				final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				this.multicastSocket.receive(packet);

				// Don't dispatch trailing packets if the thread was interrupted and null.
				synchronized (this.receiveThreadLock) {
					isReceiveThread = this.receiveThread != null;
				}
				if (isReceiveThread) {
					// Notify the listeners of the incoming packet.
					this.packetListeners.forEach(listener -> listener.accept(packet));
				}
			}
		} catch (IOException ex) {
			// Silently ignore the exception,
			// as the loop will exit if the connection drops.
		}
	}
    
    /**
     * Stops the client from listening to incoming packets.
     * @return If the client successfully stopped listening.
     * This method will return false if the client was not listening for packets 
     * prior to this method being called.
     */
    public boolean stopListening() {
    	synchronized (this.receiveThreadLock) {
			if (this.receiveThread != null && this.receiveThread.isAlive() && !this.receiveThread.isInterrupted()) {
				this.receiveThread.interrupt();
				this.receiveThread = null;
				return true;
			}
			return false;
		}
    }
    
    /**
     * Sends a <code>DatagramPacket</code> from the locally connected socket.
     * @param message The message to send.
     * @throws IOException Thrown if there is an error sending the message (see {@link DatagramSocket#send(DatagramPacket)}).
     */
    public void send(final String message) throws IOException {
    	final byte[] buffer = message.getBytes();
		this.multicastSocket.send(new DatagramPacket(buffer, buffer.length, MulticastClient.INET_ADDRESS, MulticastClient.PORT));
    }
    
    /**
     * Closes the client's connection.
     * @return If the client was closed successfully.
     * This method will return false if the client was closed prior to this method being called.
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
     * This method will return false if the listener existed before this method was called.
     */
    public boolean addPacketListener(final Consumer<DatagramPacket> listener) {
    	return this.packetListeners.add(listener);
    }
    
    /**
     * Checks if a packet listener has been added to the client.
     * @param listener The packet listener.
     * @return If the client contains the packet listener.
     * This method will return true if the listener existed before this method was called.
     */
    public boolean containsPacketListener(final Consumer<DatagramPacket> listener) {
    	return this.packetListeners.contains(listener);
    }
    
    /**
     * Removes a packet listener from the client.
     * @param listener The listener to remove.
     * @return If the listener was removed successfully.
     * This method will return false if the listener did not exist before this method was called.
     */
    public boolean removePacketListener(final Consumer<DatagramPacket> listener) {
    	return this.packetListeners.remove(listener);
    }
    
}