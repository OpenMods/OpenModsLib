package openmods.model.variant;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

public class VariantSelectorData {

	private interface Matcher {
		Set<ResourceLocation> match(String value);

		Set<ResourceLocation> getAllModels();
	}

	private Map<String, Matcher> matchers = ImmutableMap.of();

	private Set<ResourceLocation> allModels = ImmutableSet.of();

	public Set<ResourceLocation> getAllModels() {
		return allModels;
	}

	public Set<ResourceLocation> getModels(Map<String, String> key) {
		final Set<ResourceLocation> result = Sets.newHashSet();

		for (Map.Entry<String, String> e : key.entrySet()) {
			Matcher m = matchers.get(e.getKey());
			if (m != null) {
				final Set<ResourceLocation> match = m.match(e.getValue());
				result.addAll(match);
			}
		}

		return ImmutableSet.copyOf(result);
	}

	public static VariantSelectorData parse(String flatJson) {
		return GSON.fromJson(flatJson, VariantSelectorData.class);
	}

	public static VariantSelectorData parse(JsonElement json) {
		return GSON.fromJson(json, VariantSelectorData.class);
	}

	private static final Gson GSON = new GsonBuilder().registerTypeAdapter(VariantSelectorData.class, new Deserializer()).create();

	private static class Deserializer implements JsonDeserializer<VariantSelectorData> {

		@Override
		public VariantSelectorData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			final JsonObject jsonObject = json.getAsJsonObject();
			final Map<String, Matcher> matchers = Maps.newHashMap();
			final Set<ResourceLocation> allModels = Sets.newHashSet();

			for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
				final String name = e.getKey();
				final JsonElement value = e.getValue();

				final Matcher matcher;
				if (value.isJsonObject()) {
					matcher = createKeyedMatcher(name, e.getValue().getAsJsonObject());
				} else {
					matcher = createUnconditionalMatcher(name, e.getValue());
				}

				allModels.addAll(matcher.getAllModels());
				matchers.put(name, matcher);
			}

			final VariantSelectorData result = new VariantSelectorData();
			result.allModels = ImmutableSet.copyOf(allModels);
			result.matchers = ImmutableMap.copyOf(matchers);
			return result;
		}

		private static Matcher createKeyedMatcher(String name, JsonObject value) {
			final ImmutableMap.Builder<String, Set<ResourceLocation>> locsBuilder = ImmutableMap.builder();
			final ImmutableSet.Builder<ResourceLocation> allModelsBuilder = ImmutableSet.builder();

			Optional<Set<ResourceLocation>> maybeDefaultModels = Optional.absent();

			for (Map.Entry<String, JsonElement> e : value.entrySet()) {
				final String entryName = e.getKey();
				final Set<ResourceLocation> models = parseModels(entryName, e.getValue());

				if (entryName.equals(VariantModelState.DEFAULT_MARKER)) {
					maybeDefaultModels = Optional.of(models);
				} else {
					locsBuilder.put(entryName, models);
				}

				allModelsBuilder.addAll(models);
			}

			final Set<ResourceLocation> allModels = allModelsBuilder.build();
			final Map<String, Set<ResourceLocation>> locs = locsBuilder.build();

			if (maybeDefaultModels.isPresent()) {
				final Set<ResourceLocation> defaultModels = ImmutableSet.copyOf(maybeDefaultModels.get());
				return new Matcher() {
					@Override
					public Set<ResourceLocation> match(String value) {
						final Set<ResourceLocation> result = locs.get(value);
						return result != null? ImmutableSet.copyOf(result) : defaultModels;
					}

					@Override
					public Set<ResourceLocation> getAllModels() {
						return allModels;
					}
				};
			} else {
				return new Matcher() {

					@Override
					public Set<ResourceLocation> match(String value) {
						final Set<ResourceLocation> result = locs.get(value);
						return result != null? ImmutableSet.copyOf(result) : ImmutableSet.of();
					}

					@Override
					public Set<ResourceLocation> getAllModels() {
						return allModels;
					}
				};
			}
		}

		private static Matcher createUnconditionalMatcher(String name, JsonElement value) {
			final Set<ResourceLocation> models = ImmutableSet.copyOf(parseModels(name, value));
			return new Matcher() {
				@Override
				public Set<ResourceLocation> match(String value) {
					return models;
				}

				@Override
				public Set<ResourceLocation> getAllModels() {
					return models;
				}
			};
		}

		private static Set<ResourceLocation> parseModels(String name, JsonElement value) {
			if (value.isJsonArray()) {
				return parseArrayOfLocations(name, value);
			} else if (value.isJsonPrimitive()) { return parseSingleLocation(value); }

			throw new JsonSyntaxException("Expected " + name + " to be a string, was " + JSONUtils.toString(value));
		}

		private static Set<ResourceLocation> parseSingleLocation(JsonElement value) {
			final ResourceLocation loc = new ModelResourceLocation(value.getAsString());
			return ImmutableSet.of(loc);
		}

		private static Set<ResourceLocation> parseArrayOfLocations(String name, JsonElement value) {
			final ImmutableSet.Builder<ResourceLocation> result = ImmutableSet.builder();

			for (JsonElement e : value.getAsJsonArray()) {
				if (e.isJsonPrimitive()) {
					final ResourceLocation loc = new ModelResourceLocation(value.getAsString());
					result.add(loc);
				} else {
					throw new JsonSyntaxException("Expected elements of " + name + " to be a string, was " + JSONUtils.toString(e));
				}
			}

			return result.build();
		}

	}
}
