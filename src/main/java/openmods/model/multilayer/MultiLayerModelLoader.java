package openmods.model.multilayer;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import openmods.OpenMods;

public class MultiLayerModelLoader implements ICustomModelLoader {

	private static final Set<String> models = ImmutableSet.of(
			"multi-layer",
			"models/block/multi-layer",
			"models/item/multi-layer");

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		return modelLocation.getResourceDomain().equals(OpenMods.MODID)
				&& models.contains(modelLocation.getResourcePath());
	}

	@Override
	public IModel loadModel(ResourceLocation modelLocation) {
		return MultiLayerModel.EMPTY;
	}
}