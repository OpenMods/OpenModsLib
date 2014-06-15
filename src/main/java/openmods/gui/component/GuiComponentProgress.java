package openmods.gui.component;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

public class GuiComponentProgress extends BaseComponent {

	private float progress;

	public GuiComponentProgress(int x, int y, float progress) {
		super(x, y);
		this.progress = progress;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		super.render(minecraft, offsetX, offsetY, mouseX, mouseY);
		bindComponentsSheet();
		GL11.glColor3f(1, 1, 1);
		drawTexturedModalRect(offsetX + x, offsetY + y, 0, 38, getWidth(), getHeight());
		int pxProgress = Math.round(getWidth() * progress);
		drawTexturedModalRect(offsetX + x, offsetY + y, 0, 50, pxProgress, getHeight());
	}

	@Override
	public int getWidth() {
		return 29;
	}

	@Override
	public int getHeight() {
		return 12;
	}

	public void setProgress(float progress) {
		this.progress = progress;
	}
}
