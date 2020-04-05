package openmods.model.itemstate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.item.Item;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BasicState;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import openmods.model.ModelUpdater;
import openmods.state.ItemState;
import openmods.utils.CollectionUtils;

public class ItemStateModel implements IUnbakedModel {

	private final Optional<ResourceLocation> itemLocation;

	private final Optional<ResourceLocation> defaultModel;

	private final Map<ItemState, ResourceLocation> stateModels;

	private ItemStateModel(Optional<ResourceLocation> itemLocation, Optional<ResourceLocation> defaultModel) {
		this.itemLocation = itemLocation;
		this.defaultModel = defaultModel;

		this.stateModels = createModelLocations();
	}

	public static final ItemStateModel EMPTY = new ItemStateModel(Optional.empty(), Optional.empty());

	private Map<ItemState, ResourceLocation> createModelLocations() {
		if (!itemLocation.isPresent()) return ImmutableMap.of();

		final Item item = ForgeRegistries.ITEMS.getValue(itemLocation.get());

		if (!(item instanceof IStateItem)) return ImmutableMap.of();

		final StateContainer<Item, ItemState> stateContainer = ((IStateItem)item).getStateContainer();

		final ImmutableMap.Builder<ItemState, ResourceLocation> result = ImmutableMap.builder();

		final ResourceLocation base = item.getRegistryName();

		for (ItemState state : stateContainer.getValidStates()) {
			result.put(state, new ModelResourceLocation(base, state.getVariant()));
		}

		return result.build();
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Sets.union(ImmutableSet.copyOf(stateModels.values()), CollectionUtils.asSet(defaultModel));
	}

	@Override public Collection<ResourceLocation> getTextures(Function<ResourceLocation, IUnbakedModel> modelGetter, Set<String> missingTextureErrors) {
		return Collections.emptyList();
	}

	@Override
	public IBakedModel bake(final ModelBakery bakery, final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, ISprite state, final VertexFormat format) {
		final IModel defaultModel;

		if (this.defaultModel.isPresent()) {
			defaultModel = getModel(this.defaultModel.get());
		} else if (!this.stateModels.isEmpty()) {
			final ResourceLocation first = this.stateModels.values().iterator().next();
			defaultModel = getModel(first);
		} else {
			defaultModel = ModelLoaderRegistry.getMissingModel();
		}

		final IBakedModel bakedDefaultModel = defaultModel.bake(bakery, bakedTextureGetter, new BasicState(defaultModel.getDefaultState(), state.isUvLock()), format);

		final Map<ItemState, IBakedModel> bakedStateModels = Maps.transformValues(stateModels, input -> {
			final IModel model = getModel(input);
			return model.bake(bakery, bakedTextureGetter, new BasicState(model.getDefaultState(), state.isUvLock()), format);
		});

		return new ItemStateOverrideList(bakedStateModels).wrapModel(bakedDefaultModel);
	}

	private static IModel getModel(ResourceLocation model) {
		return ModelLoaderRegistry.getModelOrLogError(model, "Couldn't load model dependency: " + model);
	}

	@Override
	public IUnbakedModel process(ImmutableMap<String, String> customData) {
		final ModelUpdater updater = new ModelUpdater(customData);

		final Optional<ResourceLocation> itemLocation = updater.get("item", ModelUpdater.RESOURCE_LOCATION, this.itemLocation);
		final Optional<ResourceLocation> defaultModel = updater.get("default", ModelUpdater.MODEL_LOCATION, this.defaultModel);

		return updater.hasChanged()? new ItemStateModel(itemLocation, defaultModel) : this;
	}

}
