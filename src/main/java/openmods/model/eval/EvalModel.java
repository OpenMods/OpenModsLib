package openmods.model.eval;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import openmods.model.ModelUpdater;

public class EvalModel extends EvalModelBase {

	public static final IUnbakedModel EMPTY = new EvalModel(Optional.empty(), new EvaluatorFactory());

	private EvalModel(Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		super(baseModel, evaluator);
	}

	@Override
	public IBakedModel bake(ModelBakery bakery, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, ISprite state, VertexFormat format) {
		final IModel model = loadBaseModel(state.getState(), format, bakedTextureGetter);

		final ITransformEvaluator evaluator = evaluatorFactory.createEvaluator(model::getClip);
		return new BakedEvalModel(model, bakery, state, format, bakedTextureGetter, evaluator);
	}

	@Override
	protected IUnbakedModel update(Map<String, String> customData, ModelUpdater updater, Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		return updater.hasChanged()? new EvalModel(baseModel, evaluator) : this;
	}

}
