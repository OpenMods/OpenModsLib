package openmods.config.game;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.DataFixer;
import net.minecraftforge.event.RegistryEvent.MissingMappings.Mapping;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import openmods.Log;
import openmods.OpenMods;
import openmods.config.BlockInstances;
import openmods.config.InstanceContainer;
import openmods.config.ItemInstances;
import openmods.config.game.RegisterBlock.RegisterTileEntity;
import openmods.fixers.IFixerFactory;
import openmods.fixers.RegisterFixer;
import openmods.utils.CachedInstanceFactory;

public class GameRegistryObjectsProvider {

	private interface IAnnotationAccess<A extends Annotation, I> {
		String getEntryId(A annotation);

		Class<? extends I> getObjectType(A annotation);

		boolean isEnabled(String name);
	}

	@FunctionalInterface
	private interface IObjectVisitor<I, A extends Annotation> {
		void visit(I entry, A annotation);
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

	private AbstractFeatureManager features = NULL_FEATURE_MANAGER;

	private boolean remapFromLegacy = true;

	private final FactoryRegistry<Block> blockFactory = new FactoryRegistry<>();

	private final FactoryRegistry<Item> itemFactory = new FactoryRegistry<>();

	private final Map<ResourceLocation, Item> itemRemaps = Maps.newHashMap();

	private final Map<ResourceLocation, Block> blockRemaps = Maps.newHashMap();

	private final Map<Item, ResourceLocation> itemModelIds = Maps.newHashMap();

	private Supplier<CreativeTabs> creativeTabSupplier;
	private CreativeTabs creativeTab;

	private CreativeTabs creativeTab() {
		if (creativeTab == null) {
			if (creativeTabSupplier != null) {
				creativeTab = creativeTabSupplier.get();
				Preconditions.checkNotNull(creativeTab);
			}
		}

		return creativeTab;
	}

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

	private final IdDecorator legacyItemDecorator = new IdDecorator(".");

	private final IdDecorator legacyBlockDecorator = new IdDecorator("_");

	private final String modId;

	private final Set<String> legacyModIds = Sets.newHashSet();

	private final ModContainer modContainer;

	public GameRegistryObjectsProvider(String modPrefix) {
		langDecorator.setMod(modPrefix);
		itemModelDecorator.setMod(modPrefix);
		legacyBlockDecorator.setMod(modPrefix);
		legacyItemDecorator.setMod(modPrefix);

		this.modContainer = Loader.instance().activeModContainer();
		Preconditions.checkNotNull(this.modContainer, "This class can only be initialized in mod init");
		this.modId = this.modContainer.getModId();
	}

