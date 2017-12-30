package openmods.model.itemstate;

import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import openmods.state.State;
import openmods.state.StateContainer;

public class SimpleStateItemSetup {

	public static <T extends Item & ISimpleStateItem> void setupItemRendering(T item) {
		setupItemRendering(item.getRegistryName(), item);
	}

	public static <T extends Item & ISimpleStateItem> void setupItemRendering(ResourceLocation base, final T item) {
		final ResourceLocation id = item.getRegistryName();

		final StateContainer stateContainer = item.getStateContainer();

		for (State state : stateContainer.getAllStates()) {
			ModelResourceLocation modelLoc = new ModelResourceLocation(id, state.getVariant());
			ModelBakery.registerItemVariants(item, modelLoc);
		}

		ModelLoader.setCustomMeshDefinition(item, stack -> {
			final State state = item.getState(stack);
			return new ModelResourceLocation(id, state.getVariant());
		});
	}

}
