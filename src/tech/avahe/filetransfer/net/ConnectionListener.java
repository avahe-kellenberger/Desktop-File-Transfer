package tech.avahe.filetransfer.net;

import java.net.Socket;

/**
 * @author Avahe
 */
public interface ConnectionListener {

    /**
     * Called when a new connection is established.
     * @param socket The incoming socket.
     */
    void onConnectionEstablished(final Socket socket);

}