	public void setCreativeTab(Supplier<CreativeTabs> creativeTab) {
		this.creativeTabSupplier = creativeTab;
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

	public void setFeatures(AbstractFeatureManager features) {
		this.features = features;
	}

	public void setRemapFromLegacy(boolean remapFromLegacy) {
		this.remapFromLegacy = remapFromLegacy;
	}

	public void addModIdToRemap(String legacyModId) {
		Preconditions.checkArgument(!legacyModId.equals(this.modId));
		legacyModIds.add(legacyModId);
	}

	public FactoryRegistry<Block> getBlockFactory() {
		return blockFactory;
	}

	public FactoryRegistry<Item> getItemFactory() {
		return itemFactory;
	}

	private static <I, A extends Annotation> void processAnnotations(Class<? extends InstanceContainer<?>> config, Class<I> baseClass, Class<A> annotationClass, FactoryRegistry<I> factory, IAnnotationAccess<A, I> annotationAccess, IObjectVisitor<I, A> visitor) {
		for (Field f : config.getFields()) {
			if (f.isAnnotationPresent(IgnoreFeature.class)) continue;
			final A annotation = f.getAnnotation(annotationClass);
			if (annotation == null) continue;

			Preconditions.checkState(Modifier.isStatic(f.getModifiers()), "Field %s marked with %s must be static", f, annotationClass);

			final Class<? extends I> fieldType = getFieldType(f, baseClass);

			Class<? extends I> targetCls = annotationAccess.getObjectType(annotation);
			if (targetCls == baseClass) {
				targetCls = fieldType;
			}

			String name = annotationAccess.getEntryId(annotation);
			if (!annotationAccess.isEnabled(name)) {
				Log.info("Object %s (from field %s) is disabled", name, f);
				continue;
			}

			I entry = factory.construct(name, targetCls);
			if (entry == null) continue;
			try {
				f.set(null, entry);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			visitor.visit(entry, annotation);
		}
	}

	@SuppressWarnings("unchecked")
	private static <I> Class<? extends I> getFieldType(Field field, Class<I> baseClass) {
		final Class<?> type = field.getType();
		Preconditions.checkState(baseClass.isAssignableFrom(type), "Field %s must have type assignable to %s", field, baseClass);
		return (Class<? extends I>)type;
	}

	@FunctionalInterface
	private interface IdSetter {
		void setId(String id);
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

	private <E> void registerRemaps(Map<ResourceLocation, E> output, E object, String currentName, Iterable<String> legacyObjectNames, Iterable<String> legacyModIds) {
		for (String legacyName : legacyObjectNames)
			output.put(new ResourceLocation(modId, legacyName), object);

		for (String legacyModId : legacyModIds) {
			output.put(new ResourceLocation(legacyModId, currentName), object);

			for (String legacyName : legacyObjectNames)
				output.put(new ResourceLocation(legacyModId, legacyName), object);
		}
	}

	public void registerItems(Class<? extends ItemInstances> klazz, IForgeRegistry<Item> items) {
		processAnnotations(klazz, Item.class, RegisterItem.class, itemFactory,
				new IAnnotationAccess<RegisterItem, Item>() {
					@Override
					public String getEntryId(RegisterItem annotation) {
						return annotation.id();
					}

					@Override
					public Class<? extends Item> getObjectType(RegisterItem annotation) {
						return annotation.type();
					}

					@Override
					public boolean isEnabled(String id) {
						return features.isItemEnabled(id);
					}
				},
				(item, annotation) -> {
					final String id = annotation.id();
					final Set<String> legacyIds = Sets.newHashSet(annotation.legacyIds());

					final String legacyPrefixedId = legacyItemDecorator.decorate(id);

					final String selectedId;
					if (remapFromLegacy) {
						selectedId = id;
						legacyIds.add(legacyPrefixedId);
					} else {
						selectedId = legacyPrefixedId;
					}

					items.register(item.setRegistryName(new ResourceLocation(modId, selectedId)));

					registerRemaps(itemRemaps, item, selectedId, legacyIds, legacyModIds);

					setItemPrefixedId(annotation.unlocalizedName(), id, langDecorator, item::setUnlocalizedName);

					final ResourceLocation itemLocation = itemModelDecorator.build(id);

					if (annotation.registerDefaultModel()) {
						itemModelIds.put(item, itemLocation);
					}

					if (annotation.customItemModels() != ICustomItemModelProvider.class) {
						registerCustomItemModels(item, itemLocation, annotation.customItemModels());
					}

					if (annotation.addToModCreativeTab())
						item.setCreativeTab(creativeTab());
				});
	}

	private static void registerCustomItemModels(Item item, ResourceLocation itemLocation, Class<? extends ICustomItemModelProvider> providerCls) {
		OpenMods.proxy.runCustomItemModelProvider(itemLocation, item, providerCls);
	}

	private static void setBlockPrefixedId(String id, String blockName, IdDecorator decorator, IdSetter setter) {
		setPrefixedId(id, blockName, decorator, setter, RegisterBlock.NONE, RegisterBlock.DEFAULT);
	}

	private static final Class<?>[] ITEM_BLOCK_CTOR_ARGS = new Class<?>[] { Block.class };

	private static ItemBlock initializeItemBlock(Class<? extends ItemBlock> cls, Block block) {
		try {
			final Constructor<? extends ItemBlock> itemBlockCtor = cls.getConstructor(ITEM_BLOCK_CTOR_ARGS);
			return itemBlockCtor.newInstance(block);
		} catch (Exception e) {
			Log.warn("Failed to initialize block item for %s, class %s", block, cls);
			return null;
		}
	}

	public void registerBlocks(Class<? extends BlockInstances> klazz, IForgeRegistry<Block> blocks, IForgeRegistry<Item> items) {
		final CachedInstanceFactory<IFixerFactory> fixerFactories = CachedInstanceFactory.create();
		final DataFixer fixerRegistry = FMLCommonHandler.instance().getDataFixer();
		processAnnotations(klazz, Block.class, RegisterBlock.class, blockFactory,
				new IAnnotationAccess<RegisterBlock, Block>() {
					@Override
					public String getEntryId(RegisterBlock annotation) {
						return annotation.id();
					}

					@Override
					public Class<? extends Block> getObjectType(RegisterBlock annotation) {
						return annotation.type();
					}

					@Override
					public boolean isEnabled(String id) {
						return features.isBlockEnabled(id);
					}
				},
				new IObjectVisitor<Block, RegisterBlock>() {
					@Override
					public void visit(final Block block, RegisterBlock annotation) {
						final String id = annotation.id();

						final Class<? extends ItemBlock> itemBlockClass = annotation.itemBlock();

						final Set<String> legacyIds = Sets.newHashSet(annotation.legacyIds());

						final String legacyPrefixedId = legacyItemDecorator.decorate(id);

						final String selectedId;
						if (remapFromLegacy) {
							selectedId = id;
							legacyIds.add(legacyPrefixedId);
						} else {
							selectedId = legacyPrefixedId;
						}

						blocks.register(block.setRegistryName(new ResourceLocation(modId, selectedId)));

						registerRemaps(blockRemaps, block, selectedId, legacyIds, legacyModIds);

						final ItemBlock itemBlock;

						if (annotation.registerItemBlock()) {
							itemBlock = initializeItemBlock(itemBlockClass, block);
							if (itemBlock != null) {
								items.register(itemBlock.setRegistryName(new ResourceLocation(modId, selectedId)));
								registerRemaps(itemRemaps, itemBlock, selectedId, legacyIds, legacyModIds);
							}
						} else {
							itemBlock = null;
						}

						setBlockPrefixedId(annotation.unlocalizedName(), id, langDecorator, block::setUnlocalizedName);

						Class<? extends TileEntity> mainTeClass = null;
						final Class<? extends TileEntity> teClass = annotation.tileEntity();
						if (teClass != TileEntity.class) {
							registerTileEntity(new ResourceLocation(modId, id), teClass);
							mainTeClass = teClass;
						}

						for (RegisterTileEntity te : annotation.tileEntities()) {
							registerTileEntity(new ResourceLocation(modId, te.name()), te.cls());
							if (te.main()) {
								Preconditions.checkState(mainTeClass == null, "Multiple main TEs");
								mainTeClass = te.cls();
							}
						}

						if (block instanceof IRegisterableBlock) ((IRegisterableBlock)block).setupBlock(modContainer, id, mainTeClass, itemBlock);

						if (annotation.addToModCreativeTab())
							block.setCreativeTab(creativeTab());

						if (itemBlock != null) {
							final ResourceLocation itemLocation = itemModelDecorator.build(id);

							if (annotation.customItemModels() != ICustomItemModelProvider.class) {
								registerCustomItemModels(itemBlock, itemLocation, annotation.customItemModels());
							}

							if (annotation.registerDefaultItemModel()) {
								itemModelIds.put(itemBlock, itemLocation);
							}
						}
					}

					private void registerTileEntity(ResourceLocation id, Class<? extends TileEntity> cls) {
						ResourceLocation oldTeId = TileEntity.getKey(cls);
						if (oldTeId == null) {
							GameRegistry.registerTileEntity(cls, id);
							registerFixer(cls);
						} else {
							Preconditions.checkState(oldTeId.equals(id), "Duplicate TE class %s registration: %s -> %s", cls, oldTeId, id);
						}
					}

					private void registerFixer(Class<? extends TileEntity> teClass) {
						RegisterFixer fixer = teClass.getAnnotation(RegisterFixer.class);
						if (fixer != null) {
							final IFixerFactory fixerFactory = fixerFactories.getOrCreate(fixer.value());
							fixerFactory.register(fixerRegistry, teClass);
						}
					}
				});
	}

	public boolean hasIntraModRenames() {
		return !legacyModIds.isEmpty();
	}

	public void handleBlockRemaps(Collection<Mapping<Block>> mappings) {
		for (Mapping<Block> mapping : mappings) {
			Block remap = blockRemaps.get(mapping.key);
			if (remap != null) mapping.remap(remap);
		}
	}

	public void handleItemRemaps(Collection<Mapping<Item>> mappings) {
		for (Mapping<Item> mapping : mappings) {
			Item remap = itemRemaps.get(mapping.key);
			if (remap != null) mapping.remap(remap);
		}
	}

	public void registerItemModels() {
		for (Map.Entry<Item, ResourceLocation> modelId : itemModelIds.entrySet())
			OpenMods.proxy.bindItemModelToItemMeta(modelId.getKey(), 0, modelId.getValue());
	}

}
