package openmods.config;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

import net.minecraftforge.common.*;
import net.minecraftforge.common.Property.Type;
import openmods.Log;
import openmods.utils.io.IStringSerializable;
import openmods.utils.io.StringConversionException;
import openmods.utils.io.TypeRW;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;

public abstract class ConfigPropertyMeta {
	public enum Result {
		CANCELLED,
		ONLINE,
		OFFLINE
	}

	public final String name;
	public final String category;
	public final String comment;
	public final Property.Type type;
	protected final Field field;
	private final boolean onLine;
	private final Object defaultValue;
	private final String[] defaultText;

	protected final IStringSerializable<?> converter;
	protected final Property wrappedProperty;
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

	protected ConfigPropertyMeta(Configuration config, Field field, ConfigProperty annotation) {
		this.comment = annotation.comment();
		this.category = annotation.category();
		OnLineModifiable mod = field.getAnnotation(OnLineModifiable.class);
		this.onLine = mod != null;

		String name = annotation.name();
		String category = annotation.category();

		if (Strings.isNullOrEmpty(name)) name = field.getName();
		if (Strings.isNullOrEmpty(category)) category = null;

		this.name = name;
		this.field = field;

		defaultValue = getFieldValue();
		Preconditions.checkNotNull(defaultValue, "Config field %s has no default value", name);
		defaultText = convertToStringArray(defaultValue);

		final Class<?> fieldType = getFieldType();
		type = ConfigPropertyMeta.CONFIG_TYPES.get(fieldType);
		Preconditions.checkNotNull(type, "Config field %s has no property type mapping", name);

		converter = TypeRW.TYPES.get(fieldType);
		Preconditions.checkNotNull(converter, "Config field %s has no known conversion from string", name);

		wrappedProperty = getProperty(config, type, defaultValue);
	}

	void updateValueFromConfig(boolean force) {
		// return on newly created value. Due to forge bug list properties
		// don't set this value properly
		if (!force && !wrappedProperty.wasRead() && !wrappedProperty.isList()) return;

		final Type actualType = wrappedProperty.getType();

		Preconditions.checkState(type == actualType, "Invalid config property type '%s', expected '%s'", actualType, type);

		String[] currentValue = getPropertyValue();
		try {
			Object converted = convertValue(currentValue);
			setFieldValue(converted);
		} catch (StringConversionException e) {
			Log.warn(e, "Invalid config property value %s, using default value", Arrays.toString(currentValue));
		}
	}

	protected void setFieldValue(Object value) {
		try {
			field.set(null, value);
		} catch (Throwable e) {
			throw Throwables.propagate(e);
		}
	}

	protected Object getFieldValue() {
		try {
			return field.get(null);
		} catch (Throwable t) {
			throw Throwables.propagate(t);
		}
	}

	protected abstract Class<? extends Object> getFieldType();

	protected abstract Property getProperty(Configuration configFile, Type expectedType, Object defaultValue);

	public abstract String[] getPropertyValue();

	protected abstract void setPropertyValue(String... values);

	protected abstract Object convertValue(String... values);

	public abstract boolean acceptsMultipleValues();

	public abstract String valueDescription();

	protected abstract String[] convertToStringArray(Object value);

	public Result tryChangeValue(String... proposedValues) {
		ConfigurationChange.Pre evt = new ConfigurationChange.Pre(name, category, proposedValues);
		if (MinecraftForge.EVENT_BUS.post(evt)) return Result.CANCELLED;

		Object converted = convertValue(evt.proposedValues);

		if (onLine) setFieldValue(converted);

		MinecraftForge.EVENT_BUS.post(new ConfigurationChange.Post(name, category));

		setPropertyValue(evt.proposedValues);

		return onLine? Result.ONLINE : Result.OFFLINE;
	}

	public String[] getDefaultValues() {
		return defaultText.clone();
	}

	private static class SingleValue extends ConfigPropertyMeta {

		protected SingleValue(Configuration config, Field field, ConfigProperty annotation) {
			super(config, field, annotation);
		}

		@Override
		protected Class<? extends Object> getFieldType() {
			return field.getType();
		}

		@Override
		protected Property getProperty(Configuration configFile, Type expectedType, Object defaultValue) {
			final String defaultString = defaultValue.toString();
			return configFile.get(category, name, defaultString, comment, expectedType);
		}

		@Override
		protected Object convertValue(String... values) {
			Preconditions.checkArgument(values.length == 1, "This parameter has only one value");
			final String value = values[0];
			return converter.readFromString(value);
		}

		@Override
		public String[] getPropertyValue() {
			return new String[] { wrappedProperty.getString() };
		}

		@Override
		protected void setPropertyValue(String... values) {
			Preconditions.checkArgument(values.length == 1, "This parameter has only one value");
			wrappedProperty.set(values[0]);
		}

		@Override
		public boolean acceptsMultipleValues() {
			return false;
		}

		@Override
		public String valueDescription() {
			return wrappedProperty.getString();
		}

		@Override
		protected String[] convertToStringArray(Object value) {
			return new String[] { value.toString() };
		}
	}

	private static class MultipleValues extends ConfigPropertyMeta {

		protected MultipleValues(Configuration config, Field field, ConfigProperty annotation) {
			super(config, field, annotation);
		}

		@Override
		protected Class<? extends Object> getFieldType() {
			return field.getType().getComponentType();
		}

		@Override
		protected Property getProperty(Configuration configFile, Type expectedType, Object defaultValue) {
			final String[] defaultStrings = convertToStringArray(defaultValue);
			return configFile.get(category, name, defaultStrings, comment, expectedType);
		}

		@Override
		protected Object convertValue(String... values) {
			final Object result = Array.newInstance(field.getType().getComponentType(), values.length);
			final CharMatcher matcher = CharMatcher.is('"');
			for (int i = 0; i < values.length; i++) {
				final String value = matcher.trimFrom(StringUtils.strip(values[i]));
				final Object converted = converter.readFromString(value);
				Array.set(result, i, converted);
			}
			return result;
		}

		@Override
		public String[] getPropertyValue() {
			return wrappedProperty.getStringList();
		}

		@Override
		protected void setPropertyValue(String... values) {
			wrappedProperty.set(values);
		}

		@Override
		public boolean acceptsMultipleValues() {
			return true;
		}

		@Override
		public String valueDescription() {
			return Arrays.toString(wrappedProperty.getStringList());
		}

		@Override
		protected String[] convertToStringArray(Object value) {
			Preconditions.checkArgument(value.getClass().isArray(), "Type %s is not an array", value.getClass());
			int length = Array.getLength(value);
			String[] result = new String[length];
			for (int i = 0; i < length; i++)
				result[i] = String.format("\"%s\"", Array.get(value, i).toString());

			return result;
		}
	}

	public static ConfigPropertyMeta createMetaForField(Configuration config, Field field) {
		ConfigProperty annotation = field.getAnnotation(ConfigProperty.class);
		if (annotation == null) return null;
		Class<?> fieldType = field.getType();
		return fieldType.isArray()? new MultipleValues(config, field, annotation) : new SingleValue(config, field, annotation);
	}

}
