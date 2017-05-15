package openmods.model;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Collection;
import java.util.Set;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelCustomData;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.OpenMods;

public class DependencyModelLoader implements ICustomModelLoader {

	private static class ModelWithDependencies implements IModelCustomData {
		private static final ModelWithDependencies EMPTY = new ModelWithDependencies(Optional.<ResourceLocation> absent(), ImmutableSet.<ResourceLocation> of());

		private final Optional<ResourceLocation> base;

		private final Set<ResourceLocation> dependencies;

		public ModelWithDependencies(Optional<ResourceLocation> base, Set<ResourceLocation> dependencies) {
			this.base = base;
			this.dependencies = ImmutableSet.copyOf(dependencies);
		}

		@Override
		public Collection<ResourceLocation> getDependencies() {
			return Sets.union(dependencies, base.asSet());
		}

		@Override
		public Collection<ResourceLocation> getTextures() {
			return ImmutableList.of();
		}

		@Override
		public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
			final IModel model;
			if (base.isPresent()) {
				model = ModelLoaderRegistry.getModelOrLogError(base.get(), "Couldn't load MultiLayerModel dependency: " + base.get());
			} else {
				model = ModelLoaderRegistry.getMissingModel();
			}

			return model.bake(state, format, bakedTextureGetter);
		}

		@Override
		public IModelState getDefaultState() {
			return TRSRTransformation.identity();
		}

		@Override
		public IModel process(ImmutableMap<String, String> customData) {
			boolean changed = false;
			Optional<ResourceLocation> base = this.base;
			Set<ResourceLocation> dependencies = this.dependencies;

			final String baseJson = customData.get("base");
			if (baseJson != null) {
				final JsonElement parsedBase = new JsonParser().parse(baseJson);
				base = Optional.of(getLocation(parsedBase));
				changed = true;
			}

			final String dependenciesJson = customData.get("dependencies");
			if (dependenciesJson != null) {

				final JsonElement parsedDependenies = new JsonParser().parse(dependenciesJson);

				final ImmutableSet.Builder<ResourceLocation> newDependencies = ImmutableSet.builder();
				if (parsedDependenies.isJsonArray()) {
					for (JsonElement e : parsedDependenies.getAsJsonArray())
						newDependencies.add(getLocation(e));
				} else if (parsedDependenies.isJsonPrimitive()) {
					newDependencies.add(getLocation(parsedDependenies));
				} else {
					throw new IllegalArgumentException("Invalid dependencies specification:  " + dependenciesJson);
				}

				dependencies = newDependencies.build();
				changed = true;
			}

			return changed? new ModelWithDependencies(base, dependencies) : this;
		}

		private static ResourceLocation getLocation(JsonElement e) {
			if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) return new ModelResourceLocation(e.getAsString());
			throw new IllegalArgumentException("Not model location: " + e);
		}
	}

	private static final Set<String> models = ImmutableSet.of(
			"with-dependencies",
			"models/block/with-dependencies",
			"models/item/with-dependencies");

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		return modelLocation.getResourceDomain().equals(OpenMods.MODID)
				&& models.contains(modelLocation.getResourcePath());
	}

	@Override
	public IModel loadModel(ResourceLocation modelLocation) {
		return ModelWithDependencies.EMPTY;
	}
}