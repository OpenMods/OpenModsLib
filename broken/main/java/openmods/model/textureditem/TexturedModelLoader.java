package openmods.model.textureditem;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.client.model.IModelLoader;

public class TexturedModelLoader implements IModelLoader<TexturedModelGeometry> {
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
	}

	@Override
	public TexturedModelGeometry read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
		return null;
	}
}
