package openmods.gui.component;

import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class GuiComponentCraftingGrid extends GuiComponentSprite {

	private ItemStack[] items;

	public GuiComponentCraftingGrid(int x, int y, ItemStack[] items, IIcon icon, ResourceLocation texture) {
		super(x, y, icon, texture);
		Preconditions.checkNotNull(items, "No items in grid");
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

		for (int i = 0; i < items.length; i++) {
			ItemStack input = items[i];
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
		}
		if (tooltip != null) {
			drawItemStackTooltip(tooltip, relativeMouseX + 25, relativeMouseY + 30);
		}
	}

	protected void drawItemStackTooltip(ItemStack stack, int x, int y) {
		final Minecraft mc = Minecraft.getMinecraft();
		FontRenderer font = Objects.firstNonNull(stack.getItem().getFontRenderer(stack), mc.fontRenderer);

		GL11.glColor3f(1, 1, 1);
		GL11.glDisable(GL11.GL_LIGHTING);
		@SuppressWarnings("unchecked")
		List<String> list = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);

		List<String> colored = Lists.newArrayListWithCapacity(list.size());
		Iterator<String> it = list.iterator();
		colored.add(getRarityColor(stack) + it.next());

		while (it.hasNext())
			colored.add(EnumChatFormatting.GRAY + it.next());

		drawHoveringText(colored, x, y, font);
	}

	private static EnumChatFormatting getRarityColor(ItemStack stack) {
		return stack.getRarity().rarityColor;
	}

}
