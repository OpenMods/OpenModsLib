package openmods.gui.component;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public class GuiComponentCraftingGrid extends GuiComponentSprite {
	protected static RenderItem itemRenderer = new RenderItem();
	private ItemStack[] items;

	public GuiComponentCraftingGrid(int x, int y, ItemStack[] items, IIcon icon, ResourceLocation texture) {
		super(x, y, icon, texture);
		this.items = items;
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		super.renderOverlay(minecraft, offsetX, offsetY, mouseX, mouseY);

		int relativeMouseX = mouseX + offsetX - x;
		int relativeMouseY = mouseY + offsetY - y;
		int gridOffsetX = 1;
		int gridOffsetY = 1;
		int itemBoxSize = 19;

		ItemStack tooltip = null;
		int i = 0;
		for (ItemStack input : items) {
			if (input != null) {
				int row = (i % 3);
				int column = i / 3;
				int itemX = offsetX + gridOffsetX + (row * itemBoxSize);
				int itemY = offsetY + gridOffsetY + (column * itemBoxSize);
				drawItemStack(input, x + itemX, y + itemY, "");
				if (relativeMouseX > itemX - 2 && relativeMouseX < itemX - 2 + itemBoxSize &&
						relativeMouseY > itemY - 2 && relativeMouseY < itemY - 2 + itemBoxSize) {
					tooltip = input;
				}
			}
			i++;
		}
		if (tooltip != null) {
			drawItemStackTooltip(tooltip, relativeMouseX + 25, relativeMouseY + 30);
		}
	}

	protected void drawItemStackTooltip(ItemStack stack, int x, int y) {
		final Minecraft mc = Minecraft.getMinecraft();
		FontRenderer font = Objects.firstNonNull(stack.getItem().getFontRenderer(stack), mc.fontRenderer);

		@SuppressWarnings("unchecked")
		List<String> list = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);

		List<String> colored = Lists.newArrayListWithCapacity(list.size());
		colored.add(getRarityColor(stack) + list.get(0));
		for (String line : list)
			colored.add(EnumChatFormatting.GRAY + line);

		drawHoveringText(colored, x, y, font);
	}

	protected EnumChatFormatting getRarityColor(ItemStack stack) {
		return stack.getRarity().rarityColor;
	}

	private void drawItemStack(ItemStack par1ItemStack, int par2, int par3, String par4Str)
	{
		GL11.glTranslatef(0.0F, 0.0F, 32.0F);
		this.zLevel = 200.0F;
		itemRenderer.zLevel = 200.0F;
		RenderHelper.enableGUIStandardItemLighting();
		GL11.glColor3f(1f, 1f, 1f);
		GL11.glEnable(GL11.GL_NORMALIZE);
		FontRenderer font = null;
		if (par1ItemStack != null) font = par1ItemStack.getItem().getFontRenderer(par1ItemStack);
		if (font == null) font = Minecraft.getMinecraft().fontRenderer;
		itemRenderer.renderItemAndEffectIntoGUI(font, Minecraft.getMinecraft().getTextureManager(), par1ItemStack, par2, par3);
		itemRenderer.renderItemOverlayIntoGUI(font, Minecraft.getMinecraft().getTextureManager(), par1ItemStack, par2, par3, par4Str);
		this.zLevel = 0.0F;
		itemRenderer.zLevel = 0.0F;
	}
}
