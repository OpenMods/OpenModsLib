package openmods.gui.component;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import openmods.gui.IComponentParent;

import org.lwjgl.opengl.GL11;

public class GuiComponentItemStackSpinner extends BaseComponent {

	private final ItemStack stack;
	private float rotationY = 0f;

	public GuiComponentItemStackSpinner(IComponentParent parent, int x, int y, ItemStack stack) {
		super(parent, x, y);
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
	public void renderOverlay(int offsetX, int offsetY, int mouseX, int mouseY) {
		GL11.glPushMatrix();
		GlStateManager.enableDepth();
		float scale = 30.0f;
		GL11.glTranslated(offsetX + x + (scale / 2), offsetY + y + (scale / 2), scale);
		GL11.glScaled(scale, -scale, scale);
		rotationY += 0.6f;
		GL11.glRotatef(20, 1, 0, 0);
		GL11.glRotatef(rotationY, 0, 1, 0);
		GL11.glColor3f(1, 1, 1);
		renderItem(stack);
		GlStateManager.disableDepth();
		GL11.glPopMatrix();
	}

	public void renderItem(ItemStack itemStack) {
		// TODO
	}
}
