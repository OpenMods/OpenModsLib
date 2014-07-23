package openmods.gui.component;

public abstract class GuiComponentResizable extends BaseComponent {
	protected int width;
	protected int height;

	public GuiComponentResizable(int x, int y, int width, int height) {
		super(x, y);
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
