package openmods.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

public class ModelUpdater {

	@FunctionalInterface
	public interface ValueConverter<T> {
		T convert(String name, JsonElement element);
	}

	public static final ValueConverter<ResourceLocation> RESOURCE_LOCATION = (name, element) -> {
		final String value = JSONUtils.getString(element, name);
		return new ResourceLocation(value);
	};

	public static final ValueConverter<ModelResourceLocation> MODEL_RESOURCE_LOCATION = (name, element) -> {
		final String value = JSONUtils.getString(element, name);
		return new ModelResourceLocation(value);
	};

	public static final ValueConverter<ResourceLocation> MODEL_LOCATION = (name, element) -> {
		final String value = JSONUtils.getString(element, name);
		return value.contains("#")? new ModelResourceLocation(value) : new ResourceLocation(value);
	};

	public static final ValueConverter<String> TO_STRING = (name, element) -> JSONUtils.getString(element, name);

	public static final ValueConverter<Integer> TO_INT = (name, element) -> JSONUtils.getInt(element, name);

	public static final ValueConverter<Long> TO_LONG = (name, element) -> {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
			return element.getAsLong();

		throw new JsonSyntaxException("Expected " + name + " to be a Int, was " + JSONUtils.toString(element));
	};

	public static <T extends Enum<T>> ValueConverter<T> enumConverter(Class<T> enumCls) {
		final ImmutableMap.Builder<String, T> valuesBuilder = ImmutableMap.builder();
		for (T c : enumCls.getEnumConstants())
			valuesBuilder.put(c.name().toLowerCase(Locale.ROOT), c);

		final ImmutableMap<String, T> values = valuesBuilder.build();

		return (name, element) -> {
			final String enumName = JSONUtils.getString(element, name);
			return values.get(enumName.toLowerCase(Locale.ROOT));
		};
	}

	private final Map<String, String> values;

	private final JsonParser parser = new JsonParser();

	private boolean changed;

	public ModelUpdater(Map<String, String> values) {
		this.values = values;
	}

	public boolean hasChanged() {
		return changed;
	}

	public void markChanged() {
		this.changed = true;
	}

	public <T> T get(String key, ValueConverter<T> converter, T current) {
		final String value = values.get(key);
		if (value != null) {
			final JsonElement element = parser.parse(value);

			final T result = converter.convert(key, element);
			if (!(result.equals(current))) {
				changed = true;
				return result;
			}
		}

		return current;
	}

	public <T> Optional<T> get(String key, final ValueConverter<T> converter, Optional<T> current) {
		return get(key, (String name, JsonElement element) -> Optional.of(converter.convert(name, element)), current);
	}

	public <T> Set<T> get(String key, ValueConverter<T> converter, Set<T> current) {
		final String value = values.get(key);
		if (value != null) {
			final JsonElement parsedValue = parser.parse(value);

			final ImmutableSet.Builder<T> resultBuilder = ImmutableSet.builder();
			if (parsedValue.isJsonArray()) {
				for (JsonElement e : parsedValue.getAsJsonArray())
					resultBuilder.add(converter.convert(key, e));
			} else {
				resultBuilder.add(converter.convert(key, parsedValue));
			}

			final Set<T> result = resultBuilder.build();
			if (!result.equals(current)) {
				changed = true;
				return result;
			}
		}

		return current;
	}
}
