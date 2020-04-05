package openmods.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import openmods.utils.CollectionUtils;

public class ModelWithDependencies implements IUnbakedModel {
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
	public Collection<ResourceLocation> getTextures(Function<ResourceLocation, IUnbakedModel> modelGetter, Set<String> missingTextureErrors) {
		return textures.getTextures();
	}

	@Nullable @Override public IBakedModel bake(ModelBakery bakery, Function<ResourceLocation, TextureAtlasSprite> spriteGetter, ISprite sprite, VertexFormat format) {
		final IModel model;
		if (base.isPresent()) {
			model = ModelLoaderRegistry.getModelOrLogError(base.get(), "Couldn't load MultiLayerModel dependency: " + base.get());
		} else {
			model = ModelLoaderRegistry.getMissingModel();
		}

		return model.bake(bakery, spriteGetter, sprite, format);
	}

	@Override
	public IUnbakedModel process(ImmutableMap<String, String> customData) {
		final ModelUpdater updater = new ModelUpdater(customData);

		final Optional<ResourceLocation> base = updater.get("base", ModelUpdater.MODEL_LOCATION, this.base);
		final Set<ResourceLocation> dependencies = updater.get("dependencies", ModelUpdater.MODEL_LOCATION, this.dependencies);

		return updater.hasChanged()? new ModelWithDependencies(base, dependencies, this.textures) : this;
	}

	@Override
	public IUnbakedModel retexture(ImmutableMap<String, String> updates) {
		final Optional<ModelTextureMap> newTextures = textures.update(updates);
		return newTextures.isPresent()? new ModelWithDependencies(base, dependencies, newTextures.get()) : this;
	}

}