package openmods.model.eval;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.Map;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.animation.IAnimatedModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IClip;
import net.minecraftforge.common.model.animation.IJoint;
import openmods.model.ModelUpdater;
import openmods.model.eval.EvaluatorFactory.IClipProvider;

public class EvalModel extends EvalModelBase {

	public static final IModel EMPTY = new EvalModel(Optional.<ResourceLocation> absent(), new EvaluatorFactory());

	private static final ITransformEvaluator EMPTY_EVALUATOR = new ITransformEvaluator() {
		@Override
		public TRSRTransformation evaluate(IJoint joint, Map<String, Float> args) {
			return TRSRTransformation.identity();
		}
	};

	private EvalModel(Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		super(baseModel, evaluator);
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final IModel model = loadBaseModel(state, format, bakedTextureGetter);

		final ITransformEvaluator evaluator;
		if (model instanceof IAnimatedModel) {
			final IAnimatedModel animatedModel = (IAnimatedModel)model;
			evaluator = evaluatorFactory.createEvaluator(new IClipProvider() {
				@Override
				public Optional<? extends IClip> get(String name) {
					return animatedModel.getClip(name);
				}
			});
		} else {
			evaluator = EMPTY_EVALUATOR;
		}
		return new BakedEvalModel(model, state, format, bakedTextureGetter, evaluator);
	}

	@Override
	protected IModel update(Map<String, String> customData, ModelUpdater updater, Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		return updater.hasChanged()? new EvalModel(baseModel, evaluator) : this;
	}

}
