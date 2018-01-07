package net;

import tech.avahe.filetransfer.net.peerdiscovery.PeerDiscoveryClient;
import tech.avahe.filetransfer.net.peerdiscovery.PeerListener;

import java.io.IOException;

/**
 * @author Avahe
 */
public class PeerDiscoveryClientTest {

    /**
     * Creates the test class.
     */
    public static void main(String[] args) {
        try {
            new PeerDiscoveryClientTest();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Runs the test suite.
     * @throws Exception Thrown if there is an unusual error while running the tests.
     */
    public PeerDiscoveryClientTest() throws Exception {
        final StringBuilder report = new StringBuilder();
        try {
            // Run the test suite.
            report.append("Initiating tests.");
            report.append(System.lineSeparator());
            report.append(this.checkConnectivity());
            report.append(System.lineSeparator());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(report.toString());
    }

    /**
     * Verifies if the clients are sending and receiving messages properly.
     * @return A detailed report of the test.
     * @throws IOException Thrown if the clients could not be initialized.
     * @throws InterruptedException Thrown if a thread signalling error occurs.
     */
    private String checkConnectivity() throws IOException, InterruptedException {
        final StringBuilder report = new StringBuilder();
        final String lineSeparator = System.lineSeparator();
        final ThreadSignaller signaller = new ThreadSignaller();
        final PeerDiscoveryClient clientA = new PeerDiscoveryClient("clientA");
        clientA.setLoopbackMode(false);

        final boolean[] messageReceivedFlags = new boolean[3];

        // Listen for peer messages.
        final PeerListener listener = new PeerListener() {
            @Override
            public void onPeerConnected(String ipAddress, String nickName) {
                messageReceivedFlags[0] = true;
                signaller.set();
            }

            @Override
            public void onPeerNickNameChange(String ipAddress, String newNickName, String oldNickName) {
                messageReceivedFlags[1] = true;
                signaller.set();
            }

            @Override
            public void onPeerDisconnected(String ipAddress, String nickName) {
                messageReceivedFlags[2] = true;
                signaller.set();
            }
        };

        clientA.addPeerListener(listener);

        final PeerDiscoveryClient clientB = new PeerDiscoveryClient("clientB");
        clientB.setLoopbackMode(false);
        signaller.waitForTimeout(5000);
        report.append("Received onPeerConnected: ");
        report.append(messageReceivedFlags[0]);
        report.append(lineSeparator);

        clientB.setNickName("Bob");
        signaller.waitForTimeout(5000);
        report.append("Received onPeerNickNameChange: ");
        report.append(messageReceivedFlags[1]);
        report.append(lineSeparator);

        clientB.close();
        signaller.waitForTimeout(1000);
        report.append("Received onPeerDisconnected: ");
        report.append(messageReceivedFlags[1]);
        report.append(lineSeparator);

        clientA.removePeerListener(listener);
        clientA.close();
        return report.toString();
    }

}