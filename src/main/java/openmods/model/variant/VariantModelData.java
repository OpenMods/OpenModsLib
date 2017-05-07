package openmods.model.variant;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.ResourceLocation;

class VariantModelData {
	private final VariantSelectorData variants;

	private final Evaluator expansions;

	private final LoadingCache<Map<String, String>, Set<ResourceLocation>> cache;

	public VariantModelData(final VariantSelectorData variants, final Evaluator expansions) {
		this.variants = variants;
		this.expansions = expansions;

		this.cache = CacheBuilder.newBuilder()
				.expireAfterAccess(5, TimeUnit.MINUTES)
				.build(
						new CacheLoader<Map<String, String>, Set<ResourceLocation>>() {
							@Override
							public Set<ResourceLocation> load(Map<String, String> key) throws Exception {
								final Map<String, String> mutableCopy = Maps.newHashMap(key);
								expansions.expandVars(mutableCopy);
								return variants.getModels(mutableCopy);
							}
						});
	}

	public VariantModelData() {
		this(new VariantSelectorData(), new Evaluator());
	}

	public Set<ResourceLocation> getModels(Map<String, String> key) {
		return cache.getUnchecked(ImmutableMap.copyOf(key));
	}

	public Set<ResourceLocation> getAllModels() {
		return variants.getAllModels();
	}

	private static Evaluator parseExpansions(String json) {
		final JsonElement parsedJson = new JsonParser().parse(json);

		final Evaluator result = new Evaluator();

		for (JsonElement statement : parsedJson.getAsJsonArray())
			result.addStatement(statement.getAsString());

		return result;
	}

	public VariantModelData update(Optional<String> serializedVariants, Optional<String> serializedExpansions) {
		boolean changed = false;
		VariantSelectorData newVariants = this.variants;
		Evaluator newExpansions = this.expansions;

		if (serializedVariants.isPresent()) {
			changed = true;
			newVariants = VariantSelectorData.parse(serializedVariants.get());
		}

		if (serializedExpansions.isPresent()) {
			changed = true;
			newExpansions = parseExpansions(serializedExpansions.get());
		}

		return changed? new VariantModelData(newVariants, newExpansions) : this;
	}

}