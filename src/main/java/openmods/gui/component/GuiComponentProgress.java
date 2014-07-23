package openmods.gui.component;

import net.minecraft.client.Minecraft;
import openmods.api.IValueReceiver;

import org.lwjgl.opengl.GL11;

public class GuiComponentProgress extends BaseComponent {

	private int progress;
	private float scale;

	public GuiComponentProgress(int x, int y, int maxProgress) {
		super(x, y);
		setMaxProgress(maxProgress);
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		bindComponentsSheet();
		GL11.glColor3f(1, 1, 1);
		drawTexturedModalRect(offsetX + x, offsetY + y, 0, 38, getWidth(), getHeight());
		int pxProgress = Math.round(progress * scale);
		drawTexturedModalRect(offsetX + x, offsetY + y, 0, 50, pxProgress, getHeight());
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

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
		return new IValueReceiver<Integer>() {
			@Override
			public void setValue(Integer value) {
				progress = value;
			}
		};
	}

	public IValueReceiver<Integer> maxProgressReceiver() {
		return new IValueReceiver<Integer>() {
			@Override
			public void setValue(Integer value) {
				setMaxProgress(value);
			}
		};
	}
}
