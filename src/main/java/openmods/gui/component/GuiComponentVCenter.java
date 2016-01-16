package openmods.gui.component;

import openmods.gui.IComponentParent;

public class GuiComponentVCenter extends BaseComposite {

	private int width;

	private int height;

	public GuiComponentVCenter(int x, int y, int height) {
		super(x, y);
		this.height = height;
	}

	@Override
	public void init(IComponentParent parent) {
		super.init(parent);

		for (BaseComponent c : components) {
			if (height >= 0) {
				int y = (height - c.getHeight()) / 2;
				c.setY(y);
			}

			width = Math.max(width, c.getX() + c.getWidth());
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
		return new GuiComponentVCenter(x, y, width).addComponent(component);
	}
}
