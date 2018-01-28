package net;

import tech.avahe.filetransfer.net.MulticastClient;
import tech.avahe.filetransfer.threading.ThreadSignaller;
import tech.avahe.filetransfer.util.Buffers;

import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.BiConsumer;

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

	private final MulticastClient clientA;
	private final MulticastClient clientB;

	/**
	 * Starts the tests for MulticastClient.java.
	 * @throws Exception Thrown if the conditions to test the class cannot be met.
	 */
	public MulticastClientTest() throws Exception {
		System.out.println("MulticastClientTest: ");

		// Initialize the test clients.
		final String ipAddress = "224.0.0.17";
		final int port = 7899;
		this.clientA = new MulticastClient(ipAddress, port);
		try {
			this.clientB = new MulticastClient(ipAddress, port);
			try {
				if (this.clientA.isClosed() || this.clientB.isClosed()) {
					throw new Exception("Clients were closed after being initialized; aborting tests.");
				}
				if (!this.clientB.startListening(1000)) {
					throw new Exception("Client B failed to start listening; aborting tests.");
				}

				this.clientA.setLoopbackMode(true);
				this.clientB.setLoopbackMode(true);

				// Run the test suite.
				System.out.println("Checking for basic connectivity (sending/receiving messages)");
				this.checkConnectivity();
			} finally {
				this.clientB.close();
			}
		} finally {
			this.clientA.close();
		}
	}

	/**
	 * Tests sending and receiving <code>DatagramPackets</code> via <code>MulticastClient</code>.
	 * @return If the connectivity test passed.
	 */
	private void checkConnectivity() {
		final PrintStream out = System.out;
		final ArrayList<String> receivedMessages = new ArrayList<>(3);
		final ThreadSignaller signaller = new ThreadSignaller();

		// Listen for incoming packets.
		final BiConsumer<SocketAddress, ByteBuffer> listener = (remoteAddress, buffer) -> {
			receivedMessages.add(Buffers.toString(buffer));
			signaller.signal();
		};

		this.clientB.addDataListener(listener);
		
		final String[] messages = { "Message 0", "Message 1", "Message 2" };
		
		try {
			// Send a message and check if it is received.
			out.print("Client received a message: ");
			signaller.reset();
			this.clientA.send(messages[0]);
			signaller.waitForTimeout(1000);
			log(receivedMessages.contains(messages[0]));

			// Stop the client from listening, and make sure it doesn't receive a message.
			out.print("Client properly stopped listening: ");
			log(this.clientB.stopListening(1000));

			signaller.reset();
			this.clientA.send(messages[1]);
			signaller.waitForTimeout(1000);
			out.print("Client properly not receiving a message: ");
			log(!receivedMessages.contains(messages[1]));

			// Start listening for messages again, and check to see if it still receives a message.
			out.print("Client started listening as expected: ");
			log(this.clientB.startListening(1000));

			signaller.reset();
			this.clientA.send(messages[2]);
			signaller.waitForTimeout(1000);
			out.print("Client receiving a message after listening was re-enabled: ");
			log(receivedMessages.contains(messages[2]));
		} catch (IOException|InterruptedException ex) {
			ex.printStackTrace();
		} finally {
			this.clientB.removeDataListener(listener);
		}
	}

	private static void log(boolean success) {
		if (success) {
			System.out.println("success");
		} else {
			System.out.println("error");
		}
		// System.out.flush();
		// System.err.flush();
		// Thread.yield();
	}

}