package openmods.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
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
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import openmods.Log;
import openmods.config.RegisterBlock.RegisterTileEntity;
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
			.put(int.class, Property.Type.INTEGER)
			.put(Boolean.class, Property.Type.BOOLEAN)
			.put(boolean.class, Property.Type.BOOLEAN)
			.put(Byte.class, Property.Type.INTEGER)
			.put(byte.class, Property.Type.INTEGER)
			.put(Double.class, Property.Type.DOUBLE)
			.put(double.class, Property.Type.DOUBLE)
			.put(Float.class, Property.Type.DOUBLE)
			.put(float.class, Property.Type.DOUBLE)
			.put(Long.class, Property.Type.INTEGER)
			.put(long.class, Property.Type.INTEGER)
			.put(Short.class, Property.Type.INTEGER)
			.put(short.class, Property.Type.INTEGER)
			.put(String.class, Property.Type.STRING)
			.build();

	private static String[] toStringArray(Object array) {
		Preconditions.checkArgument(array.getClass().isArray(), "Type %s is not an array", array.getClass());
		int length = Array.getLength(array);
		String[] result = new String[length];
		for (int i = 0; i < length; i++)
			result[i] = Array.get(array, i).toString();

		return result;
	}

	private abstract static class FieldProcessing {
		public void getProperty(Configuration configFile, Field f, String category, String name, String comment) {
			if (Strings.isNullOrEmpty(name)) name = f.getName();
			if (Strings.isNullOrEmpty(category)) category = null;

			final Object defaultValue;
			try {
				defaultValue = f.get(null);
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}

			Preconditions.checkNotNull(defaultValue, "Config field %s has no default value", name);
			final Class<?> fieldType = getFieldType(defaultValue);

			final Property.Type expectedType = CONFIG_TYPES.get(fieldType);
			Preconditions.checkNotNull(expectedType, "Config field %s has no property type mapping", name);

			final IStringSerializable<?> converter = TypeRW.TYPES.get(fieldType);
			Preconditions.checkNotNull(converter, "Config field %s has no known conversion from string", name);

			final Property property = getProperty(configFile, category, name, comment, expectedType, defaultValue);
			// return on newly created value. Due to forge bug list properties
			// don't set this value properly
			if (!property.wasRead() && !property.isList()) return;

			final Type actualType = property.getType();

			if (expectedType != actualType) {
				Log.warn("Invalid config property type '%s', using default value", property.getType(), expectedType);
				return;
			}

			Object value = convertValue(property, converter, fieldType);
			if (value != null) {
				try {
					f.set(null, value);
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
			}
		}

		protected abstract Property getProperty(Configuration configFile, String category, String name, String comment, Type expectedType, Object defaultValue);

		protected abstract Class<? extends Object> getFieldType(Object defaultValue);

		protected abstract Object convertValue(Property property, IStringSerializable<?> converter, Class<?> targetType);
	}

	private static final FieldProcessing SINGLE_VALUE = new FieldProcessing() {
		@Override
		protected Property getProperty(Configuration configFile, String category, String name, String comment, Type expectedType, Object defaultValue) {
			final String defaultString = defaultValue.toString();
			return configFile.get(category, name, defaultString, comment, expectedType);
		}

		@Override
		protected Class<? extends Object> getFieldType(Object defaultValue) {
			return defaultValue.getClass();
		}

		@Override
		protected Object convertValue(Property property, IStringSerializable<?> converter, Class<?> targetType) {
			final String value = property.getString();
			try {
				return converter.readFromString(value);
			} catch (StringConversionException e) {
				Log.warn(e, "Invalid config property value '%s', using default value", value);
			}
			return null;
		}
	};

	private static final FieldProcessing MULTIPLE_VALUES = new FieldProcessing() {

		@Override
		protected Property getProperty(Configuration configFile, String category, String name, String comment, Type expectedType, Object defaultValue) {
			final String[] defaultStrings = toStringArray(defaultValue);
			return configFile.get(category, name, defaultStrings, comment, expectedType);
		}

		@Override
		protected Class<? extends Object> getFieldType(Object defaultValue) {
			return defaultValue.getClass().getComponentType();
		}

		@Override
		protected Object convertValue(Property property, IStringSerializable<?> converter, Class<?> targetType) {
			final String[] values = property.getStringList();
			final Object result = Array.newInstance(targetType, values.length);
			for (int i = 0; i < values.length; i++) {
				String value = values[i];
				Object converted;
				try {
					converted = converter.readFromString(value);
				} catch (StringConversionException e) {
					Log.warn(e, "Invalid config property value '%s' at index %d, using default", value, i);
					return null;
				}
				try {
					Array.set(result, i, converted);
				} catch (IllegalArgumentException e) {
					Log.warn(e, "Invalid config property value '%s' at index %d, using default", value, i);
					return null;
				}
			}
			return result;
		}
	};

	private static void getProperty(Configuration configFile, Field f, String category, String name, String comment) {
		Class<?> fieldType = f.getType();
		FieldProcessing p = fieldType.isArray()? MULTIPLE_VALUES : SINGLE_VALUE;
		p.getProperty(configFile, f, category, name, comment);
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

	private static String dotName(String a, String b) {
		return a + "." + b;
	}

	private static String underscoreName(String a, String b) {
		return a + "_" + b;
	}

	public static void registerItems(Class<?> klazz, final String mod) {
		processAnnotations(klazz, Item.class, RegisterItem.class, new IAnnotationProcessor<Item, RegisterItem>() {
			@Override
			public void process(Item item, RegisterItem annotation) {
				String name = dotName(mod, annotation.name());
				GameRegistry.registerItem(item, name);

				String unlocalizedName = annotation.unlocalizedName();
				if (!unlocalizedName.equals(RegisterItem.NONE)) {
					if (unlocalizedName.equals(RegisterItem.DEFAULT)) unlocalizedName = name;
					else unlocalizedName = dotName(mod, unlocalizedName);
					item.setUnlocalizedName(unlocalizedName);
				}
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

				final String blockName = underscoreName(mod, name);

				GameRegistry.registerBlock(block, itemBlock, blockName);

				String unlocalizedName = annotation.unlocalizedName();
				if (!unlocalizedName.equals(RegisterBlock.NONE)) {
					if (unlocalizedName.equals(RegisterBlock.DEFAULT)) unlocalizedName = dotName(mod, name);
					else unlocalizedName = dotName(mod, unlocalizedName);
					block.setUnlocalizedName(unlocalizedName);
				}

				if (teClass != null) GameRegistry.registerTileEntity(teClass, blockName);

				if (block instanceof IRegisterableBlock) ((IRegisterableBlock)block).setupBlock(mod, name, teClass, itemBlock);

				for (RegisterTileEntity te : annotation.tileEntities()) {
					final String teName = underscoreName(mod, te.name());
					GameRegistry.registerTileEntity(te.cls(), teName);
				}
			}
		});
	}

	public static void registerFluids(Class<?> klazz, final String mod) {
		processAnnotations(klazz, Fluid.class, RegisterFluid.class, new IAnnotationProcessor<Fluid, RegisterFluid>() {
			@Override
			public void process(Fluid fluid, RegisterFluid annotation) {
				fluid.setDensity(annotation.density());
				fluid.setGaseous(annotation.gaseous());
				fluid.setLuminosity(annotation.luminosity());
				fluid.setViscosity(annotation.viscosity());
				FluidRegistry.registerFluid(fluid);
				fluid.setUnlocalizedName(String.format("%s.%s", mod, annotation.name()));

			}
		});
	}
}
