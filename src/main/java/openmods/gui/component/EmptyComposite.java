package openmods.gui.component;

import net.minecraft.client.Minecraft;

public class EmptyComposite extends BaseComposite {
	private final int width;

	private final int height;

	public EmptyComposite(int x, int y, int width, int height) {
		super(x, y);
		this.width = width;
		this.height = height;
	}

	@Override
	protected void renderComponentBackground(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}
}
