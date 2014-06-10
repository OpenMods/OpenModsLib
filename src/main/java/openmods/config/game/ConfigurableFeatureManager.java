package openmods.config.game;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import openmods.config.BlockInstances;
import openmods.config.ItemInstances;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class ConfigurableFeatureManager extends AbstractFeatureManager {

	private static class FeatureEntry {
		public boolean isEnabled;
		public final boolean isConfigurable;

		private FeatureEntry(boolean isEnabled, boolean isConfigurable) {
			this.isEnabled = isEnabled;
			this.isConfigurable = isConfigurable;
		}
	}

	private final Table<String, String, FeatureEntry> features = HashBasedTable.create();

	public void collectFromItems(Class<? extends ItemInstances> itemContainer) {
		for (Field f : itemContainer.getFields()) {
			RegisterItem item = f.getAnnotation(RegisterItem.class);
			if (item == null) continue;
			features.put(CATEGORY_ITEMS, item.name(), new FeatureEntry(item.isEnabled(), item.isConfigurable()));
		}
	}

	public void collectFromBlocks(Class<? extends BlockInstances> blockContainer) {
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

	@Override
	public boolean isEnabled(String category, String name) {
		FeatureEntry result = features.get(category, name);
		Preconditions.checkNotNull(result, "Invalid feature name %s.%s", category, name);
		return result.isEnabled;
	}
}
