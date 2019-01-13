package openmods.model.eval;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import openmods.model.ModelUpdater;

public class EvalModel extends EvalModelBase {

	public static final IModel EMPTY = new EvalModel(Optional.empty(), new EvaluatorFactory());

	private EvalModel(Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		super(baseModel, evaluator);
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final IModel model = loadBaseModel(state, format, bakedTextureGetter);

		final ITransformEvaluator evaluator = evaluatorFactory.createEvaluator(model::getClip);
		return new BakedEvalModel(model, state, format, bakedTextureGetter, evaluator);
	}

	@Override
	protected IModel update(Map<String, String> customData, ModelUpdater updater, Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		return updater.hasChanged()? new EvalModel(baseModel, evaluator) : this;
	}

}
