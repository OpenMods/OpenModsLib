package openmods.gui.component;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

public class GuiComponentCheckbox extends BaseComponent {

	protected int color;
	private boolean value;

	public GuiComponentCheckbox(int x, int y, boolean initialValue, int color) {
		super(x, y);
		this.color = color;
		this.value = initialValue;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		super.render(minecraft, offsetX, offsetY, mouseX, mouseY);
		GL11.glColor4f(1, 1, 1, 1);
		bindComponentsSheet();
		drawTexturedModalRect(offsetX + x, offsetY + y, value? 16 : 0, 62, 8, 8);
	}

	@Override
	public void mouseDown(int x, int y, int button) {
		super.mouseDown(x, y, button);
		value = !value;
	}

	@Override
	public int getHeight() {
		return 8;
	}

	@Override
	public int getWidth() {
		return 8;
	}
}
