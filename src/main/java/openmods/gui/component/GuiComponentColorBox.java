package openmods.gui.component;

public class GuiComponentColorBox extends GuiComponentRect {

	private int color;

	public GuiComponentColorBox(int x, int y, int width, int height, int color) {
		super(x, y, width, height, 0xFF000000);
		this.color = color;
	}

	@Override
	public int getColorForRender() {
		return color | (0xFF << 24);
	}

	public void setColor(int color) {
		this.color = color;
	}
}
