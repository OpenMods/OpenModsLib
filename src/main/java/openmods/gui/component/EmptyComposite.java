package openmods.gui.component;

import openmods.gui.IComponentParent;

public class EmptyComposite extends BaseComposite {
	private final int width;

	private final int height;

	public EmptyComposite(IComponentParent parent, int x, int y, int width, int height) {
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
}
