package openmods;

import java.io.File;
import java.util.Map;

import openmods.config.simple.ConfigProcessor;

/**
 * Methods from core plugin, extracted to prevent accidental early load
 * @author boq
 *
 */
public class Bootstrap {

	public static void injectData(Map<String, Object> data) {
		File mcLocation = (File)data.get("mcLocation");
		File configDir = new File(mcLocation, "config");

		if (!configDir.exists()) configDir.mkdir();

		File configFile = new File(configDir, "OpenModsLibCore.json");

		try {
			ConfigProcessor processor = new ConfigProcessor();
			OpenModsClassTransformer.instance().addConfigValues(processor);
			processor.process(configFile);
		} catch (Throwable t) {
			throw new RuntimeException(String.format("Failed to read config from file %s", configFile), t);
		}
	}

}
