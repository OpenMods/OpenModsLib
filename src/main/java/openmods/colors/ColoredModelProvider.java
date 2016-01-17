package openmods.colors;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import openmods.config.game.ICustomItemModelProvider;

public class ColoredModelProvider implements ICustomItemModelProvider {

	@Override
	public void addCustomItemModels(Item item, ResourceLocation itemId, IModelRegistrationSink modelsOut) {
		for (ColorMeta meta : ColorMeta.VALUES) {
			final String modelPath = itemId.getResourcePath() + "_" + meta.name;
			final ResourceLocation model = new ResourceLocation(itemId.getResourceDomain(), modelPath);
			modelsOut.register(meta.vanillaBlockId, model);
		}
	}

}
