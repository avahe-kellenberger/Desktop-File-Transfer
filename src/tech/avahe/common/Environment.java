package tech.avahe.common;

import java.io.File;

/**
 * 
 * @author Avahe
 *
 */
public class Environment {

	// The directory where application data is stored.
	public static final String PROGRAM_DIR = (System.getProperty("os.name").toLowerCase().contains("win")
			? System.getenv("APPDATA")
			: System.getProperty("user.home")) + "/.FileTransfer/";


	// The configuration file.
	public static final File CONFIG_FILE = new File(Environment.PROGRAM_DIR + "config.ini");
	
	// The file transfer history log file.
	public static final File HISTORY_FILE = new File(Environment.PROGRAM_DIR + "history.log");
	
}