import net.MulticastClientTest;
import net.TCPConnectivityTest;
import net.peerdiscovery.PeerDiscoveryClientTest;

public class TestSuite {

    public static void main(String[] args) throws Exception {
        new TCPConnectivityTest();
        new MulticastClientTest();
        new PeerDiscoveryClientTest();
    }

}