package openmods.config.game;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import net.minecraftforge.fml.common.registry.GameRegistry;
import openmods.Log;
import openmods.OpenMods;
import openmods.config.BlockInstances;
import openmods.config.InstanceContainer;
import openmods.config.ItemInstances;
import openmods.config.game.RegisterBlock.RegisterTileEntity;
import openmods.utils.CachedFactory;
import openmods.utils.SneakyThrower;

public class GameConfigProvider {

	private interface IAnnotationProcessor<I, A extends Annotation> {
		public void process(I entry, A annotation);

		public String getEntryName(A annotation);

		public boolean isEnabled(String name);
	}

	private static final AbstractFeatureManager NULL_FEATURE_MANAGER = new AbstractFeatureManager() {
		@Override
		public boolean isEnabled(String category, String name) {
			return true;
		}

		@Override
		public Set<String> getCategories() {
			return ImmutableSet.of();
		}

		@Override
		public Set<String> getFeaturesInCategory(String category) {
			return ImmutableSet.of();
		}
	};

	private static final CachedFactory<Class<? extends ICustomItemModelProvider>, ICustomItemModelProvider> customItemModelProviders = new CachedFactory<Class<? extends ICustomItemModelProvider>, ICustomItemModelProvider>() {

		@Override
		protected ICustomItemModelProvider create(Class<? extends ICustomItemModelProvider> key) {
			try {
				return key.newInstance();
			} catch (Exception e) {
				throw SneakyThrower.sneakyThrow(e);
			}
		}
	};

	private AbstractFeatureManager features = NULL_FEATURE_MANAGER;

	private boolean remapFromLegacy = true;

	private final FactoryRegistry<Block> blockFactory = new FactoryRegistry<Block>();

	private final FactoryRegistry<Item> itemFactory = new FactoryRegistry<Item>();

	private final Map<String, Item> itemRemaps = Maps.newHashMap();

	private final Map<String, Block> blockRemaps = Maps.newHashMap();

	private final Map<Item, ResourceLocation> itemModelIds = Maps.newHashMap();

	private CreativeTabs creativeTab;

	private static class IdDecorator {
		private String modId;
		private final String joiner;

		public IdDecorator(String joiner) {
			this.joiner = joiner;
		}

		public void setMod(String modId) {
			this.modId = modId;
		}

		public String decorate(String id) {
			return modId + joiner + id;
		}
	}

	private static class ResourceLocationBuilder {
		private String modId;

		public void setMod(String modId) {
			this.modId = modId;
		}

		public ResourceLocation build(String id) {
			return new ResourceLocation(modId, id);
		}
	}

	private final IdDecorator langDecorator = new IdDecorator(".");

	private final ResourceLocationBuilder itemModelDecorator = new ResourceLocationBuilder();

	private final IdDecorator teDecorator = new IdDecorator("_");

	private final IdDecorator legacyItemDecorator = new IdDecorator(".");

	private final IdDecorator legacyBlockDecorator = new IdDecorator("_");

	private final String modId;

	private final ModContainer modContainer;

	public GameConfigProvider(String modPrefix) {
		langDecorator.setMod(modPrefix);
		itemModelDecorator.setMod(modPrefix);
		teDecorator.setMod(modPrefix);
		legacyBlockDecorator.setMod(modPrefix);
		legacyItemDecorator.setMod(modPrefix);

		this.modContainer = Loader.instance().activeModContainer();
		Preconditions.checkNotNull(this.modContainer, "This class can only be initialized in mod init");
		this.modId = this.modContainer.getModId();
	}

	public void setCreativeTab(CreativeTabs creativeTab) {
		this.creativeTab = creativeTab;
	}

	public void setLanguageModId(String modId) {
		langDecorator.setMod(modId);
	}

	public void setItemModelId(String modId) {
		itemModelDecorator.setMod(modId);
	}

	public void setTileEntityModId(String modId) {
		teDecorator.setMod(modId);
	}

