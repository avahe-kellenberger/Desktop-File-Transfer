package tech.avahe.filetransfer.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * @author Avahe
 */
public class MulticastClient extends MulticastSocket {

	private final Thread receiveThread;
	private final CopyOnWriteArraySet<Consumer<DatagramPacket>> packetListeners;
	
    /**
     * Creates a new multicast client which is automatically bound to the port number.
     * @param port The port number to bind to.
     * @throws IOException Thrown if an I/O exception occurs
     * while creating the MulticastSocket.
     */
    public MulticastClient(final int port) throws IOException {
    	super(port);
    	this.packetListeners = new CopyOnWriteArraySet<>();
    	
    	// Listens for packets while the socket is open.
    	this.receiveThread = new Thread(() -> {
    		while(!this.isClosed()) {
    			final byte[] buffer = new byte[4096];
    			try {
					this.receive(new DatagramPacket(buffer, buffer.length));
				} catch (IOException ex) {
					// Silently ignore the exception, 
					// as the loop will exit if the connection drops.
				}
    		}
    		// The socket has been closed, and cannot be reopened.
    	});
    }

    /**
     * Tells the client to start listening for incoming packets.
     * @return If the client has started listening after this method call.
     * This method will return false if it was already listening for packets.
     */
    public boolean listen() {
    	if (this.receiveThread.isAlive()) {
    		return false;
    	}
    	this.receiveThread.start();
    	return true;
    }
    
    /**
     * Sends a <code>DatagramPacket</code> from the locally connected socket.
     * @param message The message to send.
     * @param group The group to send the message to.
     * @throws IOException Thrown if there is an error sending the message (see {@link DatagramSocket#send(DatagramPacket)}).
     */
    public void send(final String message, final InetAddress group) throws IOException {
    	final byte[] buffer = message.getBytes();
		super.send(new DatagramPacket(buffer, buffer.length, group, this.getLocalPort()));
    }
    
    @Override
    public void receive(final DatagramPacket packet) throws IOException {
    	super.receive(packet);
    	// Notify the listeners of the incoming packet.
    	this.packetListeners.forEach(listener -> {
    		listener.accept(packet);
    	});
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