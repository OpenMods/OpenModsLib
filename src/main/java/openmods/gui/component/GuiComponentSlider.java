package openmods.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import openmods.api.IValueReceiver;
import openmods.gui.listener.IValueChangedListener;

import org.lwjgl.opengl.GL11;

public class GuiComponentSlider extends BaseComponent implements IValueReceiver<Integer> {

	private static final int HANDLE_SIZE = 8;

	private int width;
	private int min;
	private int max;
	private int value;
	private double stepSize;
	private boolean showValue = true;

	private IValueChangedListener<Integer> listener;

	public GuiComponentSlider(int x, int y, int width, int min, int max, int initialValue, boolean showValue) {
		this(x, y, width, min, max, initialValue);
		this.showValue = showValue;
	}

	public GuiComponentSlider(int x, int y, int width, int min, int max, int initialValue) {
		super(x, y);
		this.width = width;
		this.min = min;
		this.max = max;
		this.value = initialValue;

		int steps = max - min + 1;
		this.stepSize = (double)(width - 2 * HANDLE_SIZE / 2 - 2) / (double)steps;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
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
		int handleX = (int)Math.floor(barStartX + stepSize * (value - min + 1));

		drawTexturedModalRect(handleX, top + 1, 3, 70, 9, 10);
		if (showValue) {
			String label = formatValue(value);
			int strWidth = minecraft.fontRenderer.getStringWidth(label);
			minecraft.fontRenderer.drawString(label, handleX + 4 - (strWidth / 2), top + 15, 4210752);
		}
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

	@Override
	public void mouseDrag(int mouseX, int mouseY, int button, long time) {
		super.mouseDrag(mouseX, mouseY, button, time);
		if (button == 0) {
			int offX = mouseX - HANDLE_SIZE / 2;
			if (offX < 0) return;
			final int newValue = min + MathHelper.floor_double(offX / stepSize);
			final int boundedValue = Math.max(min, Math.min(max, newValue));

			if (boundedValue != value) {
				value = boundedValue;
				if (listener != null) listener.valueChanged(value);
			}
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

	@Override
	public void setValue(Integer value) {
		this.value = value;
	}

	public void setListener(IValueChangedListener<Integer> listener) {
		this.listener = listener;
	}
}
