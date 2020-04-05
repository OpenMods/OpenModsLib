package openmods.model.eval;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import openmods.model.ModelUpdater;
import openmods.model.ModelUpdater.ValueConverter;
import openmods.utils.CollectionUtils;

public abstract class EvalModelBase implements IUnbakedModel {

	protected final Optional<ResourceLocation> baseModel;

	protected final EvaluatorFactory evaluatorFactory;

	protected EvalModelBase(Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator) {
		this.baseModel = baseModel;
		this.evaluatorFactory = evaluator;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return CollectionUtils.asSet(baseModel);
	}

	@Override
	public Collection<ResourceLocation> getTextures(Function<ResourceLocation, IUnbakedModel> modelGetter, Set<String> missingTextureErrors) {
		return Collections.emptyList();
	}

	protected IModel loadBaseModel(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		if (baseModel.isPresent()) {
			return ModelLoaderRegistry.getModelOrLogError(baseModel.get(), "Couldn't load eval model dependency: " + baseModel.get());
		} else {
			return ModelLoaderRegistry.getMissingModel();
		}
	}

	@Override
	public IUnbakedModel process(ImmutableMap<String, String> customData) {
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

		return update(customData, updater, baseModel, evaluatorFactory);
	}

	protected abstract IUnbakedModel update(Map<String, String> customData, ModelUpdater updater, Optional<ResourceLocation> baseModel, EvaluatorFactory evaluator);

}
