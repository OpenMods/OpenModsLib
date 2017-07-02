package openmods.model;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;

public class ModelUtils {

	public static void registerMetaInsensitiveModel(Block block) {
		registerMetaInsensitiveModel(Item.getItemFromBlock(block));
	}

	public static void registerMetaInsensitiveModel(Item item) {
		registerMetaInsensitiveModel(item, "inventory");
	}

	public static void registerMetaInsensitiveModel(Block item, String variant) {
		registerMetaInsensitiveModel(Item.getItemFromBlock(item), variant);
	}

	public static void registerMetaInsensitiveModel(Item item, String variant) {
		final ModelResourceLocation location = new ModelResourceLocation(item.getRegistryName(), variant);

		ModelBakery.registerItemVariants(item, location);

		ModelLoader.setCustomMeshDefinition(item, new ItemMeshDefinition() {
			@Override
			public ModelResourceLocation getModelLocation(ItemStack stack) {
				return location;
			}
		});
	}

}
