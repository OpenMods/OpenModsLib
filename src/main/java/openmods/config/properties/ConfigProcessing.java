package openmods.config.properties;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import net.minecraftforge.common.config.Configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

public class ConfigProcessing {

	public static class ModConfig {
		private final Configuration config;
		public final Class<?> configClass;
		public final String modId;

		private Table<String, String, ConfigPropertyMeta> properties = HashBasedTable.create();

		private ModConfig(String modId, Configuration config, Class<?> configClass) {
			this.modId = modId;
			this.config = config;
			this.configClass = configClass;
		}

		void tryProcessConfig(Field field) {
			ConfigPropertyMeta meta = ConfigPropertyMeta.createMetaForField(config, field);
			if (meta != null) {
				meta.updateValueFromConfig(false);
				properties.put(meta.category.toLowerCase(Locale.ENGLISH), meta.name.toLowerCase(Locale.ENGLISH), meta);
			}
		}

		public File getConfigFile() {
			return config.getConfigFile();
		}

		public void save() {
			if (config.hasChanged()) config.save();
		}

		public Collection<String> getCategories() {
			return Collections.unmodifiableCollection(properties.rowKeySet());
		}

		public Collection<String> getValues(String category) {
			return Collections.unmodifiableCollection(properties.row(category.toLowerCase(Locale.ENGLISH)).keySet());
		}

		public ConfigPropertyMeta getValue(String category, String name) {
			return properties.get(category.toLowerCase(Locale.ENGLISH), name.toLowerCase(Locale.ENGLISH));
		}
	}

	private static final Map<String, ModConfig> configs = Maps.newHashMap();

	public static Collection<String> getConfigsIds() {
		return Collections.unmodifiableCollection(configs.keySet());
	}

	public static ModConfig getConfig(String modId) {
		return configs.get(modId.toLowerCase(Locale.ENGLISH));
	}

	public static void processAnnotations(String modId, Configuration config, Class<?> klazz) {
		Preconditions.checkState(!configs.containsKey(modId), "Trying to configure mod '%s' twice", modId);
		ModConfig configMeta = new ModConfig(modId, config, klazz);
		configs.put(modId.toLowerCase(Locale.ENGLISH), configMeta);

		for (Field f : klazz.getFields())
			configMeta.tryProcessConfig(f);
	}
}
