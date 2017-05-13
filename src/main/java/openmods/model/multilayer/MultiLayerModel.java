package openmods.model.multilayer;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelCustomData;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.Log;
import openmods.model.BakedModelAdapter;

// This is more or less Forge's multi-layer model, but this one changes order of quads renderes without any layer (solid first, then translucent)
public final class MultiLayerModel implements IModelCustomData {

	public static final MultiLayerModel EMPTY = new MultiLayerModel(Optional.<ResourceLocation> absent(), ImmutableMap.<BlockRenderLayer, ResourceLocation> of());

	private final Optional<ResourceLocation> base;
	private final Map<BlockRenderLayer, ResourceLocation> models;

	public MultiLayerModel(Optional<ResourceLocation> base, Map<BlockRenderLayer, ResourceLocation> models) {
		this.base = base;
		this.models = models;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return ImmutableList.copyOf(Iterables.concat(models.values(), base.asSet()));
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return ImmutableList.of();
	}

	private static IBakedModel bakeModel(ResourceLocation model, IModelState state, final VertexFormat format, final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		IModel baseModel = ModelLoaderRegistry.getModelOrLogError(model, "Couldn't load MultiLayerModel dependency: " + model);
		return baseModel.bake(new ModelStateComposition(state, baseModel.getDefaultState()), format, bakedTextureGetter);
	}

	@Override
	public IBakedModel bake(final IModelState state, final VertexFormat format, final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final Map<BlockRenderLayer, IBakedModel> bakedModels = Maps.transformValues(models, new Function<ResourceLocation, IBakedModel>() {
			@Override
			@Nullable
			public IBakedModel apply(@Nullable ResourceLocation location) {
				return bakeModel(location, state, format, bakedTextureGetter);
			}
		});

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
				IPerspectiveAwareModel.MapWrapper.getTransforms(state));
	}

	@Override
	public IModelState getDefaultState() {
		return TRSRTransformation.identity();
	}

	@Override
	public MultiLayerModel process(ImmutableMap<String, String> customData) {
		boolean changed = false;
		Optional<ResourceLocation> base = this.base;
		Map<BlockRenderLayer, ResourceLocation> models = Maps.newHashMap(this.models);

		final String baseValue = customData.get("base");
		if (baseValue != null) {
			base = Optional.<ResourceLocation> of(getLocation(baseValue));
			changed = true;
		}

		for (BlockRenderLayer layer : BlockRenderLayer.values()) {
			final String layerModel = customData.get(layer.toString());
			if (layerModel != null) {
				models.put(layer, getLocation(layerModel));
				changed = true;
			}
		}

		return changed? new MultiLayerModel(base, models) : this;

	}

	private static ResourceLocation getLocation(String json) {
		JsonElement e = new JsonParser().parse(json);
		if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) { return new ModelResourceLocation(e.getAsString()); }
		Log.severe("Expect ModelResourceLocation, got: ", json);
		return new ModelResourceLocation("builtin/missing", "missing");
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
			if (layer == null) { return side == null? quads : ImmutableList.<BakedQuad> of(); }

			final IBakedModel model = models.get(layer);
			return Objects.firstNonNull(model, missing).getQuads(state, side, rand);
		}
	}

}
