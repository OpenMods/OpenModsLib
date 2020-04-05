package openmods.renderer;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.TextureUtil;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class DisposableDynamicTexture extends Texture implements AutoCloseable {
	@Nullable
	private NativeImage dynamicTextureData;
	private int width = -1;
	private int height = -1;

	private static int textureCounter;

	public DisposableDynamicTexture() {}

	public NativeImage prepareImage(int width, int height) {
		if (width != this.width || height != this.height) {
			this.width = width;
			this.height = height;
			TextureUtil.prepareImage(getGlTextureId(), width, height);
			if (dynamicTextureData != null) {
				dynamicTextureData.close();
				dynamicTextureData = null;
			}
		}

		if (dynamicTextureData == null) {
			dynamicTextureData = new NativeImage(width, height, true);
		}

		return dynamicTextureData;
	}

	@Override
	public void loadTexture(IResourceManager par1ResourceManager) {}

	public void upload() {
		Preconditions.checkNotNull(dynamicTextureData, "Texture not allocated");
		bindTexture();
		dynamicTextureData.uploadTextureSub(0, 0, 0, false);
	}

	public ResourceLocation register(TextureManager manager, String prefix) {
		ResourceLocation location = new ResourceLocation(String.format("dynamic_o/%s_%d", prefix, textureCounter));
		textureCounter++;

		manager.loadTexture(location, this);
		return location;
	}

	@Override
	public void close() {
		if (dynamicTextureData != null) {
			dynamicTextureData.close();
		}
	}
}
