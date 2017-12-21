package net;

import java.io.IOException;
import java.util.ArrayList;

import tech.avahe.filetransfer.net.MulticastClient;

/**
 * @author Avahe
 * 
 * Tests the MulticastClient class.
 */
public class MulticastClientTest {

	private boolean signaled = false;
	
	/**
	 * Creates the test class.
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		try {
			new MulticastClientTest();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Starts the tests for MulticastClient.java.
	 * @throws Exception 
	 * @throws IOException 
	 */
	private MulticastClientTest() throws Exception {
		final StringBuilder report = new StringBuilder();
		report.append("Initializing the test clients...");
		report.append(System.lineSeparator());
		
		try {
			// Initialize the test clients.
			final MulticastClient clientA = new MulticastClient();
			final MulticastClient clientB = new MulticastClient();
			if (!clientA.listen() || !clientB.listen()) {
				throw new Exception("Clients failed to start listening; aborting tests.");
			}
			
			// Run the test suite.
			report.append("Checking for basic connectivity (sending/receiving messages)");
			report.append(System.lineSeparator());
			report.append("\tPassed: ");
			report.append(this.checkConnectivity(clientA, clientB));
			report.append(System.lineSeparator());
			
			
			// Close the client connections after the tests have finished.
			clientA.close();
			clientB.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			System.out.println(report.toString());
		}
	}

	/**
	 * Tests sending and receiving <code>DatagramPackets</code> via <code>MulticastClient</code>.
	 * @param clientA A multicast client.
	 * @param clientB Another multicast client.
	 * @return If the connectivity test passed.
	 */
	private boolean checkConnectivity(final MulticastClient clientA, final MulticastClient clientB) {
		final ArrayList<String> received = new ArrayList<>(3);
		clientB.addPacketListener(packet -> {
			if (!received.isEmpty()) {
				this.setSignal();
			}
			received.add(new String(packet.getData(), 0, packet.getLength()));
		});
		
		final String[] messages = { "Message 0", "Message 1", "Message 2" };
		
		try {
			clientA.send(messages[0]);
			if (!clientB.stopListening()) {
				System.err.println("Client B failed to stop listening.");
				return false;
			}
			clientA.send(messages[1]);
			if (!clientB.listen()) {
				System.err.println("Client B failed to start listening.");
				return false;
			}
			clientA.send(messages[2]);
			this.waitForSignal();
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} finally {
			this.signaled = false;
		}
		return received.contains(messages[0]) && received.contains(messages[2]) && !received.contains(messages[1]);
	}
	
	/**
	 * Notifies the <code>signaled</code> boolean to be set to true,
	 * and notifies all threads waiting on the object.
	 */
	private synchronized void setSignal() {
		signaled = true;
        notifyAll();
    }
	
	/**
	 * Blocks the current thread until the <code>signaled</code> boolean is set to true.
	 * @throws InterruptedException See {@link Object#wait()}.
	 */
	private synchronized void waitForSignal() throws InterruptedException {
		while (!this.signaled) {
			wait();
		}
	}
	
}