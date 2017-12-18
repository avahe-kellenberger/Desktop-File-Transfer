package tech.avahe.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Avahe
 *
 */
public class Settings {

	/**
	 * The settings keys and default values.
	 */
	public enum Keys {
		
		USERNAME("username", System.getProperty("user.name"));
		
		private final String name;
		private final String defaultValue;
		
		/**
		 * Creates a key with a default value.
		 * @param name The name of the key.
		 * @param defaultValue The default value of the key.
		 */
		Keys(final String name, final String defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}
		
		/**
		 * @return The name of the key.
		 */
		public String getName() {
			return this.name;
		}
		
		/**
		 * @return The default value associated with the key.
		 */
		public String getDefaultValue() {
			return this.defaultValue;
		}
		
	}
	
	// The direction where settings for the application are placed.
	public static final String SETTINGS_DIR = 
			(System.getProperty("os.name").toLowerCase().contains("win") ? 
					System.getenv("APPDATA") : System.getProperty("user.home")) + "/.FileTransfer/";
	
	// The configuration file name.
	public static final File CONFIG_FILE = new File(Settings.SETTINGS_DIR + "config.ini");
	
	/**
	 * Creates the default configuration file, as defined by Settings.Keys.
	 * @return Key/Value pairs of the file.
	 * @throws IOException Thrown if the file cannot be written to.
	 */
	public static Map<String, String> createDefaultConfigFile() throws IOException {
		final LinkedHashMap<String, String> settings = new LinkedHashMap<>();
		for (final Keys key : Keys.values()) {
			settings.put(key.getName(), key.getDefaultValue());
		}
		Settings.writeSettings(settings);
		return settings;
	}
	
	/**
	 * Loads the configuration settings from config file.
	 * @return The configuration settings.
	 * @throws IOException Thrown if the file cannot be read from.
	 */
	public static Map<String, String> getSettings() throws IOException {
		if (Settings.CONFIG_FILE.exists()) {
			try (final BufferedReader reader = new BufferedReader(new FileReader(Settings.CONFIG_FILE))) {
				final LinkedHashMap<String, String> settings = new LinkedHashMap<>();
				String line;
				while ((line = reader.readLine()) != null) {
					final String[] split = line.split("=");
					settings.put(split[0], split[1]);
				}
				return settings;
			}
		}
		return null;
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
	 * @param updatedSetting The setting to update or add to the settings file.
	 * @return The settings after being updated.
	 * @throws IOException Thrown if the file cannot be written to.
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
		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(Settings.CONFIG_FILE))) {
			for (final Map.Entry<String, String> entry : settings.entrySet()) {
				writer.write(entry.getKey() + "=" + entry.getValue() + System.lineSeparator());
			}
		}
	}
	
}