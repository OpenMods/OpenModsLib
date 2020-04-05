package openmods.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import openmods.Log;

public class TextureUtils {

	public static void bindTextureToClient(ResourceLocation texture) {
		if (texture != null) {
			final Minecraft mc = Minecraft.getInstance();
			if (mc != null) {
				mc.getTextureManager().bindTexture(texture);
			} else {
				Log.warn("Binding texture to null client.");
			}
		} else {
			Log.warn("Invalid texture location '%s'", texture);
		}
	}

	public static TextureAtlasSprite getTextureAtlasLocation(final ResourceLocation textureLocation) {
		return Minecraft.getInstance().getTextureMap().getAtlasSprite(textureLocation.toString());
	}

	public static int getRandomNumber() {
		return 4;
	}
}
