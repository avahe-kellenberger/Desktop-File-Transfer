package tech.avahe.filetransfer.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {

    private final Socket socket;

    public TCPClient(final int port) throws IOException {
        this.socket = new Socket(InetAddress.getByName(null), port);
    }

}