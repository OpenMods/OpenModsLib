package openmods.gui.component;

import net.minecraft.client.Minecraft;
import openmods.api.IValueReceiver;

public class GuiComponentLevel extends BaseComponent implements IValueReceiver<Float> {
	private int width;
	private int height;
	private int fColor;
	private int bColor;
	private float value;
	private float min, max;

	public GuiComponentLevel(int x, int y, int width, int height, int levelColor, int backgroundColor, float min, float max, float value) {
		super(x, y);
		this.width = width;
		this.height = height;
		this.fColor = levelColor;
		this.bColor = backgroundColor;
		this.min = min;
		this.max = max;
		this.value = value;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setValue(Float v) {
		this.value = v;
	}

	public float getValue() {
		return value;
	}

	private float getFillHeight() {
		float value = getValue();
		if (value > max) value = max;
		if (value < min) value = min;
		float percent = value / max;
		return percent * getHeight();
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		int oX = x + offsetX;
		int oY = y + offsetY;
		// Fill with background
		drawRect(oX, oY, oX + width, oY + height, bColor);
		// Draw level
		drawRect(oX, oY + (height - (int)getFillHeight()), oX + width, oY + height, fColor);
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}
}
