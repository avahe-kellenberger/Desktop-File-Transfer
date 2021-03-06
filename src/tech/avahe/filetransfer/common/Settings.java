package tech.avahe.filetransfer.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;

/**
 * 
 * @author Avahe
 *
 * This class handles the reading and writing associated with the user settings (configuration file).
 * The config file is an *.ini file, using the key=value format, which each key on a new line.
 * 
 * The settings are as follows:
 * 		username=The user's display name
 * 		gui-state=(A value from {JFrame#getExtendedState})
 */
public class Settings {

	/**
	 * The settings entries.
	 */
	public enum Entry {
		
		NICK_NAME("nick-name", System.getProperty("user.name")),
		GUI_STATE("gui-state", "" + JFrame.NORMAL);

		private static final String SEPARATOR = "=";

		private final String key;
		private final String defaultValue;
		
		/**
		 * Creates an entry with a key and a default value.
		 * @param key The name of the key.
		 * @param defaultValue The default value associated with the key.
		 */
		Entry(final String key, final String defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}
		
		/**
		 * @return The key of the setting.
		 */
		public String getKey() {
			return this.key;
		}
		
		/**
		 * @return The default value associated with the key.
		 */
		public String getDefaultValue() {
			return this.defaultValue;
		}
		
	}
	
	/**
	 * The default configuration file settings.
	 */
	public static final Map<String, String> DEFAULT_SETTINGS = new LinkedHashMap<>();
	
	// Loads the default settings.
	static {
		for (final Entry entry : Entry.values()) {
			Settings.DEFAULT_SETTINGS.put(entry.getKey(), entry.getDefaultValue());
		}
	}
	
	/**
	 * Attempts to create the configuration file and all its parent directories.
	 * <p>This method does not check if the file already exists.</p>
	 * @return If the config file or any of its parent directories were created.
	 * @throws IOException Thrown if the file or its parent directories could not be created.
	 */
	public static boolean createConfigFile() throws IOException {
		return Environment.CONFIG_FILE.getParentFile().mkdirs() || Environment.CONFIG_FILE.createNewFile();
	}
	
	/**
	 * Creates the default configuration file, as defined by {@link Entry}.
	 * @return Key/Value pairs of the file.
	 * @throws IOException Thrown if the config file cannot be written to.
	 */
	public static Map<String, String> writeDefaultSettings() throws IOException {
		final Map<String, String> settings = Settings.DEFAULT_SETTINGS;
		Settings.writeSettings(settings);
		return settings;
	}
	
	/**
	 * Loads the configuration settings from config file.
	 * @return The configuration settings.
	 * @throws IOException Thrown if the file exists but cannot be read from.
	 */
	public static Map<String, String> getSettings() throws IOException {
		if (!Environment.CONFIG_FILE.exists()) {
			return null;
		}
		try (final BufferedReader reader = new BufferedReader(new FileReader(Environment.CONFIG_FILE))) {
			final LinkedHashMap<String, String> settings = new LinkedHashMap<>();
			String line;
			while ((line = reader.readLine()) != null) {
				final String[] split = line.split(Entry.SEPARATOR);
				settings.put(split[0], split[1]);
			}
			return settings;
		}
	}
	
	/**
	 * Updates the current settings file with the updated settings.
	 * @param updatedSettings The settings to update or add to the settings file.
	 * @return The settings after being updated.
	 * @throws IOException Thrown if the file cannot be written to.
	 */
	public static Map<String, String> updateSettings(final Map<String, String> updatedSettings) throws IOException {
		final Map<String, String> newSettings;
		
		final Map<String, String> oldSettings = Settings.getSettings();
		if (oldSettings != null) {
			for (final Map.Entry<String, String> entry : updatedSettings.entrySet()) {
				oldSettings.put(entry.getKey(), entry.getValue());
			}
			newSettings = oldSettings;
		} else {
			newSettings = updatedSettings;
		}
		Settings.writeSettings(newSettings);
		return newSettings;
	}
	
	/**
	 * Updates the current settings file with the a singular updated setting.
	 * @param key The key of the setting to update.
	 * @param value The value of the setting to update.
	 * @return The settings after being updated.
	 * @throws IOException Thrown if the config file cannot be written to.
	 */
	public static Map<String, String> updateSetting(final String key, final String value) throws IOException {
		return Settings.updateSettings(Collections.singletonMap(key, value));
	}
	
	/**
	 * Writes a new settings file, overwriting any previous settings.
	 * @param settings The settings to write.
	 * @throws IOException Thrown if the file cannot be written to.
	 */
	public static void writeSettings(final Map<String, String> settings) throws IOException {
		if (!Environment.CONFIG_FILE.exists()) {
			Settings.createConfigFile();
		}
		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(Environment.CONFIG_FILE))) {
			for (final Map.Entry<String, String> entry : settings.entrySet()) {
				writer.write(entry.getKey() + Entry.SEPARATOR + entry.getValue() + System.lineSeparator());
			}
		}
	}
	
}