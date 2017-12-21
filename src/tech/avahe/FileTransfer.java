package tech.avahe;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import tech.avahe.common.Settings;
import tech.avahe.common.Settings.Keys;

/**
 * 
 * @author Avahe
 *
 */
public abstract class FileTransfer {

	private String username;
	
	/**
	 * Creates the basic application needs for transferring files.
	 * 
	 * Creating this class automatically calls {@link FileTransfer#loadSettings()}, which passes on
	 * control of loading settings to {@link FileTransfer#onSettingsLoaded(Map)} which all base classes must implement.
	 * 
	 * An internal <code>MulticastClient</code> is used for Local Area Network peer discovery,
	 * and uses TCP for transferring files from one client to another.
	 */
	public FileTransfer() {
		// TODO: Check if multicasting is allowed on this interface, and throw an exception if it is not.
		// TODO: Ensure the MulticastClient's loopback mode is set to false.
		this.loadSettings();
	}
	
	/**
	 * Handles the user settings once they are loaded.
	 * This method is called after the settings are loaded/configured by {@link FileTransfer#loadSettings()}.
	 * @param settings The loaded user settings.
	 */
	public abstract void onSettingsLoaded(final Map<String, String> settings);
	
	/**
	 * Loads the user settings from the configuration file.
	 * If the settings do not exist or any members are missing,
	 * the default settings will be written to the configuration file.
	 * 
	 * Note: Once this method finishes, it will invoke {@link FileTransfer#onSettingsLoaded(Map)}.
	 * 
	 * @throws IOException Thrown if the configuration file cannot be read from or written to.
	 */
	protected void loadSettings() {
		Map<String, String> settings = null;
		try {
			settings = Settings.getSettings();
			if (settings != null) {
				// Check for missing settings.
				final Map<String, String> missingSettings = new LinkedHashMap<String, String>();
				for (final Keys key : Settings.Keys.values()) {
					final String keyName = key.getName();
					 if (!settings.containsKey(keyName)) {
						 missingSettings.put(keyName, key.getDefaultValue());
					 }
				}
				// If any settings are missing, add the defaults to the settings file.
				if (missingSettings.size() > 0) {
					settings.putAll(missingSettings);
					Settings.writeSettings(settings);
				}
			} else {
				// Create the config file with default settings if it doesn't exist.
				settings = Settings.writeDefaultSettings();
			}
		} catch (IOException ex) {
			// Silently ignore the exception if the file is not accessible.
			// The settings will be passed as null.
			ex.printStackTrace();
		}
		this.onSettingsLoaded(settings);
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