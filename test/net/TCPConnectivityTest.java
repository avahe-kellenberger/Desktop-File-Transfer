package net;

import tech.avahe.filetransfer.net.TCPClient;
import tech.avahe.filetransfer.net.TCPServer;

import java.io.IOException;

public class TCPConnectivityTest {

    public static void main(String[] args) throws IOException {
        new TCPConnectivityTest();
    }

    private final int port = 1337;

    public TCPConnectivityTest() throws IOException {
        this.testConnectivity();
    }

    private void testConnectivity() throws IOException {
        final TCPServer server = new TCPServer(this.port);
        server.addConnectionListener(socket -> System.out.println("Received a connection from " + socket.getInetAddress().getHostAddress()));
        server.acceptIncomingConnections();

        final TCPClient client = new TCPClient();
        client.connect("localhost", this.port);

        client.close();
        server.close();
    }

}