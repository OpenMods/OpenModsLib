package openmods.config.game;

import java.io.File;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import openmods.config.BlockInstances;
import openmods.config.ConfigStorage;
import openmods.config.ItemInstances;

import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import cpw.mods.fml.common.event.FMLMissingMappingsEvent;

public class ModStartupHelper {

	private final Set<Class<? extends BlockInstances>> blockHolders = Sets.newHashSet();

	private final Set<Class<? extends ItemInstances>> itemHolders = Sets.newHashSet();
	private final GameConfigProvider gameConfig;

	public ModStartupHelper(String modId) {
		this.gameConfig = new GameConfigProvider(modId);
	}

	public void registerBlocksHolder(Class<? extends BlockInstances> holder) {
		blockHolders.add(holder);
	}

	public void registerItemsHolder(Class<? extends ItemInstances> holder) {
		itemHolders.add(holder);
	}

	public void preInit(File configFile) {
		preInit(new Configuration(configFile));
	}

	public void preInit(Configuration config) {
		ConfigurableFeatureManager features = new ConfigurableFeatureManager();
		for (Class<? extends BlockInstances> blockHolder : blockHolders)
			features.collectFromBlocks(blockHolder);

		for (Class<? extends ItemInstances> itemHolder : itemHolders)
			features.collectFromItems(itemHolder);

		registerCustomFeatures(features);

		populateConfig(config);
		final Table<String, String, Property> properties = features.loadFromConfiguration(config);
		FeatureRegistry.instance.register(features, properties);

		if (config.hasChanged()) config.save();

		ConfigStorage.instance.register(config);

		gameConfig.setFeatures(features);

		setupBlockFactory(gameConfig.getBlockFactory());

		setupItemFactory(gameConfig.getItemFactory());

		for (Class<? extends BlockInstances> blockHolder : blockHolders)
			gameConfig.registerBlocks(blockHolder);

		for (Class<? extends ItemInstances> itemHolder : itemHolders)
			gameConfig.registerItems(itemHolder);

		setupProvider(gameConfig);
	}

	public void handleRenames(FMLMissingMappingsEvent event) {
		gameConfig.handleRemaps(event.get());
	}

	protected void setupItemFactory(FactoryRegistry<Item> itemFactory) {}

	protected void setupBlockFactory(FactoryRegistry<Block> blockFactory) {}

	protected void populateConfig(Configuration config) {}

	protected void registerCustomFeatures(ConfigurableFeatureManager features) {}

	protected void setupProvider(GameConfigProvider gameConfig) {}
}
