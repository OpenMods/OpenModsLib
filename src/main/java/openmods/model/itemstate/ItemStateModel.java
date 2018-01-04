package openmods.model.itemstate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import openmods.model.ModelUpdater;
import openmods.state.State;
import openmods.state.StateContainer;
import openmods.utils.CollectionUtils;

public class ItemStateModel implements IModel {

	private final Optional<ResourceLocation> itemLocation;

	private final Optional<ResourceLocation> defaultModel;

	private final Map<State, ResourceLocation> stateModels;

	private ItemStateModel(Optional<ResourceLocation> itemLocation, Optional<ResourceLocation> defaultModel) {
		this.itemLocation = itemLocation;
		this.defaultModel = defaultModel;

		this.stateModels = createModelLocations();
	}

	public static final ItemStateModel EMPTY = new ItemStateModel(Optional.empty(), Optional.empty());

	private Map<State, ResourceLocation> createModelLocations() {
		if (!itemLocation.isPresent()) return ImmutableMap.of();

		final Item item = Item.REGISTRY.getObject(itemLocation.get());

		if (!(item instanceof IStateItem)) return ImmutableMap.of();

		final StateContainer stateContainer = ((IStateItem)item).getStateContainer();

		final ImmutableMap.Builder<State, ResourceLocation> result = ImmutableMap.builder();

		final ResourceLocation base = item.getRegistryName();

		for (State state : stateContainer.getAllStates())
			result.put(state, new ModelResourceLocation(base, state.getVariant()));

		return result.build();
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Sets.union(ImmutableSet.copyOf(stateModels.values()), CollectionUtils.asSet(defaultModel));
	}

	@Override
	public IBakedModel bake(IModelState state, final VertexFormat format, final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final IModel defaultModel;

		if (this.defaultModel.isPresent()) {
			defaultModel = getModel(this.defaultModel.get());
		} else if (!this.stateModels.isEmpty()) {
			final ResourceLocation first = this.stateModels.values().iterator().next();
			defaultModel = getModel(first);
		} else {
			defaultModel = ModelLoaderRegistry.getMissingModel();
		}

		final IBakedModel bakedDefaultModel = defaultModel.bake(defaultModel.getDefaultState(), format, bakedTextureGetter);

		final Map<State, IBakedModel> bakedStateModels = Maps.transformValues(stateModels, input -> {
			final IModel model = getModel(input);
			return model.bake(model.getDefaultState(), format, bakedTextureGetter);
		});

		return new ItemStateOverrideList(bakedStateModels).wrapModel(bakedDefaultModel);
	}

	private static IModel getModel(ResourceLocation model) {
		return ModelLoaderRegistry.getModelOrLogError(model, "Couldn't load model dependency: " + model);
	}

	@Override
	public IModel process(ImmutableMap<String, String> customData) {
		final ModelUpdater updater = new ModelUpdater(customData);

		final Optional<ResourceLocation> itemLocation = updater.get("item", ModelUpdater.RESOURCE_LOCATION, this.itemLocation);
		final Optional<ResourceLocation> defaultModel = updater.get("default", ModelUpdater.MODEL_LOCATION, this.defaultModel);

		return updater.hasChanged()? new ItemStateModel(itemLocation, defaultModel) : this;
	}

}
