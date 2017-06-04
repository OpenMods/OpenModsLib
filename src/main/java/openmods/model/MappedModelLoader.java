package openmods.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;

public class MappedModelLoader implements ICustomModelLoader {

	public static class Builder {
		private final ImmutableMap.Builder<String, IModel> modelsBuilder = ImmutableMap.builder();

		public Builder put(String id, IModel model) {
			modelsBuilder.put(id, model);
			modelsBuilder.put("models/block/" + id, model);
			modelsBuilder.put("models/item/" + id, model);
			return this;
		}

		public MappedModelLoader build(String modid) {
			Preconditions.checkNotNull(modid);
			return new MappedModelLoader(modid, modelsBuilder.build());
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	private final Map<String, IModel> models;

	private final String modid;

	private MappedModelLoader(String modid, Map<String, IModel> models) {
		this.modid = modid;
		this.models = models;
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		return modid.equals(modelLocation.getResourceDomain()) &&
				models.containsKey(modelLocation.getResourcePath());
	}

	@Override
	public IModel loadModel(ResourceLocation modelLocation) throws Exception {
		return models.get(modelLocation.getResourcePath());
	}

}
