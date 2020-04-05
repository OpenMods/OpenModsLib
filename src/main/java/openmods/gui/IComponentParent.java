package openmods.gui;

import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public interface IComponentParent {

	Minecraft getMinecraft();

	AtlasTexture getBlocksTextureMap();

	TextureAtlasSprite getIcon(ResourceLocation location);

	FontRenderer getFontRenderer();

	ItemRenderer getItemRenderer();

	SoundHandler getSoundHandler();

	void bindTexture(ResourceLocation texture);

	void drawHoveringText(List<String> textLines, int x, int y);

	void drawItemStackTooltip(@Nonnull ItemStack stack, int x, int y);

	void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor);

}
