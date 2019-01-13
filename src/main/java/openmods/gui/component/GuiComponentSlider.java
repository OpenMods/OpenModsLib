package openmods.gui.component;

import com.google.common.primitives.Ints;
import joptsimple.internal.Strings;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import openmods.api.IValueReceiver;
import openmods.gui.listener.IValueChangedListener;
import org.lwjgl.opengl.GL11;

public class GuiComponentSlider extends BaseComponent implements IValueReceiver<Double> {

	private static final int HANDLE_SIZE = 8;

	private final int width;
	private final double min;
	private final double max;
	private final int steps;
	private final double stepSize;
	private final boolean showValue;
	private final String label;

	private int step;

	private IValueChangedListener<Double> listener;

	public GuiComponentSlider(int x, int y, int width, int min, int max, int initialValue) {
		this(x, y, width, min, max, initialValue, max - min, true, "");
	}

	public GuiComponentSlider(int x, int y, int width, int min, int max, int initialValue, boolean showValue) {
		this(x, y, width, min, max, initialValue, max - min, showValue, "");
	}

	public GuiComponentSlider(int x, int y, int width, int min, int max, int initialValue, boolean showValue, String label) {
		this(x, y, width, min, max, initialValue, max - min, showValue, label);
	}

	public GuiComponentSlider(int x, int y, int width, double min, double max, double initialValue, int steps, boolean showValue, String label) {
		super(x, y);
		this.width = width;
		this.min = min;
		this.max = max;
		this.steps = steps;
		this.showValue = showValue;
		this.label = label;

		this.step = (int)valueToStep(initialValue);
		this.stepSize = (double)(width - 2 * HANDLE_SIZE / 2 - 2) / (double)steps;
	}

	private double valueToStep(double value) {
		return steps * (value - min) / (max - min);
	}

	private double stepToValue(double step) {
		return (step / this.steps) * (max - min) + min;
	}

	@Override
	public void render(int offsetX, int offsetY, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1, 1);
		final int left = offsetX + x;
		final int top = offsetY + y;
		int barStartX = left + 1;
		bindComponentsSheet();
		drawTexturedModalRect(left, top, 0, 70, 1, getHeight());
		GL11.glPushMatrix();
		GL11.glTranslated(left + 1, top, 0);
		GL11.glScaled(getWidth() - 2, 1, 1);
		drawTexturedModalRect(0, 0, 1, 70, 1, getHeight());
		GL11.glPopMatrix();
		drawTexturedModalRect(left + getWidth() - 1, top, 2, 70, 1, getHeight());

		if (!Strings.isNullOrEmpty(label)) {
			final FontRenderer fr = parent.getFontRenderer();
			int strWidth = fr.getStringWidth(label);
			fr.drawString(label, left + getWidth() / 2 - strWidth / 2, top + 2, 4210752);
		}

		GlStateManager.color(1, 1, 1, 1);
		bindComponentsSheet();
		int handleX = (int)Math.floor(barStartX + stepSize * step);
		drawTexturedModalRect(handleX, top + 1, 3, 70, 9, 10);
		if (showValue) {
			final double value = stepToValue(step);
			String label = formatValue(value);
			final FontRenderer fr = parent.getFontRenderer();
			int strWidth = fr.getStringWidth(label);
			fr.drawString(label, handleX + 4 - (strWidth / 2), top + 15, 4210752);
		}
	}

	@Override
	public void mouseDrag(int mouseX, int mouseY, int button, long time) {
		super.mouseDrag(mouseX, mouseY, button, time);
		if (button == 0) {
			int offX = mouseX - HANDLE_SIZE / 2;
			if (offX < 0) return;
			final int newStep = Ints.saturatedCast(Math.round(offX / stepSize));
			final int boundedNewStep = Math.max(0, Math.min(steps, newStep));

			if (boundedNewStep != step) {
				step = boundedNewStep;
				final double newValue = stepToValue(step);
				if (listener != null) listener.valueChanged(newValue);
			}
		}
	}

	protected String formatValue(double value) {
		return String.format("%.2f", value);
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return 12;
	}

	public double getValue() {
		return stepToValue(step);
	}

	@Override
	public void setValue(Double value) {
		this.step = (int)valueToStep(value);
	}

	public void setListener(IValueChangedListener<Double> listener) {
		this.listener = listener;
	}
}
