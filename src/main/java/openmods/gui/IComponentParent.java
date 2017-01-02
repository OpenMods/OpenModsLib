package openmods.gui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public interface IComponentParent {

	public Minecraft getMinecraft();

	public TextureMap getBlocksTextureMap();

	public TextureAtlasSprite getIcon(ResourceLocation location);

	public FontRenderer getFontRenderer();

	public RenderItem getItemRenderer();

	public SoundHandler getSoundHandler();

	public void bindTexture(ResourceLocation texture);

	public void drawHoveringText(List<String> textLines, int x, int y);

	public void drawItemStackTooltip(ItemStack stack, int x, int y);

	public void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor);

}
