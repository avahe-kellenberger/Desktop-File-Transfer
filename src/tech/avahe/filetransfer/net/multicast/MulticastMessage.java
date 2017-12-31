package tech.avahe.filetransfer.net.multicast;

import tech.avahe.filetransfer.util.Pair;

import java.util.Set;

/**
 * <code>MulticastMessage</code> is used to determine the message types
 * of incoming multicast messages.
 */
public enum MulticastMessage {
    /**
     * A message intended to share the client's ID.
     */
    ID_SHARE((byte) 0),
    /**
     * A message to request <code>MulticastMessage#ID_SHARE</code> messages from every client in the multicast group.
     */
    ID_REQUEST((byte) 1),
    /**
     * A request sent to a client to send files to said client.
     */
    SEND_REQUEST((byte) 2),
    /**
     * A message accepting a <code>MulticastMessage#SEND_REQUEST</code> message.
     */
    SEND_REQUEST_ACCEPTED((byte) 3),
    /**
     * A message rejecting a <code>MulticastMessage#SEND_REQUEST</code> message.
     */
    SEND_REQUEST_REJECTED((byte) 4);

    public static final String DELIMITER = ",";
    private final byte identifier;

    /**
     * Used to send structured messages with given types, and to determine
     * the types of messages being received.
     *
     * <p>Messages should be structured as:
     * {@link MulticastMessage#getIdentifier()}{@link MulticastMessage#DELIMITER}message data
     * </p>
     *
     * @param identifier The id used to determine the message type.
     */
    MulticastMessage(final byte identifier) {
        this.identifier = identifier;
    }

    /**
     * @return The identifier of the <code>MulticastMessage</code>.
     */
    public byte getIdentifier() {
        return this.identifier;
    }

    /**
     * Finds the <code>MulticastMessage</code> that matches the identifier.
     * @param identifier The identifier of the <code>MulticastMessage</code>.
     * @return The <code>MulticastMessage</code> with the given identifier.
     */
    public static MulticastMessage getByIdentifier(final byte identifier) {
        for (final MulticastMessage type : MulticastMessage.values()) {
            if (type.getIdentifier() == identifier) {
                return type;
            }
        }
        return null;
    }

    /**
     * Creates a structured message with the given type and message data to send.
     * @param type The message type.
     * @param messageArgs The message to send, in its parts.
     * @return The structured <code>String</code> representation of the message.
     */
    public static String createMessage(final MulticastMessage type, final String[] messageArgs) {
        final StringBuilder builder = new StringBuilder();
        if (messageArgs != null) {
            for (int i = 0; i < messageArgs.length; i++) {
                builder.append(messageArgs[i]);
                if (i < messageArgs.length - 1) {
                    builder.append(",");
                }
            }
        }
        return type == null ? builder.toString() : type.getIdentifier() + builder.length() > 0 ? MulticastMessage.DELIMITER + builder.toString() : "";
    }

    //region Message creators

    /**
     * Creates
     * @param nick The nick name of the local client.
     * @param ipAddress The IP address of the local client.
     * @return A standardized message to be sent to a <code>MulticastClient.</code>
     */
    public static String createIDShareMessage(final String nick, final String ipAddress) {
        return MulticastMessage.createMessage(ID_SHARE, new String[] { nick, ipAddress });
    }

    /**
     * Creates a message that requests all <code>MulticastClients</code> to send
     * a MulticastMessage{@link #ID_SHARE} message to the group.
     * @return A standardized message to be sent to a <code>MulticastClient.</code>
     */
    public static String createIDRequestMessage() {
        return MulticastMessage.createMessage(ID_REQUEST, null);
    }

    /**
     * Creates a message that is a request for a file transfer, from the local client to a remote client.
     * @param ipAddress The ip address of the local client.
     * @return A standardized message to be sent to a <code>MulticastClient.</code>
     */
    public static String createSendRequestMessage(final String ipAddress) {
        return MulticastMessage.createMessage(SEND_REQUEST, new String[] { ipAddress });
    }

