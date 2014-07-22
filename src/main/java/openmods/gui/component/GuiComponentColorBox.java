package openmods.gui.component;

import openmods.api.IValueReceiver;

public class GuiComponentColorBox extends GuiComponentRect implements IValueReceiver<Integer> {

	private int color;

	public GuiComponentColorBox(int x, int y, int width, int height, int color) {
		super(x, y, width, height, 0xFF000000);
		this.color = color;
	}

	@Override
	public int getColorForRender() {
		return color | (0xFF << 24);
	}

	@Override
	public void setValue(Integer color) {
		this.color = color;
	}
}
