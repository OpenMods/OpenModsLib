package openmods.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import net.minecraftforge.common.Property.Type;
import openmods.Log;
import openmods.utils.io.IStringSerializable;
import openmods.utils.io.StringConversionException;
import openmods.utils.io.TypeRW;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import cpw.mods.fml.common.registry.GameRegistry;

public class ConfigProcessing {

	public static final Map<Class<?>, Property.Type> CONFIG_TYPES = ImmutableMap.<Class<?>, Property.Type> builder()
			.put(Integer.class, Property.Type.INTEGER)
			.put(Boolean.class, Property.Type.BOOLEAN)
			.put(Byte.class, Property.Type.INTEGER)
			.put(Double.class, Property.Type.DOUBLE)
			.put(Float.class, Property.Type.DOUBLE)
			.put(Long.class, Property.Type.INTEGER)
			.put(Short.class, Property.Type.INTEGER)
			.put(String.class, Property.Type.STRING)
			.build();

	private static void getProperty(Configuration configFile, Field f, String category, String name, String comment) {
		if (Strings.isNullOrEmpty(name)) name = f.getName();
		if (Strings.isNullOrEmpty(category)) category = null;

		final Object defaultValue;
		try {
			defaultValue = f.get(null);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		Preconditions.checkNotNull(defaultValue, "Config field %s has no default value", name);
		// f.getType may return primitive type, so let's use value
		final Class<?> fieldType = defaultValue.getClass();

		final Property.Type expectedType = CONFIG_TYPES.get(fieldType);
		Preconditions.checkNotNull(expectedType, "Config field %s has no property type mapping", name);

		final IStringSerializable<?> converter = TypeRW.TYPES.get(fieldType);
		Preconditions.checkNotNull(converter, "Config field %s has no known conversion from string", name);

		final String defaultString = defaultValue.toString();
		final Property property = configFile.get(category, name, defaultString, comment, expectedType);
		if (property.hasChanged()) return; // newly created value

		final String valueString = property.getString();

		final Type actualType = property.getType();
		if (expectedType != actualType) {
			Log.warn("Invalid config property type '%s', using default value '%s' of type '%s'", property.getType(), defaultString, expectedType);
		} else if (!valueString.equals(defaultString)) {
			try {
				Object value = converter.readFromString(valueString);
				try {
					f.set(null, value);
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
			} catch (StringConversionException e) {
				Log.warn(e, "Invalid config property value '%s', using default '%s'", valueString, defaultString);
				property.set(defaultString);
			}
		}
	}

	private static void getBlock(Configuration configFile, Field field, String description) {
		try {
			int defaultValue = field.getInt(null);
			Property prop = configFile.getBlock("block", field.getName(), defaultValue, description);
			field.set(null, prop.getInt());
		} catch (Throwable e) {
			throw Throwables.propagate(e);
		}
	}

	private static void getItem(Configuration configFile, Field field, String description) {
		try {
			int defaultValue = field.getInt(null);
			Property prop = configFile.getItem("item", field.getName(), defaultValue, description);
			field.set(null, prop.getInt());
		} catch (Throwable e) {
			throw Throwables.propagate(e);
		}
	}

	public static boolean canRegisterBlock(int blockId) {
		if (blockId > 0) {
			Preconditions.checkState(Block.blocksList[blockId] == null,
					"OpenBlocks tried to register a block for ID: %s but it was in use", blockId);
			return true;
		}
		return false; // Block disabled, fail silently
	}

	public static void processAnnotations(Configuration configFile, Class<?> klazz) {
		for (Field f : klazz.getFields()) {
			{
				ItemId a = f.getAnnotation(ItemId.class);
				if (a != null) {
					getItem(configFile, f, a.description());
					continue;
				}
			}

			{
				BlockId a = f.getAnnotation(BlockId.class);
				if (a != null) {
					getBlock(configFile, f, a.description());
				}
			}

			{
				ConfigProperty a = f.getAnnotation(ConfigProperty.class);
				if (a != null) {
					getProperty(configFile, f, a.category(), a.name(), a.comment());
				}
			}
		}
	}

	private interface IAnnotationProcessor<I, A extends Annotation> {
		public void process(I entry, A annotation);
	}

	public static <I, A extends Annotation> void processAnnotations(Class<?> config, Class<I> fieldClass, Class<A> annotationClass, IAnnotationProcessor<I, A> processor) {
		for (Field f : config.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) && fieldClass.isAssignableFrom(f.getType())) {
				A annotation = f.getAnnotation(annotationClass);
				if (annotation != null) {
					try {
						@SuppressWarnings("unchecked")
						I entry = (I)f.get(null);
						if (entry != null) processor.process(entry, annotation);
					} catch (Exception e) {
						throw Throwables.propagate(e);
					}
				} else {
					Log.warn("Field %s has valid type %s for registration, but no annotation %s", f, fieldClass, annotationClass);
				}
			}
		}
	}

	public static void registerItems(Class<?> klazz, final String mod) {
		processAnnotations(klazz, Item.class, RegisterItem.class, new IAnnotationProcessor<Item, RegisterItem>() {
			@Override
			public void process(Item item, RegisterItem annotation) {
				String name = String.format("%s.%s", mod, annotation.name());
				GameRegistry.registerItem(item, name);
			}
		});
	}

	public static void registerBlocks(Class<?> klazz, final String mod) {
		processAnnotations(klazz, Block.class, RegisterBlock.class, new IAnnotationProcessor<Block, RegisterBlock>() {
			@Override
			public void process(Block block, RegisterBlock annotation) {
				final String name = annotation.name();
				final Class<? extends ItemBlock> itemBlock = annotation.itemBlock();
				Class<? extends TileEntity> teClass = annotation.tileEntity();
				if (teClass == TileEntity.class) teClass = null;

				GameRegistry.registerBlock(block, itemBlock, String.format("%s_%s", mod, name));
				block.setUnlocalizedName(String.format("%s.%s", mod, name));

				if (teClass != null) GameRegistry.registerTileEntity(teClass, String.format("%s_%s", mod, name));

				if (block instanceof IRegisterableBlock) ((IRegisterableBlock)block).setupBlock(mod, name, teClass, itemBlock);
			}
		});
	}
}