    /**
     * Creates a message to notify a remote client that its MulticastMessage{@link #SEND_REQUEST} message was accepted,
     * and that the local client is ready to accept a connection.
     *
     * @param ipAddress The ip address of the client sending the message.
     * @param port The port number of the local client to connect to.
     * @return A standardized message to be sent to a <code>MulticastClient.</code>
     */
    public static String createSendRequestAcceptedMessage(final String ipAddress, final int port) {
        return MulticastMessage.createMessage(SEND_REQUEST_ACCEPTED, new String[] { ipAddress, String.valueOf(port) });
    }

    /**
     * Creates a message to notify a remote client that its MulticastMessage{@link #SEND_REQUEST} message was rejected.
     *
     * @param ipAddress The ip address of the client sending the message.
     * @return A standardized message to be sent to a <code>MulticastClient.</code>
     */
    public static String createSendRequestRejectedMessage(final String ipAddress) {
        return MulticastMessage.createMessage(SEND_REQUEST_REJECTED, new String[] { ipAddress });
    }

    //endregion

    /**
     * Parses an incoming message.
     *
     * <p>The message will be split into a <code>Pair</code>, which has
     * a key of <code>MulticastMessage</code> and a <code>String</code> value as the message.</p>
     *
     * If the message does not have a correct identifier, the returned <code>MulticastMessage</code>
     * will be null. If the message itself is null, then null will be returned.
     *
     * @return The parsed message.
     */
    public static Pair<MulticastMessage, String> splitMessage(final String message) {
        if (message != null) {
            final String[] split = message.split(MulticastMessage.DELIMITER, 2);
            if (split.length == 1) {
                return new Pair<>(null, message);
            }
            try {
                final MulticastMessage type = MulticastMessage.getByIdentifier(Byte.parseByte(split[0]));
                return type == null ? new Pair<>(null, message) : new Pair<>(type, split[1]);
            } catch (NumberFormatException ex) {
                return new Pair<>(null, message);
            }
        }
        return null;
    }

    /**
     * Notifies the listeners of the message with the proper parameters and type of message.
     * This method parses the message and hands it to the <code>MulticastMessageListeners</code>.
     * If a type cannot be determined, {@link MulticastMessageListener#onUnknownMessage(String)} is called.
     * @param rawMessage The raw message to parse.
     * @param listeners The listeners to notify of the incoming message.
     */
    public static void notifyListeners(final String rawMessage, final Set<MulticastMessageListener> listeners) {
        final Pair<MulticastMessage, String> split = MulticastMessage.splitMessage(rawMessage);
        if (split != null) {
            final MulticastMessage type = split.getKey();
            if (type == null) {
                listeners.forEach(listener -> listener.onUnknownMessage(split.getValue()));
            } else {
                final String[] args = split.getValue().split(MulticastMessage.DELIMITER);
                try {
                    switch (type) {

                        case ID_SHARE:
                            if (args.length == 2) {
                                final String nick = args[0];
                                final String ip = args[1];
                                listeners.forEach(listener -> listener.onIDShare(nick, ip));
                            } else {
                                System.err.println("Invalid message format.");
                            }
                            break;

                        case ID_REQUEST:
                            listeners.forEach(MulticastMessageListener::onIDRequest);
                            break;

                        case SEND_REQUEST:
                            if (args.length == 1) {
                                final String ip = args[0];
                                listeners.forEach(listener -> listener.onSendRequest(ip));
                            } else {
                                System.err.println("Invalid message format.");
                            }
                            break;

                        case SEND_REQUEST_ACCEPTED:
                            if (args.length == 2) {
                                final String ip = args[0];
                                final int port = Integer.parseInt(args[1]);
                                listeners.forEach(listener -> listener.onSendRequestAccepted(ip, port));
                            } else {
                                System.err.println("Invalid message format.");
                            }
                            break;

                        case SEND_REQUEST_REJECTED:
                            if (args.length == 1) {
                            final String ip = args[0];
                            listeners.forEach(listener -> listener.onSendRequestRejected(ip));
                        } else {
                            System.err.println("Invalid message format.");
                        }
                            break;

                    }
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid message format.");
                    ex.printStackTrace();
                }
            }
        }
    }

}