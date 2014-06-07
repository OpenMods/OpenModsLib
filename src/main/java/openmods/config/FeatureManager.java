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

	private final Table<String, String, Boolean> features = HashBasedTable.create();

	public void collectFromItems(Class<?> itemContainer) {
		for (Field f : itemContainer.getFields()) {
			RegisterItem item = f.getAnnotation(RegisterItem.class);
			if (item == null) continue;
			String itemName = item.name();
			boolean isEnabled = item.isEnabled();
			features.put(CATEGORY_ITEMS, itemName, isEnabled);
		}
	}

	public void collectFromBlocks(Class<?> blockContainer) {
		for (Field f : blockContainer.getFields()) {
			RegisterBlock item = f.getAnnotation(RegisterBlock.class);
			if (item == null) continue;
			String itemName = item.name();
			boolean isEnabled = item.isEnabled();
			features.put(CATEGORY_BLOCKS, itemName, isEnabled);
		}
	}

	public void loadFromConfiguration(Configuration config) {
		for (Map.Entry<String, Map<String, Boolean>> category : features.rowMap().entrySet()) {
			String categoryName = category.getKey();
			for (Map.Entry<String, Boolean> entries : category.getValue().entrySet()) {
				String featureName = entries.getKey();
				boolean defaultValue = entries.getValue();

				Property prop = config.get(categoryName, featureName, defaultValue);
				if (!prop.wasRead()) continue;

				if (!prop.isBooleanValue()) prop.set(defaultValue);
				else {
					boolean configValue = prop.getBoolean(defaultValue);
					if (configValue != defaultValue) entries.setValue(configValue);
				}
			}
		}
	}

	public boolean isEnabled(String category, String name) {
		Boolean result = features.get(category, name);
		Preconditions.checkNotNull(result, "Invalid feature name %s.%s", category, name);
		return result;
	}

	public boolean isBlockEnabled(String name) {
		return isEnabled(CATEGORY_BLOCKS, name);
	}

	public boolean isItemEnabled(String name) {
		return isEnabled(CATEGORY_ITEMS, name);
	}
}