	public void setFeatures(AbstractFeatureManager features) {
		this.features = features;
	}

	public void setRemapFromLegacy(boolean remapFromLegacy) {
		this.remapFromLegacy = remapFromLegacy;
	}

	public FactoryRegistry<Block> getBlockFactory() {
		return blockFactory;
	}

	public FactoryRegistry<Item> getItemFactory() {
		return itemFactory;
	}

	private static <I, A extends Annotation> void processAnnotations(Class<? extends InstanceContainer<?>> config, Class<I> fieldClass, Class<A> annotationClass, FactoryRegistry<I> factory, IAnnotationProcessor<I, A> processor) {
		for (Field f : config.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) && fieldClass.isAssignableFrom(f.getType())) {
				if (f.isAnnotationPresent(IgnoreFeature.class)) continue;
				A annotation = f.getAnnotation(annotationClass);
				if (annotation == null) {
					Log.warn("Field %s has valid type %s for registration, but no annotation %s", f, fieldClass, annotationClass);
					continue;
				}

				String name = processor.getEntryName(annotation);
				if (!processor.isEnabled(name)) {
					Log.info("Item %s (from field %s) is disabled", name, f);
					continue;
				}

				@SuppressWarnings("unchecked")
				Class<? extends I> fieldType = (Class<? extends I>)f.getType();
				I entry = factory.construct(name, fieldType);
				if (entry == null) continue;
				try {
					f.set(null, entry);
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
				processor.process(entry, annotation);
			}
		}
	}

	private interface IdSetter {
		public void setId(String id);
	}

	private static void setPrefixedId(String id, String objectName, IdDecorator decorator, IdSetter setter, String noneValue, String defaultValue) {
		if (!id.equals(RegisterBlock.NONE)) {
			if (id.equals(RegisterBlock.DEFAULT)) id = decorator.decorate(objectName);
			else id = decorator.decorate(id);
			setter.setId(id);
		}
	}

	private static void setItemPrefixedId(String id, String itemName, IdDecorator decorator, IdSetter setter) {
		setPrefixedId(id, itemName, decorator, setter, RegisterItem.NONE, RegisterItem.DEFAULT);
	}

	public void registerItems(Class<? extends ItemInstances> klazz) {
		processAnnotations(klazz, Item.class, RegisterItem.class, itemFactory, new IAnnotationProcessor<Item, RegisterItem>() {
			@Override
			public void process(final Item item, RegisterItem annotation) {
				final String name = annotation.name();

				final String legacyName = legacyItemDecorator.decorate(name);

				if (remapFromLegacy) {
					GameRegistry.registerItem(item, name);
					itemRemaps.put(modId + ":" + legacyName, item);
				} else {
					GameRegistry.registerItem(item, legacyName);
				}

				setItemPrefixedId(annotation.unlocalizedName(), name, langDecorator, new IdSetter() {
					@Override
					public void setId(String unlocalizedName) {
						item.setUnlocalizedName(unlocalizedName);
					}
				});

				final ResourceLocation itemLocation = itemModelDecorator.build(name);

				if (annotation.registerDefaultModel()) {
					itemModelIds.put(item, itemLocation);
				}

				if (annotation.customItemModels() != ICustomItemModelProvider.class) {
					registerCustomItemModels(item, itemLocation, annotation.customItemModels());
				}

				if (annotation.addToModCreativeTab()) {
					Preconditions.checkNotNull(creativeTab, "Trying to set creative tab, but none provided");
					item.setCreativeTab(creativeTab);
				}
			}

			@Override
			public String getEntryName(RegisterItem annotation) {
				return annotation.name();
			}

			@Override
			public boolean isEnabled(String name) {
				return features.isItemEnabled(name);
			}
		});
	}

	private static void registerCustomItemModels(final Item item, ResourceLocation itemLocation, Class<? extends ICustomItemModelProvider> providerCls) {

		final ICustomItemModelProvider provider = customItemModelProviders.getOrCreate(providerCls);
		provider.addCustomItemModels(item, itemLocation, new ICustomItemModelProvider.IModelRegistrationSink() {
			@Override
			public void register(int meta, ResourceLocation modelLocation) {
				OpenMods.proxy.registerCustomItemModel(item, meta, modelLocation);
			}
		});
	}

	private static void setBlockPrefixedId(String id, String blockName, IdDecorator decorator, IdSetter setter) {
		setPrefixedId(id, blockName, decorator, setter, RegisterBlock.NONE, RegisterBlock.DEFAULT);
	}

	public void registerBlocks(Class<? extends BlockInstances> klazz) {
		processAnnotations(klazz, Block.class, RegisterBlock.class, blockFactory, new IAnnotationProcessor<Block, RegisterBlock>() {
			@Override
			public void process(final Block block, RegisterBlock annotation) {
				final String name = annotation.name();
				final Class<? extends ItemBlock> itemBlockClass = annotation.itemBlock();
				Class<? extends TileEntity> teClass = annotation.tileEntity();
				if (teClass == TileEntity.class) teClass = null;

				final String legacyName = legacyBlockDecorator.decorate(name);

				if (remapFromLegacy) {
					GameRegistry.registerBlock(block, itemBlockClass, name);
					blockRemaps.put(modId + ":" + legacyName, block);
				} else {
					GameRegistry.registerBlock(block, itemBlockClass, legacyName);
				}

				setBlockPrefixedId(annotation.unlocalizedName(), name, langDecorator, new IdSetter() {
					@Override
					public void setId(String unlocalizedName) {
						block.setUnlocalizedName(unlocalizedName);
					}
				});

				if (teClass != null) {
					final String teName = teDecorator.decorate(name);
					GameRegistry.registerTileEntity(teClass, teName);
				}

				if (block instanceof IRegisterableBlock) ((IRegisterableBlock)block).setupBlock(modContainer, name, teClass, itemBlockClass);

				for (RegisterTileEntity te : annotation.tileEntities()) {
					final String teName = teDecorator.decorate(te.name());
					GameRegistry.registerTileEntity(te.cls(), teName);
				}

				if (annotation.addToModCreativeTab()) {
					Preconditions.checkNotNull(creativeTab, "Trying to set creative tab, but none provided");
					block.setCreativeTab(creativeTab);
				}

				final Item item = Item.getItemFromBlock(block);

				final ResourceLocation itemLocation = itemModelDecorator.build(name);

				if (annotation.customItemModels() != ICustomItemModelProvider.class) {
					registerCustomItemModels(item, itemLocation, annotation.customItemModels());
				}

				if (annotation.registerDefaultItemModel()) {
					itemModelIds.put(item, itemLocation);
				}
			}

			@Override
			public String getEntryName(RegisterBlock annotation) {
				return annotation.name();
			}

			@Override
			public boolean isEnabled(String name) {
				return features.isBlockEnabled(name);
			}
		});
	}

	public void handleRemaps(Collection<MissingMapping> mappings) {
		for (MissingMapping mapping : mappings) {
			switch (mapping.type) {
				case BLOCK: {
					Block remap = blockRemaps.get(mapping.name);
					if (remap != null) mapping.remap(remap);
					break;
				}
				case ITEM: {
					Item remap = itemRemaps.get(mapping.name);

					if (remap == null) {
						Block blockRemap = blockRemaps.get(mapping.name);
						if (blockRemap != null) remap = Item.getItemFromBlock(blockRemap);
					}

					if (remap != null) mapping.remap(remap);
				}

				default:
					break;

			}
		}
	}

	public void registerItemModels() {
		for (Map.Entry<Item, ResourceLocation> modelId : itemModelIds.entrySet())
			OpenMods.proxy.bindItemModelToItemMeta(modelId.getKey(), 0, modelId.getValue());
	}

}
