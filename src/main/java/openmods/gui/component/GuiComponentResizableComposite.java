package openmods.gui.component;

import openmods.gui.IComponentParent;

public abstract class GuiComponentResizableComposite extends BaseComposite {
	protected int width;
	protected int height;

	public GuiComponentResizableComposite(IComponentParent parent, int x, int y, int width, int height) {
		super(parent, x, y);
		this.width = width;
		this.height = height;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

}
