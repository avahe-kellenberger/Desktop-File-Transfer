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
	public Program() throws IOException {
		Map<String, String> settings = Settings.getSettings();
		if (settings == null) {
			// Settings file doesn't exist; create the file.
			settings = Settings.createDefaultConfigFile();
		}
		final String usernameKey = Settings.Keys.USERNAME.getName();
		// Iterate through settings, to find the username.
		for (final Map.Entry<String, String> entry : settings.entrySet()) {
			final String key = entry.getKey();
			if (key.equals(usernameKey)) {
				// Username found in settings file.
				this.username = entry.getValue();
				break;
			}
		}
		// Settings exist, but there is no username entry.
		if (this.username == null) {
			this.username = System.getProperty("user.name");
			Settings.updateSetting(Settings.Keys.USERNAME.getName(), Settings.Keys.USERNAME.getDefaultValue());
		}

		// TODO: Configure connections and GUI.
	}
	
	/**
	 * 
	 * @return
	 */
	public String getUsername() {
		return this.username;
	}
	
}