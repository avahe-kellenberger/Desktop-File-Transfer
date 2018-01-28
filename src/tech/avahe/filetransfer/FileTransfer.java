package tech.avahe.filetransfer;

import tech.avahe.filetransfer.common.Settings;
import tech.avahe.filetransfer.common.Settings.Entry;
import tech.avahe.filetransfer.net.MulticastClient;
import tech.avahe.filetransfer.net.peerdiscovery.PeerDiscoveryClient;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Avahe
 *
 */
public abstract class FileTransfer {

	private final PeerDiscoveryClient discoveryClient;
	private String nickName;

	/**
	 * Creates the basic application needs for transferring files.
	 *
	 * <p>Creating this class automatically calls {@link FileTransfer#loadSettings()}, which passes on
	 * control of loading settings to {@link FileTransfer#onSettingsLoaded(Map)} which all base classes must implement.</p>
	 *
	 * <p>An internal <code>MulticastClient</code> is used for Local Area Network peerdiscovery peerdiscovery,
	 * and uses TCP for transferring files from one client to another.</p>
	 *
	 * @throws IOException Thrown if the underlying MulticastSocket cannot be created,
	 * or if there is an exception when disabling its loopback mode.
	 *
	 * @see PeerDiscoveryClient#PeerDiscoveryClient
	 * @see MulticastClient#setLoopbackMode(boolean)
	 */
	public FileTransfer() throws IOException, InterruptedException {
		this.loadSettings();
		if (this.nickName == null) {
			this.nickName = Entry.NICK_NAME.getDefaultValue();
		}
		this.discoveryClient = new PeerDiscoveryClient(this.nickName);
	}

	/**
	 * Handles the user settings once they are loaded.
	 * This method is called after the settings are loaded/configured by {@link FileTransfer#loadSettings()}.
	 * @param settings The loaded user settings.
	 */
	public void onSettingsLoaded(final Map<String, String> settings) {
		if (settings == null) {
			throw new IllegalArgumentException("Settings must not be null.");
		}
		this.nickName = settings.get(Entry.NICK_NAME.getKey());
	}

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
	private void loadSettings() {
		Map<String, String> settings = null;
		try {
			settings = Settings.getSettings();
			if (settings != null) {
				// Check for missing settings.
				final Map<String, String> missingSettings = new LinkedHashMap<>();
				for (final Entry key : Entry.values()) {
					final String keyName = key.getKey();
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
			// The settings will be passed as the default settings.
			settings = Settings.DEFAULT_SETTINGS;
		} finally {
			this.onSettingsLoaded(settings);
		}
	}

	/**
	 * @return The client's nickName.
	 */
	public String getNickName() {
		return this.nickName;
	}

	/**
	 * Sets the client's nick name.
	 *
	 * <p>Note: This does not make changes to the file system.
	 * In order to save the nick name, see FileTransfer{@link #saveNickName()}.</p>
	 *
	 * @param nickName The client's new nickName.
	 * @return If the nick name was changed.
	 * This will return false if the parameterized name was the same as the current nick name.
	 */
	public boolean setNickName(final String nickName) {
		if (!this.nickName.equals(nickName)) {
			this.nickName = nickName;
			this.discoveryClient.setNickName(this.nickName);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to save the current nick name to the configuration file.
	 * @throws IOException Thrown if the nick name could not be saved to the configuration file.
	 */
	public final void saveNickName() throws IOException {
		Settings.updateSetting(Entry.NICK_NAME.getKey(), this.nickName);
	}

}