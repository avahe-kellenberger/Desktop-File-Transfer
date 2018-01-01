package net;

import tech.avahe.filetransfer.net.TCPClient;
import tech.avahe.filetransfer.net.multicast.MulticastClient;
import tech.avahe.filetransfer.net.multicast.MulticastMessage;
import tech.avahe.filetransfer.net.multicast.MulticastMessageAdapter;
import tech.avahe.filetransfer.net.multicast.MulticastMessageListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
		final String message = "1,clientB,127.0.1.1";
		for (String s : message.split(",", 2)) {
			System.out.println(s);
		}
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

			report.append("Testing sending and receiving MulticastMessages.");
			report.append(System.lineSeparator());
			report.append("\tPassed: ");
			report.append(this.testMulticastMessages(clientA, clientB));
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
		final MulticastMessageAdapter adapter = new MulticastMessageAdapter() {
			@Override
			public void onUnknownMessage(String message) {
				received.add(message);
				signal.set();
			}
		};
		clientB.addMessageListener(adapter);
		
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
		} finally {
			clientB.removeMessageListener(adapter);
		}
		return true;
	}

	/**
	 * TODO: Document
	 * @param clientA
	 * @param clientB
	 * @return
	 */
	private boolean testMulticastMessages(final MulticastClient clientA, final MulticastClient clientB) {
		final Signal signal = new Signal();

		final List<Byte> messageTypes = new ArrayList<>(MulticastMessage.values().length + 1);
		final Byte unknownIdentifier = (byte) -1;
		messageTypes.add(unknownIdentifier);
		for (final MulticastMessage m : MulticastMessage.values()) {
			messageTypes.add(m.getIdentifier());
		}

		final MulticastMessageListener listener = new MulticastMessageListener() {

			public void onIDShare(String nick, String ipAddress) {
				messageTypes.remove(MulticastMessage.ID_SHARE.getIdentifier());
				signal.set();
			}

			public void onIDRequest() {
				messageTypes.remove(MulticastMessage.ID_REQUEST.getIdentifier());
				signal.set();
			}

			public void onSendRequest(String ipAddress) {
				messageTypes.remove(MulticastMessage.SEND_REQUEST.getIdentifier());
				signal.set();
			}

			public void onSendRequestAccepted(String ipAddress, int port) {
				messageTypes.remove(MulticastMessage.SEND_REQUEST_ACCEPTED.getIdentifier());
				signal.set();
			}

			public void onSendRequestRejected(String ipAddress) {
				messageTypes.remove(MulticastMessage.SEND_REQUEST_REJECTED.getIdentifier());
				signal.set();
			}

			public void onUnknownMessage(String message) {
				messageTypes.remove(unknownIdentifier);
				signal.set();
			}

			public void onMessage(final String message) {
				System.out.println("Received: " + message);
			}
		};

		clientB.addMessageListener(listener);

		try {
			final String nick = "clientB";
			final String ip = TCPClient.LOCAL_ADDRESS;
			final int port = 1337;

			clientA.send(MulticastMessage.createIDShareMessage(nick, ip));
			signal.waitForTimeout(3000);
			if (messageTypes.contains(MulticastMessage.ID_SHARE.getIdentifier())) {
				System.err.println("Client did not receive a MulticastMessage#ID_SHARE message when expected.");
				return false;
			}

			clientA.send(MulticastMessage.createIDRequestMessage());
			signal.waitForTimeout(3000);
			if (messageTypes.contains(MulticastMessage.ID_REQUEST.getIdentifier())) {
				System.err.println("Client did not receive a MulticastMessage#ID_REQUEST message when expected.");
				return false;
			}

			clientA.send(MulticastMessage.createSendRequestMessage(ip));
			signal.waitForTimeout(3000);
			if (messageTypes.contains(MulticastMessage.SEND_REQUEST.getIdentifier())) {
				System.err.println("Client did not receive a MulticastMessage#SEND_REQUEST message when expected.");
				return false;
			}

			clientA.send(MulticastMessage.createSendRequestAcceptedMessage(ip, port));
			signal.waitForTimeout(3000);
			if (messageTypes.contains(MulticastMessage.SEND_REQUEST_ACCEPTED.getIdentifier())) {
				System.err.println("Client did not receive a MulticastMessage#SEND_REQUEST_ACCEPTED message when expected.");
				return false;
			}

			clientA.send(MulticastMessage.createSendRequestRejectedMessage(ip));
			signal.waitForTimeout(3000);
			if (messageTypes.contains(MulticastMessage.SEND_REQUEST_REJECTED.getIdentifier())) {
				System.err.println("Client did not receive a MulticastMessage#SEND_REQUEST_REJECTED message when expected.");
				return false;
			}

			clientA.send("Unknown message");
			signal.waitForTimeout(3000);
			if (messageTypes.contains(unknownIdentifier)) {
				System.err.println("Client did not receive a message of unknown type when expected.");
				return false;
			}

			return true;
		} catch (IOException ex) {
			System.err.println("Failed to send multicast messages - terminating test.");
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			System.err.println("Thread using Signal was unexpectedly interrupted - terminating test.");
			ex.printStackTrace();
		} finally {
			clientA.removeMessageListener(listener);
		}
		return false;
	}

}