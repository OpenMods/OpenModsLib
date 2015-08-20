package openmods.config.gui;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import openmods.config.game.AbstractFeatureManager;
import openmods.config.game.FeatureRegistry;
import openmods.config.properties.ConfigProcessing;
import openmods.config.properties.ConfigProcessing.ModConfig;
import openmods.config.properties.ConfigPropertyMeta;

import com.google.common.collect.Lists;

import cpw.mods.fml.client.config.DummyConfigElement.DummyCategoryElement;
import cpw.mods.fml.client.config.*;
import cpw.mods.fml.client.config.GuiConfigEntries.CategoryEntry;
import cpw.mods.fml.client.config.GuiConfigEntries.IConfigEntry;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class OpenModsConfigScreen extends GuiConfig {

	private static class CategoryElement extends DummyCategoryElement {
		private CategoryElement(String name, String langKey, List<IConfigElement> childElements) {
			super(name, langKey, childElements);
		}

		public CategoryElement(String name, String langKey, List<IConfigElement> childElements, Class<? extends IConfigEntry> customListEntryClass) {
			super(name, langKey, childElements, customListEntryClass);
		}

		@Override
		public String getComment() {
			return null;
		}
	}

	public static class EntryWithWarning extends CategoryEntry {
		public EntryWithWarning(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
			super(owningScreen, owningEntryList, configElement);
		}

		@Override
		protected GuiScreen buildChildScreen() {
			return new GuiConfig(this.owningScreen, this.configElement.getChildElements(), this.owningScreen.modID,
					owningScreen.allRequireWorldRestart || this.configElement.requiresWorldRestart(),
					owningScreen.allRequireMcRestart || this.configElement.requiresMcRestart(), this.owningScreen.title,
					I18n.format("openmodslib.config.update_warning"));
		}

	}

	public OpenModsConfigScreen(GuiScreen parent, String modId, String title) {
		super(parent, createConfigElements(modId), modId, false, true, title);
	}

	private static List<IConfigElement> createConfigElements(String modId) {
		List<IConfigElement> result = Lists.newArrayList();

		{
			final IConfigElement<?> features = createFeatureEntries(modId);
			if (features != null) result.add(features);
		}

		{
			final IConfigElement<?> config = createConfigEntries(modId);
			if (config != null) result.add(config);
		}

		return result;
	}

	protected static IConfigElement<?> createFeatureEntries(String modId) {
		final AbstractFeatureManager manager = FeatureRegistry.instance.getManager(modId);
		if (manager == null) return null;

		final List<IConfigElement> categories = Lists.newArrayList();

		for (String category : manager.getCategories()) {
			List<IConfigElement> categoryEntries = Lists.newArrayList();
			for (String feature : manager.getFeaturesInCategory(category)) {
				final Property property = FeatureRegistry.instance.getProperty(modId, category, feature);
				if (property != null) categoryEntries.add(new ConfigElement(property));
			}

			categories.add(new CategoryElement(category, "openmodslib.config.features." + category, categoryEntries));
		}

		return new CategoryElement("features", "openmodslib.config.features", categories);
	}

	private static IConfigElement<?> createConfigEntries(String modId) {
		final ModConfig config = ConfigProcessing.getConfig(modId);
		if (config == null) return null;

		final List<IConfigElement> categories = Lists.newArrayList();

		for (String category : config.getCategories()) {
			final List<IConfigElement> categoryEntries = Lists.newArrayList();
			for (String value : config.getValues(category)) {
				final ConfigPropertyMeta meta = config.getValue(category, value);
				categoryEntries.add(new ConfigElement(meta.getProperty()));
			}

			categories.add(new CategoryElement(category, "openmodslib.config.category." + category, categoryEntries));
		}

		return new CategoryElement("config", "openmodslib.config.config", categories, EntryWithWarning.class);
	}
}