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
		new MulticastClientTest();
	}

	/**
	 * Starts the tests for MulticastClient.java.
	 * @throws IOException 
	 */
	private MulticastClientTest() {
		final StringBuilder report = new StringBuilder();
		report.append("Initializing the test clients...");
		report.append(System.lineSeparator());
		
		try {
			// Initialize the test clients.
			final MulticastClient clientA = new MulticastClient();
			final MulticastClient clientB = new MulticastClient();
			clientA.listen();
			clientB.listen();
			
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
		final ArrayList<String> received = new ArrayList<>(1);
		clientB.addPacketListener(packet -> {
			received.add("Client B: " + new String(packet.getData(), 0, packet.getLength()));
			this.setSignal();
		});
		
		try {
			clientA.send("Hello world!");
			this.waitForSignal();
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} finally {
			this.signaled = false;
		}
		
		return received.contains("Client B: Hello world!");
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