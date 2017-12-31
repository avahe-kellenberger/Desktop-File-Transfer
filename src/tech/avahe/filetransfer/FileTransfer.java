package tech.avahe.filetransfer;

import tech.avahe.filetransfer.common.Settings;
import tech.avahe.filetransfer.common.Settings.Keys;
import tech.avahe.filetransfer.net.multicast.MulticastClient;
import tech.avahe.filetransfer.net.multicast.MulticastMessageListener;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Avahe
 *
 */
public abstract class FileTransfer implements MulticastMessageListener {

	private String username;
	
	private final MulticastClient multicastClient;
	
	/**
	 * Creates the basic application needs for transferring files.
	 * 
	 * <p>Creating this class automatically calls {@link FileTransfer#loadSettings()}, which passes on
	 * control of loading settings to {@link FileTransfer#onSettingsLoaded(Map)} which all base classes must implement.</p>
	 * 
	 * <p>An internal <code>MulticastClient</code> is used for Local Area Network peer discovery,
	 * and uses TCP for transferring files from one client to another.</p>
	 * 
	 * @throws IOException Thrown if the underlying MulticastSocket cannot be created,
	 * or if there is an exception when disabling its loopback mode. 
	 *
	 * @see MulticastClient#MulticastClient()
	 * @see MulticastClient#setLoopbackMode(boolean)
	 */
	public FileTransfer() throws IOException {
		this.multicastClient = new MulticastClient();
		// Ensure the MulticastClient's loopback mode is set to false,
		// so that the program will not receive its own messages as an external program on the network.
		this.multicastClient.setLoopbackMode(true);
		this.multicastClient.addMessageListener(this);
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
     * <p>If the settings file cannot be accessed, the default settings will be used
     * and no settings will be saved.</p>
	 * 
	 * <p>Note: Once this method finishes, it will invoke {@link FileTransfer#onSettingsLoaded(Map)}.</p>
	 */
	protected void loadSettings() {
		Map<String, String> settings = null;
		try {
			settings = Settings.getSettings();
			if (settings != null) {
				// Check for missing settings.
				final Map<String, String> missingSettings = new LinkedHashMap<>();
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
		if (!this.username.equals(name)) {
			this.username = name;
			return true;
		}
		return false;
	}
	
	/**
	 * Attempts to save the current username to the configuration file.
	 * @throws IOException Thrown f the username could not be saved to the configuration file.
	 */
	public final void saveUsername() throws IOException {
		Settings.updateSetting(Settings.Keys.USERNAME.getName(), this.username);
	}
	
}