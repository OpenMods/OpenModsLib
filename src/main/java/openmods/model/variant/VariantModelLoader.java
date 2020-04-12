package openmods.model.variant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraftforge.client.model.IModelLoader;

public class VariantModelLoader implements IModelLoader<VariantModelGeometry> {
	private static final String KEY_VARIANTS = "variants";
	private static final String KEY_EXPANSIONS = "expansions";
	private static final String KEY_BASE = "base";

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
	}

	@Override
	public VariantModelGeometry read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
		final IUnbakedModel base = deserializationContext.deserialize(JSONUtils.getJsonObject(modelContents, KEY_BASE), BlockModel.class);

		final Evaluator evaluator = new Evaluator();
		if (modelContents.has(KEY_EXPANSIONS)) {
			JsonArray expansions = JSONUtils.getJsonArray(modelContents, KEY_EXPANSIONS);
			for (JsonElement statement : expansions) {
				evaluator.addStatement(statement.getAsString());
			}
		}

		final List<Pair<Predicate<VariantModelState>, IUnbakedModel>> parts = Lists.newArrayList();

		if (modelContents.has(KEY_VARIANTS)) {
			JsonObject partData = JSONUtils.getJsonObject(modelContents, KEY_VARIANTS);
			for (Map.Entry<String, JsonElement> p : partData.entrySet()) {
				final Predicate<VariantModelState> predicate = parsePredicate(p.getKey());
				final IUnbakedModel partModel = deserializationContext.deserialize(p.getValue(), BlockModel.class);
				parts.add(Pair.of(predicate, partModel));
			}
		}

		return new VariantModelGeometry(evaluator, base, ImmutableList.copyOf(parts));
	}

	private static Predicate<VariantModelState> parsePredicate(String key) {
		int separator = key.indexOf('.');
		if (separator != -1) {
			final String k = key.substring(0, separator);
			final String v = key.substring(separator + 1);
			return state -> state.testKeyValue(k, v);
		} else {
			return state -> state.testKey(key);
		}
	}
}
