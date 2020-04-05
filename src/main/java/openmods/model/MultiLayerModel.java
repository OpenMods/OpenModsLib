package openmods.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.BasicState;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.utils.CollectionUtils;

// This is more or less Forge's multi-layer model, but this one changes order of quads renderes without any layer (solid first, then translucent)
public final class MultiLayerModel implements IUnbakedModel {

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

	@Override public Collection<ResourceLocation> getTextures(Function<ResourceLocation, IUnbakedModel> modelGetter, Set<String> missingTextureErrors) {
		// TODO 1.14 Am I responsible for returning deps?
		return Collections.emptyList();
	}

	private static IBakedModel bakeModel(ModelBakery bakery, ResourceLocation model, ISprite state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		IModel baseModel = ModelLoaderRegistry.getModelOrLogError(model, "Couldn't load MultiLayerModel dependency: " + model);
		return baseModel.bake(bakery, bakedTextureGetter, new BasicState(new ModelStateComposition(state.getState(), baseModel.getDefaultState()), state.isUvLock()), format);
	}

	@Nullable
	@Override public IBakedModel bake(ModelBakery bakery, Function<ResourceLocation, TextureAtlasSprite> spriteGetter, ISprite sprite, VertexFormat format) {
		final Map<BlockRenderLayer, IBakedModel> bakedModels = Maps.transformValues(models, location -> bakeModel(bakery, location, sprite, format, spriteGetter));

		IModel missing = ModelLoaderRegistry.getMissingModel();
		IBakedModel bakedMissing = missing.bake(bakery, spriteGetter, new BasicState(missing.getDefaultState(), sprite.isUvLock()), format);

		final IBakedModel bakedBase;
		if (base.isPresent()) {
			bakedBase = bakeModel(bakery, base.get(), sprite, format, spriteGetter);
		} else {
			bakedBase = bakedMissing;
		}

		return new MultiLayerBakedModel(
				bakedModels,
				bakedBase,
				bakedMissing,
				PerspectiveMapWrapper.getTransforms(sprite.getState()));
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

		public MultiLayerBakedModel(Map<BlockRenderLayer, IBakedModel> models, IBakedModel base, IBakedModel missing, ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> cameraTransforms) {
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
			final Random random = new Random(0);
			quads.addAll(model.getQuads(null, null, random, EmptyModelData.INSTANCE));

			for (Direction side : Direction.values())
				quads.addAll(model.getQuads(null, side, random, EmptyModelData.INSTANCE));
		}

		@Nonnull
		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, Random random, IModelData data) {
			final BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
			if (layer == null) { return side == null? quads : ImmutableList.of(); }

			final IBakedModel model = models.get(layer);
			return MoreObjects.firstNonNull(model, missing).getQuads(state, side, random, data);
		}
	}

}
