package openmods.config;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class FeatureManager {

	public static final String CATEGORY_ITEMS = "items";

	public static final String CATEGORY_BLOCKS = "blocks";

	private static class FeatureEntry {
		public boolean isEnabled;
		public final boolean isConfigurable;

		private FeatureEntry(boolean isEnabled, boolean isConfigurable) {
			this.isEnabled = isEnabled;
			this.isConfigurable = isConfigurable;
		}
	}

	private final Table<String, String, FeatureEntry> features = HashBasedTable.create();

	public void collectFromItems(Class<?> itemContainer) {
		for (Field f : itemContainer.getFields()) {
			RegisterItem item = f.getAnnotation(RegisterItem.class);
			if (item == null) continue;
			features.put(CATEGORY_ITEMS, item.name(), new FeatureEntry(item.isEnabled(), item.isConfigurable()));
		}
	}

	public void collectFromBlocks(Class<?> blockContainer) {
		for (Field f : blockContainer.getFields()) {
			RegisterBlock item = f.getAnnotation(RegisterBlock.class);
			if (item == null) continue;
			features.put(CATEGORY_BLOCKS, item.name(), new FeatureEntry(item.isEnabled(), item.isConfigurable()));
		}
	}

	public void loadFromConfiguration(Configuration config) {
		for (Map.Entry<String, Map<String, FeatureEntry>> category : features.rowMap().entrySet()) {
			String categoryName = category.getKey();
			for (Map.Entry<String, FeatureEntry> entries : category.getValue().entrySet()) {
				FeatureEntry entry = entries.getValue();
				if (!entry.isConfigurable) continue;

				String featureName = entries.getKey();
				Property prop = config.get(categoryName, featureName, entry.isEnabled);
				if (!prop.wasRead()) continue;

				if (!prop.isBooleanValue()) prop.set(entry.isEnabled);
				else entry.isEnabled = prop.getBoolean(entry.isEnabled);
			}
		}
	}

	public boolean isEnabled(String category, String name) {
		FeatureEntry result = features.get(category, name);
		Preconditions.checkNotNull(result, "Invalid feature name %s.%s", category, name);
		return result.isEnabled;
	}

	public boolean isBlockEnabled(String name) {
		return isEnabled(CATEGORY_BLOCKS, name);
	}

	public boolean isItemEnabled(String name) {
		return isEnabled(CATEGORY_ITEMS, name);
	}
}
