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
	
	/**
	 * Creates the test class.
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
	 * @throws Exception Thrown if the conditions to test the class cannot be met.
	 */
	private MulticastClientTest() throws Exception {
		final StringBuilder report = new StringBuilder();
		report.append("Initializing the test clients...");
		report.append(System.lineSeparator());
		MulticastClient clientA = null, clientB = null;
		
		try {
			// Initialize the test clients.
			clientA = new MulticastClient();
			clientB = new MulticastClient();

			if (clientA.isClosed() || clientB.isClosed()) {
				throw new Exception("Clients were closed after being initialized; aborting tests.");
			}

			if (!clientB.listen()) {
				throw new Exception("Client B failed to start listening; aborting tests.");
			}

			// Run the test suite.
			report.append("Checking for basic connectivity (sending/receiving messages)");
			report.append(System.lineSeparator());
			report.append("\tPassed: ");
			report.append(this.checkConnectivity(clientA, clientB));
			report.append(System.lineSeparator());
			
			
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (clientA != null && clientB != null) {
				// Close the client connections after the tests have finished.
				clientA.close();
				clientB.close();
			}
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
		final Signal signal = new Signal();
		final ArrayList<String> received = new ArrayList<>(3);

		// Listen for incoming packets.
		clientB.addMessageListener(message -> {
			received.add(message);
			signal.set();
		});
		
		final String[] messages = { "Message 0", "Message 1", "Message 2" };
		
		try {
			// Send a message and check if it is received.
			clientA.send(messages[0]);
			signal.waitForTimeout(5000);
			if (!received.contains(messages[0])) {
				System.err.println("Client B failed to receive a message (5 second timeout).");
				return false;
			}

			// Stop the client from listening, and make sure it doesn't receive a message.
			if (!clientB.stopListening()) {
				System.err.println("Client B failed to stop listening.");
				return false;
			}
			clientA.send(messages[1]);
			signal.waitForTimeout(1000);
			if (received.contains(messages[1])) {
				System.err.println("Client B received a message when it shouldn't have.");
				return false;
			}

			// Start listening for messages again, and check to see if it still receives a message.
			if (!clientB.listen()) {
				System.err.println("Client B failed to start listening.");
				return false;
			}
			clientA.send(messages[2]);
			signal.waitForTimeout(5000);
			if (!received.contains(messages[2])) {
				System.err.println("Client B didn't receive a message after listening was re-enabled.");
				return false;
			}
		} catch (IOException | InterruptedException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
}