package tech.avahe.filetransfer.net;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import tech.avahe.filetransfer.util.Pair;

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

	/**
	 * <code>MessageType</code> is used to determine the message types
	 * of incoming multicast messages.
	 */
	public enum MessageType {
		/**
		 * A message intended to share the client's ID.
		 */
		SHARE_ID((byte) 0),
		/**
		 * A message to request <code>MessageType#SHARE_ID</code> messages from every client in the multicast group.
		 */
		REQUEST_ID((byte) 1),
		/**
		 * A message notifying other clients of an ID change.
		 */
		CHANGE_ID((byte) 2),
        /**
         * A request sent to a client to send files to said client.
         */
		REQUEST_TO_SEND((byte) 3),
        /**
         * A message accepting a <code>MessageType#REQUEST_TO_SEND</code> message.
         */
		ACCEPT_SEND_REQUEST((byte) 4);

		public static final String DELIMITER = ":";
		private final byte identifier;

		/**
		 * Used to send structured messages with given types, and to determine
		 * the types of messages being received.
		 *
		 * <p>Messages should be structured as:
		 * {@link MessageType#getIdentifier()}{@link MessageType#DELIMITER}message data
		 * </p>
		 *
		 * @param identifier The id used to determine the message type.
		 */
		MessageType(final byte identifier) {
			this.identifier = identifier;
		}

		/**
		 * @return The identifier of the <code>MessageType</code>.
		 */
		public byte getIdentifier() {
			return this.identifier;
		}

		/**
		 * Finds the <code>MessageType</code> that matches the identifier.
		 * @param identifier The identifier of the <code>MessageType</code>.
		 * @return The <code>MessageType</code> with the given identifier.
		 */
		public static MessageType getByIdentifier(final byte identifier) {
			for (final MessageType type : MessageType.values()) {
				if (type.getIdentifier() == identifier) {
					return type;
				}
			}
			return null;
		}

		/**
		 * Parses an incoming message.
		 *
		 * <p>The message will be split into a <code>Pair</code>, which has
		 * a key of <code>MessageType</code> and value of the message.</p>
		 *
		 * If the message does not have a correct identifier, the returned <code>MessageType</code>
		 * will be null. If the message itself is null, then null will be returned.
		 *
		 * @return The parsed message.
		 */
		public static Pair<MessageType, String> parseMessage(final String message) {
			if (message != null) {
				final String[] split = message.split(MessageType.DELIMITER, 2);
				if (split.length == 1) {
					return new Pair<>(null, message);
				}
				try {
                    return new Pair<>(MessageType.getByIdentifier(Byte.parseByte(split[0])), split[1]);
                } catch (NumberFormatException ex) {
                    return new Pair<>(null, message);
				}
			}
			return null;
		}

        /**
         * Creates a structured message with the given type and message data to send.
         * @param type The message type.
         * @param message The message to send.
         * @return The structured <code>String</code> representation of the message.
         */
		public static String createMessage(final MessageType type, final String message) {
            if (type == null || message == null) {
                return null;
            }
            return type.getIdentifier() + MessageType.DELIMITER + message;
        }
	}

	private final MulticastSocket multicastSocket;
	private final CopyOnWriteArraySet<Consumer<String>> messageListeners;
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
					this.messageListeners.forEach(listener -> listener.accept(message));
				}
			} while (this.listening);
		} catch (IOException ex) {
			// Silently ignore the exception,
			// as the loop will exit if the connection drops.
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
    public boolean addMessageListener(final Consumer<String> listener) {
    	return this.messageListeners.add(listener);
    }

    /**
     * Checks if a message listener has been added to the client.
	 * <p>This method will return true if the listener existed before this method was called.</p>
     * @param listener The listener to check for.
     * @return If the client contains the listener.
     */
    public boolean containsMessageListener(final Consumer<String> listener) {
    	return this.messageListeners.contains(listener);
    }

    /**
     * Removes a message listener from the client.
	 * <p>This method will return false if the listener did not exist before this method was called.</p>
     * @param listener The listener to remove.
     * @return If the listener was removed successfully.
     */
    public boolean removeMessageListener(final Consumer<String> listener) {
    	return this.messageListeners.remove(listener);
    }

}