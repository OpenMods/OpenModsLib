package openmods.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import openmods.block.OpenBlock;
import openmods.tileentity.OpenTileEntity;

import org.lwjgl.opengl.GL11;

public class GuiComponentItemStackSpinner extends BaseComponent {

	private OpenTileEntity tile;
	private OpenBlock block;
	private ItemStack stack;
	private Item item;
	private float rotationX = 0.1f;
	private float rotationY = 0f;
	private int meta = 0;
	private static ItemRenderer itemRenderer;
	
	public GuiComponentItemStackSpinner(int x, int y, ItemStack stack) {
		super(x, y);
		if (itemRenderer == null) {
			itemRenderer = new ItemRenderer(Minecraft.getMinecraft());
		}
		this.stack = stack;
	}

	@Override
	public int getWidth() {
		return 64;
	}

	@Override
	public int getHeight() {
		return 64;
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		super.render(minecraft, offsetX, offsetY, mouseX, mouseY);
		GL11.glPushMatrix();
		float scale = 30.0f;
		Tessellator t = Tessellator.instance;
		GL11.glTranslated(offsetX + x + (scale / 2), offsetY + y + (scale / 2), scale);
		GL11.glScaled(scale, -scale, scale);
		rotationY += 0.6f;
		rotationX = 20f;
		GL11.glRotatef(rotationX, 1, 0, 0);
		GL11.glRotatef(rotationY, 0, 1, 0);
		itemRenderer.renderItem(Minecraft.getMinecraft().thePlayer, stack, 0);
		GL11.glPopMatrix();
	}

}
