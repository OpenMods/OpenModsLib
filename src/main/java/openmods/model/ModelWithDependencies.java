package openmods.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.utils.CollectionUtils;

public class ModelWithDependencies implements IModel {
	public static final ModelWithDependencies EMPTY = new ModelWithDependencies(Optional.empty(), ImmutableSet.of(), new ModelTextureMap());

	private final Optional<ResourceLocation> base;

	private final Set<ResourceLocation> dependencies;

	private final ModelTextureMap textures;

	private ModelWithDependencies(Optional<ResourceLocation> base, Set<ResourceLocation> dependencies, ModelTextureMap textures) {
		this.base = base;
		this.dependencies = ImmutableSet.copyOf(dependencies);
		this.textures = textures;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Sets.union(dependencies, CollectionUtils.asSet(base));
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return textures.getTextures();
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
		final ModelUpdater updater = new ModelUpdater(customData);

		final Optional<ResourceLocation> base = updater.get("base", ModelUpdater.MODEL_LOCATION, this.base);
		final Set<ResourceLocation> dependencies = updater.get("dependencies", ModelUpdater.MODEL_LOCATION, this.dependencies);

		return updater.hasChanged()? new ModelWithDependencies(base, dependencies, this.textures) : this;
	}

	@Override
	public IModel retexture(ImmutableMap<String, String> updates) {
		final Optional<ModelTextureMap> newTextures = textures.update(updates);
		return newTextures.isPresent()? new ModelWithDependencies(base, dependencies, newTextures.get()) : this;
	}

}