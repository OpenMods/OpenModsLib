package openmods.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.vecmath.Matrix4f;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.utils.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

public final class PerspectiveAwareModel implements IModel {

	public static final PerspectiveAwareModel EMPTY = new PerspectiveAwareModel(Optional.empty(), ImmutableMap.of());

	private final Optional<ResourceLocation> base;
	private final Map<TransformType, ResourceLocation> models;

	public PerspectiveAwareModel(Optional<ResourceLocation> base, Map<TransformType, ResourceLocation> models) {
		this.base = base;
		this.models = ImmutableMap.copyOf(models);
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return ImmutableList.copyOf(Iterables.concat(models.values(), CollectionUtils.asSet(base)));
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return ImmutableList.of();
	}

	private static IBakedModel bakeModel(ResourceLocation model, IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		IModel baseModel = ModelLoaderRegistry.getModelOrLogError(model, "Couldn't load MultiLayerModel dependency: " + model);
		return baseModel.bake(new ModelStateComposition(state, baseModel.getDefaultState()), format, bakedTextureGetter);
	}

	@Override
	public IBakedModel bake(final IModelState state, final VertexFormat format, final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final Map<TransformType, IBakedModel> bakedModels = Maps.transformValues(models, (location) -> bakeModel(location, state, format, bakedTextureGetter));

		IModel missing = ModelLoaderRegistry.getMissingModel();
		IBakedModel bakedMissing = missing.bake(missing.getDefaultState(), format, bakedTextureGetter);

		final IBakedModel bakedBase;
		if (base.isPresent()) {
			bakedBase = bakeModel(base.get(), state, format, bakedTextureGetter);
		} else {
			bakedBase = bakedMissing;
		}

		return new PerspectiveAwareBakedModel(
				bakedModels,
				bakedBase,
				PerspectiveMapWrapper.getTransforms(state));
	}

	@Override
	public IModelState getDefaultState() {
		return TRSRTransformation.identity();
	}

	@Override
	public PerspectiveAwareModel process(ImmutableMap<String, String> customData) {
		final ModelUpdater updater = new ModelUpdater(customData);

		final Optional<ResourceLocation> base = updater.get("base", ModelUpdater.MODEL_LOCATION, this.base);

		final Map<TransformType, ResourceLocation> models = Maps.newHashMap();
		for (TransformType layer : TransformType.values()) {
			final ResourceLocation result = updater.get(layer.toString(), ModelUpdater.MODEL_LOCATION, this.models.get(layer));
			if (result != null) models.put(layer, result);
		}

		return updater.hasChanged()? new PerspectiveAwareModel(base, models) : this;

	}

	private static final class PerspectiveAwareBakedModel extends BakedModelAdapter {
		private final Map<TransformType, IBakedModel> models;

		public PerspectiveAwareBakedModel(Map<TransformType, IBakedModel> models, IBakedModel base, ImmutableMap<TransformType, TRSRTransformation> cameraTransforms) {
			super(base, cameraTransforms);
			this.models = ImmutableMap.copyOf(models);
		}

		@Override
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
			IBakedModel model = models.get(cameraTransformType);
			if (model == null) model = base;

			return model.handlePerspective(cameraTransformType);
		}
	}

}
