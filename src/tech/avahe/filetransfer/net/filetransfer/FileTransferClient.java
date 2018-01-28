package tech.avahe.filetransfer.net.filetransfer;

import tech.avahe.filetransfer.net.TCPClient;
import tech.avahe.filetransfer.net.peerdiscovery.PeerInfo;

import java.io.File;
import java.io.IOException;

/**
 * @author Avahe
 */
public class FileTransferClient {

    private final TCPClient tcpClient = new TCPClient();

    /**
     * Sends all files to the peer.
     * @param peerInfo The information of the peer to communicate with.
     * @param files The files to send.
     * @return If all files were sent successfully.
     */
    public boolean send(final PeerInfo peerInfo, final File... files) {

        return false;
    }

    /**
     * @param peerInfo
     * @param file
     * @return
     * @throws IOException
     */
    private boolean sendFile(final PeerInfo peerInfo, final File file) throws IOException {


        return false;
    }


}