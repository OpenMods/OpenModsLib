package openmods.model.itemstate;

import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import openmods.state.ItemState;

public class SimpleStateItemSetup {

	public static <T extends Item & ISimpleStateItem> void setupItemRendering(T item) {
		setupItemRendering(item.getRegistryName(), item);
	}

	public static <T extends Item & ISimpleStateItem> void setupItemRendering(ResourceLocation base, final T item) {
		final ResourceLocation id = item.getRegistryName();

		final StateContainer<Item, ItemState> stateContainer = item.getStateContainer();

		for (ItemState state : stateContainer.getValidStates()) {
			ModelResourceLocation modelLoc = new ModelResourceLocation(id, state.getVariant());
			// TODO 1.14 Wait for forge functionalty
		}

		// TODO 1.14 Wait for forge functionalty
	}

}
