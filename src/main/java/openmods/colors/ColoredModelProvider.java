package openmods.colors;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import openmods.config.game.ICustomItemModelProvider;

public class ColoredModelProvider implements ICustomItemModelProvider {
	@Override
	public void addCustomItemModels(Item item, ResourceLocation itemId, IModelRegistrationSink modelsOut) {
		for (ColorMeta meta : ColorMeta.VALUES) {
			modelsOut.register(meta.vanillaBlockId, new ModelResourceLocation(itemId, "inventory_" + meta.name));
		}
	}

}
