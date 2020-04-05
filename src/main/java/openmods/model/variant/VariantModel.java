package openmods.model.variant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BasicState;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.model.BakedModelAdapter;
import openmods.model.ModelUpdater;
import openmods.utils.CollectionUtils;

public class VariantModel implements IUnbakedModel {

	private static class BakedModel extends BakedModelAdapter {

		private final VariantModelData modelData;

		private final Map<ResourceLocation, IBakedModel> bakedSubModels;

		public BakedModel(IBakedModel base, VariantModelData modelData, Map<ResourceLocation, IBakedModel> bakedSubModels, ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> cameraTransforms) {
			super(base, cameraTransforms);
			this.modelData = modelData;
			this.bakedSubModels = bakedSubModels;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData extState) {
			final VariantModelState modelState = getModelSelectors(extState);

			final List<BakedQuad> result = Lists.newArrayList(base.getQuads(state, side, rand, extState));

			for (ResourceLocation subModel : modelData.getModels(modelState.getSelectors())) {
				final IBakedModel bakedSubModel = bakedSubModels.get(subModel);
				result.addAll(bakedSubModel.getQuads(state, side, rand, extState));
			}

			return result;
		}

		private static VariantModelState getModelSelectors(IModelData state) {
			if (state != null) {
				final VariantModelState data = state.getData(VariantModelState.PROPERTY);
				if (data != null)
					return data;
			}

			return VariantModelState.EMPTY;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.EMPTY;
		}
	}

	private static final String KEY_VARIANTS = "variants";

	private static final String KEY_EXPANSIONS = "expansions";

	private static final String KEY_BASE = "base";

	public static final VariantModel EMPTY_MODEL = new VariantModel(Optional.empty(), new VariantModelData());

	private final Optional<ResourceLocation> base;

	private final VariantModelData modelData;

	public VariantModel(Optional<ResourceLocation> base, VariantModelData modelData) {
		this.base = base;
		this.modelData = modelData;
	}

	@Override
	public IBakedModel bake(ModelBakery bakery, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, ISprite state, VertexFormat format) {
		final Map<ResourceLocation, IBakedModel> bakedSubModels = Maps.newHashMap();

		for (ResourceLocation subModel : modelData.getAllModels()) {
			IModel model = ModelLoaderRegistry.getModelOrLogError(subModel, "Couldn't load sub-model dependency: " + subModel);
			bakedSubModels.put(subModel, model.bake(bakery, bakedTextureGetter, new BasicState(new ModelStateComposition(state.getState(), model.getDefaultState()), state.isUvLock()), format));
		}

		final IModel baseModel;
		if (base.isPresent()) {
			ResourceLocation baseLocation = base.get();
			baseModel = ModelLoaderRegistry.getModelOrLogError(baseLocation, "Couldn't load base-model dependency: " + baseLocation);
		} else {
			baseModel = ModelLoaderRegistry.getMissingModel();
		}

		final IBakedModel bakedBaseModel = baseModel.bake(bakery, bakedTextureGetter, new BasicState(new ModelStateComposition(state.getState(), baseModel.getDefaultState()), state.isUvLock()), format);

		return new BakedModel(bakedBaseModel, modelData, bakedSubModels, PerspectiveMapWrapper.getTransforms(state.getState()));
	}

	@Override
	public IUnbakedModel process(ImmutableMap<String, String> customData) {
		final ModelUpdater updater = new ModelUpdater(customData);

		final Optional<ResourceLocation> base = updater.get(KEY_BASE, ModelUpdater.MODEL_LOCATION, this.base);

		VariantModelData modelData = this.modelData;
		if (customData.containsKey(KEY_VARIANTS) || customData.containsKey(KEY_EXPANSIONS)) {
			updater.markChanged();
			Optional<String> variants = Optional.ofNullable(customData.get(KEY_VARIANTS));
			Optional<String> expansions = Optional.ofNullable(customData.get(KEY_EXPANSIONS));
			modelData = modelData.update(variants, expansions);
		}

		return updater.hasChanged()? new VariantModel(base, modelData) : this;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return ImmutableList.copyOf(Sets.union(modelData.getAllModels(), CollectionUtils.asSet(base)));
	}

	@Override public Collection<ResourceLocation> getTextures(Function<ResourceLocation, IUnbakedModel> modelGetter, Set<String> missingTextureErrors) {
		return Collections.emptyList();
	}
}
