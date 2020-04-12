package openmods.model.textureditem;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BasicState;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.model.BakedModelAdapter;
import openmods.model.ModelUpdater;
import openmods.utils.CollectionUtils;

public class TexturedItemModel implements IUnbakedModel {

	private static class BakedModel extends BakedModelAdapter {

		private final ItemOverrideList overrideList;

		public BakedModel(IBakedModel base, ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> cameraTransforms, ItemOverrideList itemOverrideList) {
			super(base, cameraTransforms);
			this.overrideList = itemOverrideList;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return overrideList;
		}
	}

	public static final TexturedItemModel INSTANCE = new TexturedItemModel();

	private final Optional<ResourceLocation> untexturedModel;
	private final Optional<ResourceLocation> texturedModel;
	private final Set<String> textures;

	private TexturedItemModel() {
		untexturedModel = Optional.empty();
		texturedModel = Optional.empty();
		textures = ImmutableSet.of();
	}

	private TexturedItemModel(Optional<ResourceLocation> empty, Optional<ResourceLocation> fluid, Set<String> textures) {
		this.untexturedModel = empty;
		this.texturedModel = fluid;
		this.textures = ImmutableSet.copyOf(textures);
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Sets.union(CollectionUtils.asSet(untexturedModel), CollectionUtils.asSet(texturedModel));
	}

	@Override public Collection<ResourceLocation> getTextures(Function<ResourceLocation, IUnbakedModel> modelGetter, Set<String> missingTextureErrors) {
		return Collections.emptyList();
	}

	@Override
	public IBakedModel bake(ModelBakery bakery, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, ISprite state, VertexFormat format) {
		final IModel untexturedModel = getModel(this.untexturedModel);
		final IBakedModel untexturedBakedModel = untexturedModel.bake(bakery, bakedTextureGetter, new BasicState(new ModelStateComposition(state.getState(), untexturedModel.getDefaultState()), state.isUvLock()), format);

		final IUnbakedModel texturedModel = getModel(this.texturedModel);
		final IBakedModel texturedBakedModel = texturedModel.bake(bakery, bakedTextureGetter, new BasicState(new ModelStateComposition(state.getState(), texturedModel.getDefaultState()), state.isUvLock()), format);

		final ItemOverrideList overrides = new TexturedItemOverrides(untexturedBakedModel, texturedModel, bakery, texturedBakedModel.getOverrides().getOverrides(), textures, state, format, bakedTextureGetter);

		return new BakedModel(untexturedBakedModel, PerspectiveMapWrapper.getTransforms(state.getState()), overrides);
	}

	private static IUnbakedModel getModel(Optional<ResourceLocation> model) {
		if (model.isPresent()) {
			ResourceLocation location = model.get();
			return ModelLoaderRegistry.getModelOrLogError(location, "Couldn't load base-model dependency: " + location);
		} else {
			return ModelLoaderRegistry.getMissingModel();
		}
	}

	@Override
	public IUnbakedModel process(ImmutableMap<String, String> customData) {
		final ModelUpdater updater = new ModelUpdater(customData);

		final Optional<ResourceLocation> untexturedModel = updater.get("untexturedModel", ModelUpdater.MODEL_LOCATION, this.untexturedModel);
		final Optional<ResourceLocation> filled = updater.get("texturedModel", ModelUpdater.MODEL_LOCATION, this.texturedModel);
		final Set<String> textures = updater.get("textures", ModelUpdater.TO_STRING, this.textures);

		return updater.hasChanged()? new TexturedItemModel(untexturedModel, filled, textures) : this;
	}

}
