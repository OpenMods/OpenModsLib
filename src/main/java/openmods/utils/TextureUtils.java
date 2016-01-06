package openmods.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
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

	public static TextureAtlasSprite getFluidTexture(FluidStack fluid) {
		final ResourceLocation textureLocation = fluid.getFluid().getStill(fluid);
		return getTextureAtlasLocation(textureLocation);
	}

	public static TextureAtlasSprite getFluidTexture(Fluid fluid) {
		final ResourceLocation textureLocation = fluid.getStill();
		return getTextureAtlasLocation(textureLocation);
	}

	public static TextureAtlasSprite getTextureAtlasLocation(final ResourceLocation textureLocation) {
		return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(textureLocation.toString());
	}

	public static int getRandomNumber() {
		return 4;
	}
}
