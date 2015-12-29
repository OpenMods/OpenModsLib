package openmods.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import openmods.Log;

public class TextureUtils {

	public static void bindTextureToClient(ResourceLocation texture) {
		if (texture != null) {
			final Minecraft mc = Minecraft.getMinecraft();
			if (mc != null) {
				mc.renderEngine.bindTexture(texture);
			} else {
				Log.warn("Binding texture to null client.");
			}
		} else {
			Log.warn("Invalid texture location '%s'", texture);
		}
	}

	public static int getRandomNumber() {
		return 4;
	}
}
