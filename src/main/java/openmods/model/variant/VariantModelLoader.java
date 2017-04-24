package openmods.model.variant;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import openmods.OpenMods;

public class VariantModelLoader implements ICustomModelLoader {

	private static final Set<String> models = ImmutableSet.of(
			"variantmodel",
			"models/block/variantmodel",
			"models/item/variantmodel");

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		return modelLocation.getResourceDomain().equals(OpenMods.MODID)
				&& models.contains(modelLocation.getResourcePath());
	}

	@Override
	public IModel loadModel(ResourceLocation modelLocation) throws Exception {
		return VariantModel.INSTANCE;
	}

}
