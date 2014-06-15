package openmods.gui.component;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

public class GuiComponentSlider extends BaseComponent {

	private static final int HANDLE_SIZE = 8;

	private int width;
	private int min;
	private int max;
	private int value;
	private double stepSize;
	private int steps;
	private int startDragX;
	private boolean showValue = true;

	public GuiComponentSlider(int x, int y, int width, int min, int max, int initialValue, boolean showValue) {
		this(x, y, width, min, max, initialValue);
		this.showValue = showValue;
	}

	public GuiComponentSlider(int x, int y, int width, int min, int max, int initialValue) {
		super(x, y);
		this.width = width;
		this.min = min;
		this.max = max;
		this.steps = max - min;
		this.value = initialValue;
		this.stepSize = (double)(width - HANDLE_SIZE - 2) / (double)steps;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		super.render(minecraft, offsetX, offsetY, mouseX, mouseY);
		GL11.glColor4f(1, 1, 1, 1);
		int left = offsetX + x;
		int top = offsetY + y;
		int barStartX = left + 1;
		bindComponentsSheet();
		drawTexturedModalRect(left, top, 0, 70, 1, getHeight());
		GL11.glPushMatrix();
		GL11.glTranslated(left + 1, top, 0);
		GL11.glScaled(getWidth() - 2, 1, 1);
		drawTexturedModalRect(0, 0, 1, 70, 1, getHeight());
		GL11.glPopMatrix();
		drawTexturedModalRect(left + getWidth() - 1, top, 2, 70, 1, getHeight());
		int handleX = (int)Math.floor(barStartX + stepSize * (value - min));

		value = Math.max(min, Math.min(max, value));
		drawTexturedModalRect(handleX, top + 1, 3, 70, 9, 10);
		if (showValue) {
			String label = formatValue(value);
			int strWidth = minecraft.fontRenderer.getStringWidth(label);
			minecraft.fontRenderer.drawString(label, handleX + 4
					- (strWidth / 2), top + 15, 4210752);
		}
	}

	@Override
	public void mouseDown(int mouseX, int mouseY, int button) {
		super.mouseDown(mouseX, mouseY, button);
		if (button == 0) startDragX = mouseX;
	}

	@Override
	public void mouseDrag(int mouseX, int mouseY, int button, long time) {
		super.mouseDrag(mouseX, mouseY, button, time);
		if (button == 0) {
			int offX = mouseX - startDragX;
			value = min + (int)Math.round(offX / stepSize);
		}
	}

	public String formatValue(int value) {
		return Integer.toString(value);
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return 12;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
}
