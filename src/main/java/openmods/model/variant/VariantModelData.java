package openmods.model.variant;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.ResourceLocation;

class VariantModelData {
	private final VariantSelectorData variants;

	// TODO expand for calculated values

	public VariantModelData(VariantSelectorData variants) {
		this.variants = variants;
	}

	private final LoadingCache<Map<String, String>, Set<ResourceLocation>> cache = CacheBuilder.newBuilder().maximumSize(256).build(
			new CacheLoader<Map<String, String>, Set<ResourceLocation>>() {
				@Override
				public Set<ResourceLocation> load(Map<String, String> key) throws Exception {
					return variants.getModels(key);
				}
			});

	public Set<ResourceLocation> getModels(Map<String, String> key) {
		return cache.getUnchecked(ImmutableMap.copyOf(key));
	}

	public Set<ResourceLocation> getAllModels() {
		return variants.getAllModels();
	}

	public static VariantModelData load(String string) {
		final VariantSelectorData variants = VariantSelectorData.parse(string);
		return new VariantModelData(variants);
	}

}