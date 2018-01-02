package tech.avahe.filetransfer;

import java.io.IOException;
import java.util.Map;

/**
 * 
 * @author Avahe
 *
 */
public class Boot {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO: Launch command line or gui version based on arguments.
        FileTransfer program = null;
        try {
			program = new FileTransfer() {
				@Override
				public void onSettingsLoaded(Map<String, String> settings) {
					if (settings != null) {
						for (final String key : settings.keySet()) {
							System.out.println(key + "=" + settings.get(key));
						}
					} else {
						System.out.println("Null settings.");
					}
				}
			};
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		if (program == null) {
            System.err.println("Client could not be established - terminating program.");
        }
	}
	
}