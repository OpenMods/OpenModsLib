package openmods.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import openmods.Log;

public final class TextureUtils {

	public static void bindTextureToClient(ResourceLocation texture) {
		if (texture != null) {
			if (Minecraft.getMinecraft() != null) {
				Minecraft.getMinecraft().renderEngine.bindTexture(texture);
			} else {
				Log.warn("Binding texture to null client.");
			}
		} else {
			Log.warn("Invalid texture location '%s'", texture);
		}
	}

	public static void bindDefaultTerrainTexture() {
		bindTextureToClient(TextureMap.locationBlocksTexture);
	}

	public static void bindDefaultItemsTexture() {
		bindTextureToClient(TextureMap.locationItemsTexture);
	}

	public static int getRandomNumber() {
		return 4;
	}
}
