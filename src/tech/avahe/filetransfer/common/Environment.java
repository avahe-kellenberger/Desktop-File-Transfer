package tech.avahe.filetransfer.common;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 
 * @author Avahe
 *
 */
public class Environment {

	/**
	 * The local host of this machine.
	 */
	public static final InetAddress LOCAL_HOST;

	/**
	 * The local address of this machine.
	 */
	public static final String LOCAL_ADDRESS;

	static {
		InetAddress tempAddress = null;
		try {
			tempAddress = InetAddress.getLocalHost();
		} catch (final UnknownHostException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
		LOCAL_HOST = tempAddress;
		LOCAL_ADDRESS = tempAddress.getHostAddress();
	}

	/**
	 * The home directory of the user that is currently logged in.
	 */
	public static final String USER_HOME = System.getProperty("user.home");
	
	/**
	 * The directory where application data is stored.
	 */
	public static final String PROGRAM_DIR = (System.getProperty("os.name").toLowerCase().contains("win")
			? System.getenv("APPDATA")
			: Environment.USER_HOME) + "/.FileTransfer/";

	/**
	 * The computer's default download directory.
	 */
	public static final String DOWNLOAD_DEFAULT_DIR = Environment.USER_HOME + "/Downloads/";

	/**
	 * The configuration file.
	 */
	public static final File CONFIG_FILE = new File(Environment.PROGRAM_DIR + "config.ini");
	
	/**
	 * The file transfer history log file.
	 */
	public static final File HISTORY_FILE = new File(Environment.PROGRAM_DIR + "history.log");
	
}