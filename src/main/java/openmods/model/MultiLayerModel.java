package openmods.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.utils.CollectionUtils;

// This is more or less Forge's multi-layer model, but this one changes order of quads renderes without any layer (solid first, then translucent)
public final class MultiLayerModel implements IModel {

	public static final MultiLayerModel EMPTY = new MultiLayerModel(Optional.empty(), ImmutableMap.of());

	private final Optional<ResourceLocation> base;
	private final Map<BlockRenderLayer, ResourceLocation> models;

	public MultiLayerModel(Optional<ResourceLocation> base, Map<BlockRenderLayer, ResourceLocation> models) {
		this.base = base;
		this.models = ImmutableMap.copyOf(models);
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return ImmutableList.copyOf(Iterables.concat(models.values(), CollectionUtils.asSet(base)));
	}

	private static IBakedModel bakeModel(ResourceLocation model, IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		IModel baseModel = ModelLoaderRegistry.getModelOrLogError(model, "Couldn't load MultiLayerModel dependency: " + model);
		return baseModel.bake(new ModelStateComposition(state, baseModel.getDefaultState()), format, bakedTextureGetter);
	}

	@Override
	public IBakedModel bake(final IModelState state, final VertexFormat format, final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final Map<BlockRenderLayer, IBakedModel> bakedModels = Maps.transformValues(models, location -> bakeModel(location, state, format, bakedTextureGetter));

		IModel missing = ModelLoaderRegistry.getMissingModel();
		IBakedModel bakedMissing = missing.bake(missing.getDefaultState(), format, bakedTextureGetter);

		final IBakedModel bakedBase;
		if (base.isPresent()) {
			bakedBase = bakeModel(base.get(), state, format, bakedTextureGetter);
		} else {
			bakedBase = bakedMissing;
		}

		return new MultiLayerBakedModel(
				bakedModels,
				bakedBase,
				bakedMissing,
				PerspectiveMapWrapper.getTransforms(state));
	}

	@Override
	public MultiLayerModel process(ImmutableMap<String, String> customData) {
		final ModelUpdater updater = new ModelUpdater(customData);

		final Optional<ResourceLocation> base = updater.get("base", ModelUpdater.MODEL_LOCATION, this.base);

		final Map<BlockRenderLayer, ResourceLocation> models = Maps.newHashMap();
		for (BlockRenderLayer layer : BlockRenderLayer.values()) {
			final ResourceLocation result = updater.get(layer.toString(), ModelUpdater.MODEL_LOCATION, this.models.get(layer));
			if (result != null) models.put(layer, result);
		}

		return updater.hasChanged()? new MultiLayerModel(base, models) : this;

	}

	private static final class MultiLayerBakedModel extends BakedModelAdapter {
		private final Map<BlockRenderLayer, IBakedModel> models;
		private final IBakedModel missing;
		private final List<BakedQuad> quads;

		public MultiLayerBakedModel(Map<BlockRenderLayer, IBakedModel> models, IBakedModel base, IBakedModel missing, ImmutableMap<TransformType, TRSRTransformation> cameraTransforms) {
			super(base, cameraTransforms);
			this.models = ImmutableMap.copyOf(models);
			this.missing = missing;

			final List<BakedQuad> quads = Lists.newArrayList();

			for (BlockRenderLayer layer : BlockRenderLayer.values()) {
				final IBakedModel model = models.get(layer);
				if (model != null) {
					buildQuadsForLayer(quads, model);
				}
			}

			this.quads = ImmutableList.copyOf(quads);
		}

		private static void buildQuadsForLayer(List<BakedQuad> quads, IBakedModel model) {
			quads.addAll(model.getQuads(null, null, 0));

			for (EnumFacing side : EnumFacing.VALUES)
				quads.addAll(model.getQuads(null, side, 0));
		}

		@Nonnull
		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			final BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
			if (layer == null) { return side == null? quads : ImmutableList.of(); }

			final IBakedModel model = models.get(layer);
			return MoreObjects.firstNonNull(model, missing).getQuads(state, side, rand);
		}
	}

}
