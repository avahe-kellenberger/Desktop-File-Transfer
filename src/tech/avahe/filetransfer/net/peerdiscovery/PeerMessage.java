package tech.avahe.filetransfer.net.peerdiscovery;

/**
 * @author Avahe
 */
public class PeerMessage {

    /**
     * @author Avahe
     */
    public enum MessageType {
        PING("ping"),
        DISCONNECT("dc");

        private final String identifier;

        /**
         * Creates a new message type to be used for messaging peers.
         * @param identifier The identifier of the message.
         */
        MessageType(final String identifier) {
            this.identifier = identifier;
        }

        /**
         * @return The string representation of the <code>MessageType</code>.
         */
        public String getIdentifier() {
            return this.identifier;
        }

        /**
         * Finds the <code>MessageType</code> with the given identifier.
         * @param identifier The identifier of the message type.
         * @return the <code>MessageType</code> with the given identifier.
         * @see MessageType#getIdentifier()
         */
        protected static MessageType getByIdentifier(final String identifier) {
            for (final MessageType type : MessageType.values()) {
                if (type.getIdentifier().equals(identifier)) {
                    return type;
                }
            }
            return null;
        }

    }

    private static final String DELIMITER = ",";

    private final MessageType messageType;
    private final String ipAddress;
    private final String nickName;

    /**
     * Creates a standardized message with the given information.
     * @param messageType The type of message.
     * @param ipAddress The ip address of the client sending the message.
     * @param nickName The nick name of the client sending the message.
     */
    public PeerMessage(final MessageType messageType, final String ipAddress, final String nickName) {
        this.messageType = messageType;
        this.ipAddress = ipAddress;
        this.nickName = nickName;
    }

    /**
     * @return The type of message being sent.
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * @return The IP address of the client sending the message.
     */
    public String getIpAddress() {
        return this.ipAddress;
    }

    /**
     * @return The nick name of the client sending the message.
     */
    public String getNickName() {
        return this.nickName;
    }

    /**
     * Creates a standardized message with the given information.
     * @param messageType The type of message.
     * @param ipAddress The ip address of the client sending the message.
     * @param nickName The nick name of the client sending the message.
     * @return A standardized message.
     */
    public static String createFormattedMessage(final MessageType messageType, final String ipAddress, final String nickName) {
        return messageType.getIdentifier() + PeerMessage.DELIMITER + ipAddress + PeerMessage.DELIMITER + nickName;
    }

    /**
     * Creates a standardized message with the given information.
     * @param peerMessage The message being formatted into a String.
     * @return A standardized message.
     */
    public static String createFormattedMessage(final PeerMessage peerMessage) {
        return PeerMessage.createFormattedMessage(peerMessage.messageType, peerMessage.ipAddress, peerMessage.nickName);
    }

    /**
     * Parses a formatted message into a <code>PeerMessage</code>.
     * @param formattedMessage The formatted message being parsed.
     * @return A <code>PeerMessage</code> parsed from a raw formatted message.
     */
    public static PeerMessage parseFormattedMessage(final String formattedMessage) {
        final String[] parameters = formattedMessage.split(PeerMessage.DELIMITER);
        if (parameters.length != 3) {
            throw new IllegalArgumentException("Invalid message format.");
        }
        return new PeerMessage(MessageType.getByIdentifier(parameters[0]), parameters[1], parameters[2]);
    }

}