package openmods.model.variant;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelCustomData;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.commons.lang3.tuple.Pair;

public class VariantModel implements IModelCustomData {

	private static class BakedModel implements IPerspectiveAwareModel {

		private final IBakedModel base;

		private final VariantModelData modelData;

		private final Map<ResourceLocation, IBakedModel> bakedSubModels;

		private final ImmutableMap<TransformType, TRSRTransformation> cameraTransforms;

		public BakedModel(IBakedModel base, VariantModelData modelData, Map<ResourceLocation, IBakedModel> bakedSubModels, ImmutableMap<TransformType, TRSRTransformation> cameraTransforms) {
			this.base = base;
			this.modelData = modelData;
			this.bakedSubModels = bakedSubModels;
			this.cameraTransforms = cameraTransforms;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
			final VariantModelState modelState = getModelSelectors(state);

			final List<BakedQuad> result = Lists.newArrayList(base.getQuads(state, side, rand));

			for (ResourceLocation subModel : modelData.getModels(modelState.getSelectors())) {
				final IBakedModel bakedSubModel = bakedSubModels.get(subModel);
				result.addAll(bakedSubModel.getQuads(state, side, rand));
			}

			return result;
		}

		private static VariantModelState getModelSelectors(IBlockState state) {
			if (state instanceof IExtendedBlockState) {
				final IExtendedBlockState extendedState = (IExtendedBlockState)state;
				if (extendedState.getUnlistedNames().contains(VariantModelState.PROPERTY)) {
					final VariantModelState result = extendedState.getValue(VariantModelState.PROPERTY);
					if (result != null)
						return result;
				}
			}

			return VariantModelState.EMPTY;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return base.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return base.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return base.isBuiltInRenderer();
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return base.getParticleTexture();
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return ItemCameraTransforms.DEFAULT;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.NONE;
		}

		@Override
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
			return IPerspectiveAwareModel.MapWrapper.handlePerspective(this, cameraTransforms, cameraTransformType);
		}

	}

	private static final String KEY_VARIANTS = "variants";

	private static final String KEY_BASE = "base";

	static final VariantModel INSTANCE = new VariantModel(Optional.<ResourceLocation> absent(), new VariantModelData(new VariantSelectorData()));

	private final Optional<ResourceLocation> base;

	private final VariantModelData modelData;

	public VariantModel(Optional<ResourceLocation> base, VariantModelData modelData) {
		this.base = base;
		this.modelData = modelData;
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final Map<ResourceLocation, IBakedModel> bakedSubModels = Maps.newHashMap();

		for (ResourceLocation subModel : modelData.getAllModels()) {
			IModel model = ModelLoaderRegistry.getModelOrLogError(subModel, "Couldn't load sub-model dependency: " + subModel);
			bakedSubModels.put(subModel, model.bake(new ModelStateComposition(state, model.getDefaultState()), format, bakedTextureGetter));
		}

		final IModel baseModel;
		if (base.isPresent()) {
			ResourceLocation baseLocation = base.get();
			baseModel = ModelLoaderRegistry.getModelOrLogError(baseLocation, "Couldn't load base-model dependency: " + baseLocation);
		} else {
			baseModel = ModelLoaderRegistry.getMissingModel();
		}

		final IBakedModel bakedBaseModel = baseModel.bake(new ModelStateComposition(state, baseModel.getDefaultState()), format, bakedTextureGetter);

		return new BakedModel(bakedBaseModel, modelData, bakedSubModels, IPerspectiveAwareModel.MapWrapper.getTransforms(state));
	}

	@Override
	public IModelState getDefaultState() {
		return TRSRTransformation.identity();
	}

	@Override
	public IModel process(ImmutableMap<String, String> customData) {
		boolean hasChanged = false;
		Optional<ResourceLocation> base = this.base;
		VariantModelData modelData = this.modelData;

		if (customData.containsKey(KEY_BASE)) {
			hasChanged = true;
			base = Optional.of(getLocation(customData.get(KEY_BASE)));
		}

		if (customData.containsKey(KEY_VARIANTS)) {
			hasChanged = true;
			modelData = VariantModelData.load(customData.get(KEY_VARIANTS));
		}

		return hasChanged? new VariantModel(base, modelData) : this;
	}

	private static ResourceLocation getLocation(String json) {
		JsonElement e = new JsonParser().parse(json);
		if (e.isJsonPrimitive())
			return new ModelResourceLocation(e.getAsString());

		FMLLog.severe("Expect ModelResourceLocation, got: ", json);
		return new ModelResourceLocation("builtin/missing", "missing");
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return ImmutableList.copyOf(Sets.union(modelData.getAllModels(), base.asSet()));
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return ImmutableList.of();
	}

}
