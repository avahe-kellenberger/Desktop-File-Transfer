package tech.avahe.filetransfer.net;


import tech.avahe.filetransfer.util.Buffers;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author Avahe
 */
public class MulticastClient {

	private final InetAddress groupAddress;
	private final int port;
	private final InetSocketAddress groupSocketAddress;
	private final NetworkListener networkListener;

	private DatagramChannel datagramChannel;

    /**
     * Creates a new client, which automatically joins the given group address at the given port number.
     * @throws IOException Thrown if an I/O exception occurs while creating the underlying MulticastSocket.
     */
    public MulticastClient(final String groupAddress, final int port) throws IOException {
		this.groupAddress = InetAddress.getByName(groupAddress);
		this.port = port;
		this.groupSocketAddress = new InetSocketAddress(this.groupAddress, this.port);
		this.networkListener = new NetworkListener() {
			protected void prepare() throws IOException {
				if (!MulticastClient.this.datagramChannel.isOpen()) {
					MulticastClient.this.initChannel();
				}
			}
			protected SocketAddress read(ByteBuffer buffer) throws IOException {
				return MulticastClient.this.datagramChannel.receive(buffer);
			}
		};
		this.initChannel();
	}

	/**
	 * Initializes the internal datagram channel.
	 * @throws IOException Thrown if there is a configuration error.
	 */
	private void initChannel() throws IOException {
		this.datagramChannel = MulticastClient.createChannel(this.port);
		this.joinGroup();
	}

	/**
	 * Join multicast group on all network interfaces which support multicasting.
	 * @throws IOException Thrown if there is an error configuring the client channel,
	 * or if an error was thrown when joining the group.
	 */
	private void joinGroup() throws IOException {
		for (final NetworkInterface multicastNetworkInterface : MulticastClient.getMulticastNetworkInterfaces()) {
			this.datagramChannel.setOption(StandardSocketOptions.IP_MULTICAST_IF, multicastNetworkInterface);
			this.datagramChannel.join(this.groupAddress, multicastNetworkInterface);
		}
	}

	/**
	 * @return Whether the client is closed or not.
	 * @see MulticastSocket#isClosed()
	 */
	public boolean isClosed() {
		return !this.datagramChannel.isOpen();
	}

	/**
	 * Enables or disables data from looping back to the local socket.
	 * @param enable If the loopback mode should be enabled.
	 * @throws SocketException Thrown if there is an error setting the socket flag.
	 *
	 * @see MulticastSocket#setLoopbackMode(boolean)
	 */
	public void setLoopbackMode(final boolean enable) throws IOException {
		this.datagramChannel.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, enable);
	}

    /**
     * Sends a message to the locally connected socket.
     * @param message The message to send.
     * @throws IOException Thrown if there is an error sending the message.
	 *
	 * @see DatagramSocket#send(DatagramPacket)
     */
    public void send(final String message) throws IOException {
    	this.send(Buffers.toBuffer(message));
    }

	/**
	 * Sends data to the locally connected socket.
	 * @param data The data to send.
	 * @throws IOException Thrown if there is an error sending the data.
	 *
	 * @see DatagramSocket#send(DatagramPacket)
	 */
	public void send(final ByteBuffer data) throws IOException {
		this.datagramChannel.send(data, this.groupSocketAddress);
	}

    /**
     * Closes the client's connection.
     */
    public void close() {
		try {
			this.networkListener.stopListening();
			this.datagramChannel.close();
		} catch (IOException ex) {
			ex.printStackTrace();
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
	public boolean stopListening(final long timeout) throws InterruptedException {
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

    //region Static methods

	/**
	 * Creates a <code>DatagramChannel</code> to be used for mutlicasting.
	 * @param port The port on which to open the channel.
	 * @return The opened channel.
	 * @throws IOException Thrown if TODO:
	 */
	private static DatagramChannel createChannel(final int port) throws IOException {
		return DatagramChannel.open(StandardProtocolFamily.INET)
				.setOption(StandardSocketOptions.SO_REUSEADDR, true)
				.bind(new InetSocketAddress(port));
	}

	/**
	 * Gets all network interfaces that support multicasting.
	 */
	private static Set<NetworkInterface> getMulticastNetworkInterfaces() {
		try {
			final Enumeration<NetworkInterface> it = NetworkInterface.getNetworkInterfaces();
			final HashSet<NetworkInterface> networkInterfaces = new HashSet<>();
			while (it.hasMoreElements()) {
				final NetworkInterface networkInterface = it.nextElement();
				if (networkInterface.isUp() && networkInterface.supportsMulticast()) {
					networkInterfaces.add(networkInterface);
				}
			}
			return networkInterfaces;
		} catch (SocketException ignored) {
			return Collections.emptySet();
		}
	}

	/**
	 * Copies the packet to a byte array.
	 * @param packet The packet to copy.
	 * @return A byte array copy of the packet.
	 */
	private static byte[] toByteArray(final DatagramPacket packet) {
		final int length = packet.getLength();
		final byte[] copiedArray = new byte[length];
		System.arraycopy(packet.getData(), packet.getOffset(), copiedArray, 0, length);
		return copiedArray;
	}

	//endregion

}