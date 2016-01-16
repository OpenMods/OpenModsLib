package openmods.gui.component;

import openmods.gui.IComponentParent;

public class GuiComponentHCenter extends BaseComposite {

	private int width;

	private int height;

	public GuiComponentHCenter(int x, int y, int width) {
		super(x, y);
		this.width = width;
	}

	@Override
	public void init(IComponentParent parent) {
		super.init(parent);

		for (BaseComponent c : components) {
			if (width >= 0) {
				int x = (width - c.getWidth()) / 2;
				c.setX(x);
			}

			height = Math.max(height, c.getY() + c.getHeight());
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	public static BaseComposite wrap(int x, int y, int width, BaseComponent component) {
		return new GuiComponentHCenter(x, y, width).addComponent(component);
	}
}
