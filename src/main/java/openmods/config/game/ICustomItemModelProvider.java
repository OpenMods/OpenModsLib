package openmods.config.game;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public interface ICustomItemModelProvider {

	public interface IModelRegistrationSink {
		public void register(int meta, ResourceLocation modelLocation);
	}

	public void addCustomItemModels(Item item, ResourceLocation itemId, IModelRegistrationSink modelsOut);
}
