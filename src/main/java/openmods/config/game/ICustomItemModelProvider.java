package openmods.config.game;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public interface ICustomItemModelProvider {

	interface IModelRegistrationSink {
		void register(int meta, ResourceLocation modelLocation);
	}

	void addCustomItemModels(Item item, ResourceLocation itemId, IModelRegistrationSink modelsOut);
}
