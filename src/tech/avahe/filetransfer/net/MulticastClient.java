package tech.avahe.filetransfer.net;


import java.io.IOException;
import java.net.*;

/**
 * @author Avahe
 */
public class MulticastClient extends NetworkListener {

	private final InetAddress address;
	private final int port;
	private final MulticastSocket multicastSocket;

    /**
     * Creates a new client, which automatically joins the given group address at the given port number.
     * @throws IOException Thrown if an I/O exception occurs while creating the underlying MulticastSocket.
     */
    public MulticastClient(final String groupAddress, final int port) throws IOException {
    	this.address = InetAddress.getByName(groupAddress);
    	this.port = port;
		this.multicastSocket = new MulticastSocket(this.port);
		this.multicastSocket.joinGroup(this.address);
    }

    @Override
	protected void listen() {
		try {
			final byte[] buffer = new byte[4096];
			final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			do {
				this.multicastSocket.receive(packet);
				if (this.shouldBeListening()) {
					this.clearThreadInterruptedState();
					this.notifyMessageListeners(new String(packet.getData(), packet.getOffset(), packet.getLength()));
				}
			} while (this.shouldBeListening());
		} catch (IOException ex) {
			// Silently ignore the exception, as the loop will exit if the connection drops.
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

}