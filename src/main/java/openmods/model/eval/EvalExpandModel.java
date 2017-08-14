package openmods.model.eval;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import java.util.List;
import java.util.Map;
import jline.internal.Log;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;
import openmods.model.BakedModelAdapter;
import openmods.model.ModelUpdater;

public class EvalExpandModel extends EvalModelBase {

	public static final IModel EMPTY = new EvalExpandModel(Optional.<ResourceLocation> absent(), Optional.<ResourceLocation> absent(), new EvaluatorFactory());

	private static class BakedEvalExpandModel extends BakedModelAdapter {

		private final IVarExpander expander;

		private final IBlockState defaultBlockState;

		public BakedEvalExpandModel(IModel model, IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, IBlockState defaultBlockState, IVarExpander expander) {
			super(model.bake(state, format, bakedTextureGetter), MapWrapper.getTransforms(state));
			this.expander = expander;
			this.defaultBlockState = defaultBlockState;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			if (state == null)
				state = defaultBlockState;

			if (state instanceof IExtendedBlockState) {
				final IExtendedBlockState extState = (IExtendedBlockState)state;
				final EvalModelState originalArgs = Objects.firstNonNull(extState.getValue(EvalModelState.PROPERTY), EvalModelState.EMPTY);
				final EvalModelState updatedArgs = EvalModelState.create(expander.expand(originalArgs.getArgs()), originalArgs.isShortLived());
				state = extState.withProperty(EvalModelState.PROPERTY, updatedArgs);
			}

			return super.getQuads(state, side, rand);
		}
	}

	private final Optional<ResourceLocation> defaultBlockState;

	private EvalExpandModel(Optional<ResourceLocation> defaultBlockState, Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		super(baseModel, evaluator);
		this.defaultBlockState = defaultBlockState;
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final IModel model = loadBaseModel(state, format, bakedTextureGetter);

		IBlockState blockState = null;

		if (defaultBlockState.isPresent()) {
			final Block defaultBlock = Block.REGISTRY.getObject(defaultBlockState.get());
			if (defaultBlock != Blocks.AIR) {
				blockState = defaultBlock.getDefaultState();
				if (!(blockState instanceof IExtendedBlockState) ||
						!((IExtendedBlockState)blockState).getUnlistedNames().contains(EvalModelState.PROPERTY)) {
					Log.warn("State %s does not contain eval state property", blockState);
				}
			} else {
				Log.warn("Can't find default block: %s", defaultBlockState.get());
			}
		}

		final IVarExpander expander = evaluatorFactory.createExpander();
		return new BakedEvalExpandModel(model, state, format, bakedTextureGetter, blockState, expander);
	}

	@Override
	protected IModel update(Map<String, String> customData, ModelUpdater updater, Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		final Optional<ResourceLocation> defaultStateBlock = updater.get("default_state", ModelUpdater.RESOURCE_LOCATION, this.defaultBlockState);

		return updater.hasChanged()? new EvalExpandModel(defaultStateBlock, baseModel, evaluator) : this;
	}

}
