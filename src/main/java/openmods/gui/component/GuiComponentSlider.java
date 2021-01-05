package openmods.gui.component;

import com.google.common.primitives.Ints;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import joptsimple.internal.Strings;
import net.minecraft.client.gui.FontRenderer;
import openmods.api.IValueReceiver;
import openmods.gui.listener.IValueChangedListener;

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
	public void render(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		GlStateManager.color4f(1, 1, 1, 1);
		final int left = offsetX + x;
		final int top = offsetY + y;
		int barStartX = left + 1;
		bindComponentsSheet();
		blit(matrixStack, left, top, 0, 70, 1, getHeight());
		matrixStack.push();
		matrixStack.translate(left + 1, top, 0);
		matrixStack.scale(getWidth() - 2, 1, 1);
		blit(matrixStack, 0, 0, 1, 70, 1, getHeight());
		matrixStack.pop();
		blit(matrixStack, left + getWidth() - 1, top, 2, 70, 1, getHeight());

		if (!Strings.isNullOrEmpty(label)) {
			final FontRenderer fr = parent.getFontRenderer();
			int strWidth = fr.getStringWidth(label);
			fr.drawString(matrixStack, label, left + getWidth() / 2 - strWidth / 2, top + 2, 4210752);
		}

		GlStateManager.color4f(1, 1, 1, 1);
		bindComponentsSheet();
		int handleX = (int)Math.floor(barStartX + stepSize * step);
		blit(matrixStack, handleX, top + 1, 3, 70, 9, 10);
		if (showValue) {
			final double value = stepToValue(step);
			String label = formatValue(value);
			final FontRenderer fr = parent.getFontRenderer();
			int strWidth = fr.getStringWidth(label);
			fr.drawString(matrixStack, label, handleX + 4 - (strWidth / 2), top + 15, 4210752);
		}
	}

	@Override
	public boolean mouseDrag(int mouseX, int mouseY, int button, int dx, int dy) {
		boolean result = super.mouseDrag(mouseX, mouseY, button, dx, dy);
		if (button == 0) {
			int offX = mouseX - HANDLE_SIZE / 2;
			if (offX >= 0) {
				final int newStep = Ints.saturatedCast(Math.round(offX / stepSize));
				final int boundedNewStep = Math.max(0, Math.min(steps, newStep));

				if (boundedNewStep != step) {
					step = boundedNewStep;
					final double newValue = stepToValue(step);
					if (listener != null) {
						listener.valueChanged(newValue);
					}
					return true;
				}
			}
		}

		return result;
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
