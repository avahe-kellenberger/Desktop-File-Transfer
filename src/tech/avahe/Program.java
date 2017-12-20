package tech.avahe;

import java.io.IOException;
import java.util.Map;

import tech.avahe.common.Settings;

/**
 * 
 * @author Avahe
 *
 */
public class Program {

	private String username;
	
	/**
	 * @throws IOException 
	 */
	public Program() {
		try {
			// TODO: Ensure the MulticastClient's loopback mode is set to false.
			this.init();
		} catch (IOException ex) {
			ex.printStackTrace();
			// TODO: If the program cannot interact with the file system,
			// Open a dialogue notifying the using, and use the default username for the session.
		}
	}
	
	/**
	 * Reads the user settings and configures 
	 * @throws IOException
	 */
	private void init() throws IOException {
		Map<String, String> settings = Settings.getSettings();
		if (settings == null) {
			// Settings file doesn't exist; create the file.
			settings = Settings.writeDefaultSettings();
		}
		// Iterate through settings, to find the username.
		final String storedUsername = settings.get(Settings.Keys.USERNAME.getName());
		if (storedUsername == null) {
			// Settings exist, but there is no username entry.
			this.username = Settings.Keys.USERNAME.getDefaultValue();
		}
		// TODO: Configure connections and GUI.
	}
	
	/**
	 * @return The client's username.
	 */
	public String getUsername() {
		return this.username;
	}
	
	/**
	 * Sets the client's username.
	 * @param name The client's new username.
	 * @return If the username was changed.
	 * This will return false if the parameterized name was the same as the current username.
	 */
	public boolean setUsername(final String name) {
		// TODO: Attempt to save username to config file.
		if (!this.username.equals(name)) {
			this.username = name;
			return true;
		}
		return false;
	}
	
}