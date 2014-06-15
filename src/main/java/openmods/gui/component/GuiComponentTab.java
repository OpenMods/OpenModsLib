package openmods.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GuiComponentTab extends GuiComponentBox {

	private static final int FOLDED_WIDTH = 24;
	private static final int FOLDED_HEIGHT = 24;
	protected static RenderItem itemRenderer = new RenderItem();
	protected final int expandedWidth;
	protected final int expandedHeight;
	private boolean active = false;
	private ItemStack iconStack;
	private double dWidth;
	private double dHeight;

	public GuiComponentTab(int color, ItemStack iconStack, int expandedWidth, int expandedHeight) {
		super(-4, 0, FOLDED_WIDTH, FOLDED_HEIGHT, 0, 5, color);
		this.expandedWidth = expandedWidth;
		this.expandedHeight = expandedHeight;
		this.iconStack = iconStack;
		this.dWidth = 24.0;
		this.dHeight = 24.0;
	}

	@Override
	public void renderTopLeftCorner(int offsetX, int offsetY) {}

	@Override
	public void renderBottomLeftCorner(int offsetX, int offsetY) {}

	@Override
	public void renderLeftEdge(int offsetX, int offsetY) {}

	@Override
	protected boolean areChildrenActive() {
		return active && width == expandedWidth && height == expandedHeight;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		double targetWidth = active? expandedWidth : FOLDED_WIDTH;
		double targetHeight = active? expandedHeight : FOLDED_HEIGHT;
		if (width != targetWidth) dWidth += (targetWidth - dWidth) / 4;
		if (height != targetHeight) dHeight += (targetHeight - dHeight) / 4;

		width = (int)Math.round(dWidth);
		height = (int)Math.round(dHeight);
		super.render(minecraft, offsetX, offsetY, mouseX, mouseY);

		GL11.glColor4f(1, 1, 1, 1);
		RenderHelper.enableGUIStandardItemLighting();
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		itemRenderer.zLevel = zLevel + 50; // <- critical! Must be >= 50
		itemRenderer.renderItemIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), iconStack,
				offsetX + x + 3, offsetY + y + 3);
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glDisable(GL11.GL_LIGHTING);
	}

	public boolean isOrigin(int x, int y) {
		return x < FOLDED_WIDTH && y < FOLDED_WIDTH;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
