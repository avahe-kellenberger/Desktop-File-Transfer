package tech.avahe.filetransfer.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClient {

    public static final String LOCAL_ADDRESS;

    static {
        InetAddress tempAddress = null;
        try {
            tempAddress = InetAddress.getLocalHost();
        } catch (final UnknownHostException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        LOCAL_ADDRESS = tempAddress.getHostAddress();
    }

    private final Socket socket;

    public TCPClient(final int port) throws IOException {
        this.socket = new Socket(InetAddress.getByName(null), port);
    }

}