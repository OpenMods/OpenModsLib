package openmods.config.game;

import java.util.Map;

import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

public class FeatureRegistry {

	public static final FeatureRegistry instance = new FeatureRegistry();

	private static class Entry {
		public final AbstractFeatureManager manager;

		public final Table<String, String, Property> properties;

		public Entry(AbstractFeatureManager manager, Table<String, String, Property> properties) {
			this.manager = manager;
			this.properties = properties;
		}
	}

	private Map<String, Entry> features = Maps.newHashMap();

	private void addValue(Entry entry) {
		ModContainer mod = Loader.instance().activeModContainer();
		Preconditions.checkNotNull(mod, "Can't register outside initialization");
		final String modId = mod.getModId();

		final Entry prev = features.put(modId, entry);
		Preconditions.checkState(prev == null, "Duplicate on modid: " + modId);
	}

	public void register(AbstractFeatureManager manager) {
		addValue(new Entry(manager, ImmutableTable.<String, String, Property> of()));
	}

	public void register(AbstractFeatureManager manager, Table<String, String, Property> properties) {
		Preconditions.checkNotNull(properties);
		addValue(new Entry(manager, ImmutableTable.copyOf(properties)));
	}

	public AbstractFeatureManager getManager(String modId) {
		final Entry entry = features.get(modId);
		if (entry == null) return null;

		return entry.manager;
	}

	public boolean isEnabled(String modId, String category, String feature) {
		final Entry entry = features.get(modId);
		if (entry == null) return false;

		return entry.manager.isEnabled(category, feature);
	}

	public Property getProperty(String modId, String category, String feature) {
		final Entry entry = features.get(modId);
		if (entry == null) return null;

		return entry.properties.get(category, feature);
	}
}
