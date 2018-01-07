package net;

import tech.avahe.filetransfer.net.MulticastClient;
import tech.avahe.filetransfer.threading.ThreadSignaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

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
			final String ipAddress = "224.0.0.17";
			final int port = 7899;
			clientA = new MulticastClient(ipAddress, port);
			clientB = new MulticastClient(ipAddress, port);

			if (clientA.isClosed() || clientB.isClosed()) {
				throw new Exception("Clients were closed after being initialized; aborting tests.");
			}
			if (!clientB.listen()) {
				throw new Exception("Client B failed to start listening; aborting tests.");
			}

			// Run the test suite.
			report.append("Checking for basic connectivity (sending/receiving messages)");
			report.append(System.lineSeparator());
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
	 * @param clientA A peerdiscovery client.
	 * @param clientB Another peerdiscovery client.
	 * @return If the connectivity test passed.
	 */
	private String checkConnectivity(final MulticastClient clientA, final MulticastClient clientB) {
		final StringBuilder report = new StringBuilder();
		final String lineSeparator = System.lineSeparator();
		final ArrayList<String> receivedMessages = new ArrayList<>(3);
		final ThreadSignaller signaller = new ThreadSignaller();

		// Listen for incoming packets.
		final Consumer<String> listener = message -> {
			receivedMessages.add(message);
			signaller.set();
		};

		clientB.addMessageListener(listener);
		
		final String[] messages = { "Message 0", "Message 1", "Message 2" };
		
		try {
			// Send a message and check if it is received.
			clientA.send(messages[0]);
			signaller.waitForTimeout(5000);
			report.append("Client received a message: ");
			report.append(receivedMessages.contains(messages[0]));
			report.append(lineSeparator);

			// Stop the client from listening, and make sure it doesn't receive a message.
			report.append("Client properly stopped listening: ");
			report.append(clientB.stopListening());
			report.append(lineSeparator);

			clientA.send(messages[1]);
			signaller.waitForTimeout(1000);
			report.append("Client properly not receiving a message: ");
			report.append(!receivedMessages.contains(messages[1]));
			report.append(lineSeparator);

			// Start listening for messages again, and check to see if it still receives a message.
			report.append("Client start listening as expected: ");
			report.append(clientB.listen());
			report.append(lineSeparator);

			clientA.send(messages[2]);
			signaller.waitForTimeout(5000);
			report.append("Client receiving a message after listening was re-enabled: ");
			report.append(receivedMessages.contains(messages[2]));
			report.append(lineSeparator);
		} catch (IOException | InterruptedException ex) {
			ex.printStackTrace();
		} finally {
			clientB.removeMessageListener(listener);
		}
		return report.toString();
	}

}