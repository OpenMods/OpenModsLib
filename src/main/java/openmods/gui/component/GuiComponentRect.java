package openmods.gui.component;

import openmods.api.IValueReceiver;
import openmods.gui.IComponentParent;

public class GuiComponentRect extends GuiComponentResizable implements IValueReceiver<Integer> {

	private int color;
	private final int mask;

	public GuiComponentRect(IComponentParent parent, int x, int y, int width, int height, int color) {
		this(parent, x, y, width, height, color, 0xFF000000);
	}

	public GuiComponentRect(IComponentParent parent, int x, int y, int width, int height, int color, int mask) {
		super(parent, x, y, width, height);
		this.mask = mask;
		this.color = color | mask;
	}

	public int getColorForRender() {
		return color;
	}

	@Override
	public void render(int offsetX, int offsetY, int mouseX, int mouseY) {
		int oX = x + offsetX;
		int oY = y + offsetY;
		drawRect(oX, oY, oX + width, oY + height, getColorForRender());
	}

	@Override
	public void setValue(Integer color) {
		this.color = color | mask;
	}
}
