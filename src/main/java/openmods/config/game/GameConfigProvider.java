package openmods.config.game;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import openmods.Log;
import openmods.config.BlockInstances;
import openmods.config.ItemInstances;
import openmods.config.game.RegisterBlock.RegisterTileEntity;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.registry.GameRegistry;

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
	};

	private AbstractFeatureManager features = NULL_FEATURE_MANAGER;

	private boolean remapFromLegacy = true;

	private final FactoryRegistry<Block> blockFactory = new FactoryRegistry<Block>();

	private final FactoryRegistry<Item> itemFactory = new FactoryRegistry<Item>();

	private final Map<String, Item> itemRemaps = Maps.newHashMap();

	private final Map<String, Block> blockRemaps = Maps.newHashMap();

	private final String modPrefix;

	private final String modId;

	public GameConfigProvider(String modPrefix) {
		this.modPrefix = modPrefix;

		ModContainer mod = Loader.instance().activeModContainer();
		Preconditions.checkNotNull(mod, "This class can only be initialized in mod init");
		this.modId = mod.getModId();
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

	private static <I, A extends Annotation> void processAnnotations(Class<?> config, Class<I> fieldClass, Class<A> annotationClass, FactoryRegistry<I> factory, IAnnotationProcessor<I, A> processor) {
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

	private static String dotName(String a, String b) {
		return a + "." + b;
	}

	private static String underscoreName(String a, String b) {
		return a + "_" + b;
	}

	public void registerItems(Class<? extends ItemInstances> klazz) {
		processAnnotations(klazz, Item.class, RegisterItem.class, itemFactory, new IAnnotationProcessor<Item, RegisterItem>() {
			@Override
			public void process(Item item, RegisterItem annotation) {
				final String name = annotation.name();
				String unlocalizedName = annotation.unlocalizedName();

				final String prefixedName = dotName(modPrefix, name);

				if (remapFromLegacy) {
					GameRegistry.registerItem(item, name);
					itemRemaps.put(modId + ":" + prefixedName, item);
				} else {
					GameRegistry.registerItem(item, prefixedName);
				}

				if (!unlocalizedName.equals(RegisterItem.NONE)) {
					if (unlocalizedName.equals(RegisterItem.DEFAULT)) unlocalizedName = prefixedName;
					else unlocalizedName = dotName(modPrefix, unlocalizedName);
					item.setUnlocalizedName(unlocalizedName);
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

	public void registerBlocks(Class<? extends BlockInstances> klazz) {
		processAnnotations(klazz, Block.class, RegisterBlock.class, blockFactory, new IAnnotationProcessor<Block, RegisterBlock>() {
			@Override
			public void process(Block block, RegisterBlock annotation) {
				final String name = annotation.name();
				final Class<? extends ItemBlock> itemBlockClass = annotation.itemBlock();
				Class<? extends TileEntity> teClass = annotation.tileEntity();
				if (teClass == TileEntity.class) teClass = null;

				final String prefixedName = underscoreName(modPrefix, name);

				if (remapFromLegacy) {
					GameRegistry.registerBlock(block, itemBlockClass, name);
					blockRemaps.put(modId + ":" + prefixedName, block);
				} else {
					GameRegistry.registerBlock(block, itemBlockClass, prefixedName);
				}

				String unlocalizedName = annotation.unlocalizedName();
				if (!unlocalizedName.equals(RegisterBlock.NONE)) {
					if (unlocalizedName.equals(RegisterBlock.DEFAULT)) unlocalizedName = dotName(modPrefix, name);
					else unlocalizedName = dotName(modPrefix, unlocalizedName);
					block.setBlockName(unlocalizedName);
				}

				if (teClass != null) GameRegistry.registerTileEntity(teClass, prefixedName);

				if (block instanceof IRegisterableBlock) ((IRegisterableBlock)block).setupBlock(modPrefix, name, teClass, itemBlockClass);

				for (RegisterTileEntity te : annotation.tileEntities()) {
					final String teName = underscoreName(modPrefix, te.name());
					GameRegistry.registerTileEntity(te.cls(), teName);
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

}
