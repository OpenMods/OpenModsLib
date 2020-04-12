package openmods.model.eval;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import openmods.model.BakedModelAdapter;
import openmods.model.ModelUpdater;

public class EvalExpandModel extends EvalModelBase {

	public static final IUnbakedModel EMPTY = new EvalExpandModel(Optional.empty(), new EvaluatorFactory());

	private static class BakedEvalExpandModel extends BakedModelAdapter {

		private final IVarExpander expander;

		public BakedEvalExpandModel(ModelBakery bakery, IModel model, ISprite state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, IVarExpander expander) {
			super(model.bake(bakery, bakedTextureGetter, state, format), PerspectiveMapWrapper.getTransforms(state.getState()));
			this.expander = expander;
		}

		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData extState) {
			if (extState != null) {
				final EvalModelState originalArgs = MoreObjects.firstNonNull(extState.getData(EvalModelState.PROPERTY), EvalModelState.EMPTY);
				final EvalModelState updatedArgs = EvalModelState.create(expander.expand(originalArgs.getArgs()), originalArgs.isShortLived());

				final IModelData originalState = extState;
				extState = new IModelData() {
					@Override public boolean hasProperty(ModelProperty<?> prop) {
						return prop == EvalModelState.PROPERTY || originalState.hasProperty(prop);
					}

					@Nullable
					@Override public <T> T getData(ModelProperty<T> prop) {
						return prop == EvalModelState.PROPERTY? (T)updatedArgs : originalState.getData(prop);
					}

					@Nullable @Override public <T> T setData(ModelProperty<T> prop, T data) {
						return originalState.setData(prop, data);
					}
				};
			}

			return super.getQuads(state, side, rand, extState);
		}
	}

	private EvalExpandModel(Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		super(baseModel, evaluator);
	}

	@Override
	public IBakedModel bake(final ModelBakery bakery, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, ISprite state, VertexFormat format) {
		final IModel model = loadBaseModel(state.getState(), format, bakedTextureGetter);

		final IVarExpander expander = evaluatorFactory.createExpander();
		return new BakedEvalExpandModel(bakery, model, state, format, bakedTextureGetter, expander);
	}

	@Override
	protected IUnbakedModel update(Map<String, String> customData, ModelUpdater updater, Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		return updater.hasChanged()? new EvalExpandModel(baseModel, evaluator) : this;
	}

}
