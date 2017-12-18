package tech.avahe.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * @author Avahe
 */
public class MulticastClient {

    private MulticastSocket socket;

    /**
     * Creates a new multicast client which is automatically bound to the port.
     * @param port The port to bind to.
     * @throws IOException Thrown if an I/O exception occurs
     * while creating the MulticastSocket.
     */
    public MulticastClient(final int port) throws IOException {
        this.socket = new MulticastSocket(port);
    }

    /**
     * Connects to a group address, and automatically binds to an available port.
     * @param groupIP The IP address of the multicast group.
     * @return If the connection was created successfully.
     * @throws IOException Thrown if there is an error joining, or when the address 
     * is not a multicast address, or the platform does not support multicasting.
     */
    public boolean joinGroup(final String groupIP) throws IOException {
        this.socket.joinGroup(InetAddress.getByName(groupIP));
		return false;
    }

    // TODO: 
    
}