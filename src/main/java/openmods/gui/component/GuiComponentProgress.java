package openmods.gui.component;

import net.minecraft.client.renderer.GlStateManager;
import openmods.api.IValueReceiver;

public class GuiComponentProgress extends BaseComponent {

	private int progress;
	private float scale;

	public GuiComponentProgress(int x, int y, int maxProgress) {
		super(x, y);
		setMaxProgress(maxProgress);
	}

	@Override
	public void render(int offsetX, int offsetY, int mouseX, int mouseY) {
		bindComponentsSheet();
		GlStateManager.color(1, 1, 1);
		drawTexturedModalRect(offsetX + x, offsetY + y, 0, 38, getWidth(), getHeight());
		int pxProgress = Math.round(progress * scale);
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

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setMaxProgress(int maxProgress) {
		this.scale = (float)getWidth() / maxProgress;
	}

	public IValueReceiver<Integer> progressReceiver() {
		return value -> progress = value;
	}

	public IValueReceiver<Integer> maxProgressReceiver() {
		return this::setMaxProgress;
	}
}
