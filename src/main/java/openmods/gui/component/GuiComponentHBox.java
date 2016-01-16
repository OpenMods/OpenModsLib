package openmods.gui.component;

import openmods.gui.IComponentParent;

public class GuiComponentHBox extends BaseComposite {

	private int height;

	private int width;

	public GuiComponentHBox(int x, int y) {
		super(x, y);
	}

	@Override
	public void init(IComponentParent parent) {
		super.init(parent);

		int currentX = 0;
		for (BaseComponent c : components) {
			c.setX(currentX);
			currentX += c.getWidth();
			height = Math.max(height, c.getY() + c.getHeight());
		}

		width = currentX;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

}
