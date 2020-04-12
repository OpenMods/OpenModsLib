package openmods.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;

public interface IComponentParent {

	Minecraft getMinecraft();

	AtlasTexture getBlocksTextureMap();

	TextureAtlasSprite getIcon(ResourceLocation location);

	FontRenderer getFontRenderer();

	ItemRenderer getItemRenderer();

	SoundHandler getSoundHandler();

	void bindTexture(ResourceLocation texture);

	void drawHoveringText(MatrixStack matrixStack, List<? extends IReorderingProcessor> textLines, int x, int y);

	void drawItemStackTooltip(MatrixStack matrixStack, @Nonnull ItemStack stack, int x, int y);

	void drawGradientRect(MatrixStack matrixStack, int left, int top, int right, int bottom, int startColor, int endColor);

}
