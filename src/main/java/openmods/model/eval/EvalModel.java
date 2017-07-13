package openmods.model.eval;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import java.util.Collection;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelCustomData;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.model.ModelUpdater;
import openmods.model.ModelUpdater.ValueConverter;

public class EvalModel implements IModelCustomData {

	public static final IModel EMPTY = new EvalModel(Optional.<ResourceLocation> absent(), new EvaluatorFactory());

	private final Optional<ResourceLocation> baseModel;

	private final EvaluatorFactory evaluatorFactory;

	private EvalModel(Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		this.baseModel = baseModel;
		this.evaluatorFactory = evaluator;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return baseModel.asSet();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return ImmutableList.of();
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final IModel model;
		if (baseModel.isPresent()) {
			model = ModelLoaderRegistry.getModelOrLogError(baseModel.get(), "Couldn't load eval model dependency: " + baseModel.get());
		} else {
			model = ModelLoaderRegistry.getMissingModel();
		}

		final IEvaluator evaluator = evaluatorFactory.bind(model);
		return new BakedEvalModel(model, state, format, bakedTextureGetter, evaluator);
	}

	@Override
	public IModelState getDefaultState() {
		return TRSRTransformation.identity();
	}

	@Override
	public IModel process(ImmutableMap<String, String> customData) {
		final ModelUpdater updater = new ModelUpdater(customData);

		final Optional<ResourceLocation> baseModel = updater.get("base", ModelUpdater.MODEL_LOCATION, this.baseModel);

		final EvaluatorFactory evaluatorFactory = updater.get("transforms", new ValueConverter<EvaluatorFactory>() {
			@Override
			public EvaluatorFactory convert(String name, JsonElement element) {
				final EvaluatorFactory result = new EvaluatorFactory();
				if (element.isJsonArray()) {
					for (JsonElement e : element.getAsJsonArray())
						appendStatement(result, e);
				} else {
					appendStatement(result, element);
				}

				return result;
			}

			private void appendStatement(EvaluatorFactory factory, JsonElement statement) {
				factory.appendStatement(statement.getAsString());
			}
		}, this.evaluatorFactory);

		return updater.hasChanged()? new EvalModel(baseModel, evaluatorFactory) : this;
	}

}
