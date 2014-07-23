package openmods.gui.component;

import net.minecraft.client.Minecraft;
import openmods.api.IValueReceiver;

public class GuiComponentRect extends GuiComponentResizable implements IValueReceiver<Integer> {

	private int color;
	private final int mask;

	public GuiComponentRect(int x, int y, int width, int height, int color) {
		this(x, y, width, height, color, 0xFF000000);
	}

	public GuiComponentRect(int x, int y, int width, int height, int color, int mask) {
		super(x, y, width, height);
		this.mask = mask;
		this.color = color | mask;
	}

	public int getColorForRender() {
		return color;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		int oX = x + offsetX;
		int oY = y + offsetY;
		drawRect(oX, oY, oX + width, oY + height, getColorForRender());
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

	@Override
	public void setValue(Integer color) {
		this.color = color | mask;
	}
}
