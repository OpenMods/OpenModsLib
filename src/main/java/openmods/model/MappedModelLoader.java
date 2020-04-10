package openmods.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;

public class MappedModelLoader implements ICustomModelLoader {

	public static class Builder {
		private final ImmutableMap.Builder<String, IUnbakedModel> modelsBuilder = ImmutableMap.builder();

		public Builder put(String id, IUnbakedModel model) {
			modelsBuilder.put("models/" + id, model);
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

	private final Map<String, IUnbakedModel> models;

	private final String modid;

	private MappedModelLoader(String modid, Map<String, IUnbakedModel> models) {
		this.modid = modid;
		this.models = models;
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
	}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		return modid.equals(modelLocation.getNamespace()) &&
				models.containsKey(modelLocation.getPath());
	}

	@Override
	public IUnbakedModel loadModel(ResourceLocation modelLocation) {
		return models.get(modelLocation.getPath());
	}

}
