package openmods.config.game;

import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.io.File;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.RegistryEvent.MissingMappings;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import openmods.config.BlockInstances;
import openmods.config.ConfigStorage;
import openmods.config.ItemInstances;

public class ModStartupHelper {

	private final Set<Class<? extends BlockInstances>> blockHolders = Sets.newHashSet();

	private final Set<Class<? extends ItemInstances>> itemHolders = Sets.newHashSet();
	private final GameRegistryObjectsProvider gameObjectsProvider;

	public ModStartupHelper(String modId) {
		this.gameObjectsProvider = new GameRegistryObjectsProvider(modId);
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

		gameObjectsProvider.setFeatures(features);

		setupConfigPre(gameObjectsProvider);

		setupBlockFactory(gameObjectsProvider.getBlockFactory());

		setupItemFactory(gameObjectsProvider.getItemFactory());

		final IForgeRegistry<Item> items = GameRegistry.findRegistry(Item.class);
		final IForgeRegistry<Block> blocks = GameRegistry.findRegistry(Block.class);

		for (Class<? extends BlockInstances> blockHolder : blockHolders)
			gameObjectsProvider.registerBlocks(blockHolder, blocks, items);

		for (Class<? extends ItemInstances> itemHolder : itemHolders)
			gameObjectsProvider.registerItems(itemHolder, items);

		setupConfigPost(gameObjectsProvider);
	}

	public void init() {
		gameObjectsProvider.registerItemModels();
	}

	public void handleBlockRenames(MissingMappings<Block> event) {
		gameObjectsProvider.handleBlockRemaps(gameObjectsProvider.hasIntraModRenames()? event.getAllMappings() : event.getMappings());
	}

	public void handleItemRenames(MissingMappings<Item> event) {
		gameObjectsProvider.handleItemRemaps(gameObjectsProvider.hasIntraModRenames()? event.getAllMappings() : event.getMappings());
	}

	protected void setupItemFactory(FactoryRegistry<Item> itemFactory) {}

	protected void setupBlockFactory(FactoryRegistry<Block> blockFactory) {}

	protected void populateConfig(Configuration config) {}

	protected void registerCustomFeatures(ConfigurableFeatureManager features) {}

	protected void setupConfigPre(GameRegistryObjectsProvider gameConfig) {}

	protected void setupConfigPost(GameRegistryObjectsProvider gameConfig) {}
}
