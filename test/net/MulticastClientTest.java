package net;

import tech.avahe.filetransfer.net.TCPClient;
import tech.avahe.filetransfer.net.multicast.MulticastClient;
import tech.avahe.filetransfer.net.multicast.MulticastMessage;
import tech.avahe.filetransfer.net.multicast.MulticastMessageAdapter;
import tech.avahe.filetransfer.net.multicast.MulticastMessageListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			report.append(this.checkConnectivity(clientA, clientB));

			report.append(System.lineSeparator());
			report.append(System.lineSeparator());

			report.append("Testing sending and receiving MulticastMessages.");
			report.append(System.lineSeparator());
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
	private String checkConnectivity(final MulticastClient clientA, final MulticastClient clientB) {
		final StringBuilder report = new StringBuilder();
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
			report.append("Client received a message: ");
			report.append(received.contains(messages[0]));
			report.append(System.lineSeparator());

			// Stop the client from listening, and make sure it doesn't receive a message.
			report.append("Client properly stopped listening: ");
			report.append(clientB.stopListening());
			report.append(System.lineSeparator());

			clientA.send(messages[1]);
			signal.waitForTimeout(1000);
			report.append("Client properly not receiving a message: ");
			report.append(!received.contains(messages[1]));
			report.append(System.lineSeparator());

			// Start listening for messages again, and check to see if it still receives a message.
			report.append("Client start listening as expected: ");
			report.append(clientB.listen());
			report.append(System.lineSeparator());

			clientA.send(messages[2]);
			signal.waitForTimeout(5000);
			report.append("Client receiving a message after listening was re-enabled: ");
			report.append(received.contains(messages[2]));
			report.append(System.lineSeparator());
		} catch (IOException | InterruptedException ex) {
			ex.printStackTrace();
		} finally {
			clientB.removeMessageListener(adapter);
		}
		return report.toString();
	}

	/**
	 * Tests the messaging protocol for MulticastClients.
	 * @param clientA A multicast client.
	 * @param clientB Another multicast client.
	 * @return If the messaging test passed.
	 */
	private String testMulticastMessages(final MulticastClient clientA, final MulticastClient clientB) {
		final StringBuilder report = new StringBuilder();
		final Signal signal = new Signal();

		final List<MulticastMessage> messageTypes = new ArrayList<>(MulticastMessage.values().length);
		for (final MulticastMessage m : MulticastMessage.values()) {
			messageTypes.add(m);
		}

		final MulticastMessageListener listener = new MulticastMessageListener() {

			public void onIDShare(String nick, String ipAddress) {
				messageTypes.remove(MulticastMessage.ID_SHARE);
				signal.set();
			}

			public void onIDRequest() {
				messageTypes.remove(MulticastMessage.ID_REQUEST);
				signal.set();
			}

			public void onSendRequest(String ipAddress) {
				messageTypes.remove(MulticastMessage.SEND_REQUEST);
				signal.set();
			}

			public void onSendRequestAccepted(String ipAddress, int port) {
				messageTypes.remove(MulticastMessage.SEND_REQUEST_ACCEPTED);
				signal.set();
			}

			public void onSendRequestRejected(String ipAddress) {
				messageTypes.remove(MulticastMessage.SEND_REQUEST_REJECTED);
				signal.set();
			}

			public void onDisconnect(String ipAddress) {
				messageTypes.remove(MulticastMessage.DISCONNECT);
				signal.set();
			}

			public void onUnknownMessage(String message) { }
			public void onMessage(final String message) { }

		};

		clientB.addMessageListener(listener);

		try {
			final String nick = "clientB";
			final String ip = TCPClient.LOCAL_ADDRESS;
			final int port = 1337;

			final HashMap<MulticastMessage, String> messageMap = new HashMap<>();
			messageMap.put(MulticastMessage.ID_SHARE, MulticastMessage.createIDShareMessage(nick, ip));
			messageMap.put(MulticastMessage.ID_REQUEST, MulticastMessage.createIDRequestMessage(ip));
			messageMap.put(MulticastMessage.SEND_REQUEST, MulticastMessage.createSendRequestMessage(ip));
			messageMap.put(MulticastMessage.SEND_REQUEST_ACCEPTED, MulticastMessage.createSendRequestAcceptedMessage(ip, port));
			messageMap.put(MulticastMessage.SEND_REQUEST_REJECTED, MulticastMessage.createSendRequestRejectedMessage(ip));
			messageMap.put(MulticastMessage.DISCONNECT, MulticastMessage.createDisconnectMessage(ip));

			for (final Map.Entry<MulticastMessage, String> entry : messageMap.entrySet()) {
				clientA.send(entry.getValue());
				signal.waitForTimeout(3000);
				final MulticastMessage messageType = entry.getKey();
				report.append("Client received ");
				report.append(messageType.name());
				report.append(" message: ");
				report.append(!messageTypes.contains(messageType));
				report.append(System.lineSeparator());
			}

		} catch (IOException ex) {
			report.append("Failed to send multicast messages - terminating test.");
			report.append(System.lineSeparator());
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			report.append("Thread using Signal was unexpectedly interrupted - terminating test.");
			report.append(System.lineSeparator());
			ex.printStackTrace();
		} finally {
			clientA.removeMessageListener(listener);
		}
		return report.toString();
	}

}