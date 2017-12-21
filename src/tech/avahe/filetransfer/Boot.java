package tech.avahe.filetransfer;

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
		final FileTransfer program = new FileTransfer() {
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
	}
	
}