package openmods.gui.component;

import net.minecraft.client.Minecraft;
import openmods.gui.misc.BoxRenderer;

import org.lwjgl.input.Mouse;

public abstract class GuiComponentButton extends GuiComponentResizable {

	private static final BoxRenderer BOX_RENDERER_NORMAL = new BoxRenderer(0, 10);
	private static final BoxRenderer BOX_RENDERER_PRESSED = new BoxRenderer(20, 10);
	private static final BoxRenderer BOX_RENDERER_DISABLED = new BoxRenderer(40, 10);

	private int borderColor;

	protected boolean buttonEnabled = true;

	public GuiComponentButton(int x, int y, int width, int height, int borderColor) {
		super(x, y, width, height);
		this.borderColor = borderColor;
	}

	public void setButtonEnabled(boolean enabled) {
		this.buttonEnabled = enabled;
	}

	public boolean isButtonEnabled() {
		return buttonEnabled;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		boolean pressed = isMouseOver(mouseX, mouseY) && Mouse.isButtonDown(0);
		BoxRenderer box = buttonEnabled? (pressed? BOX_RENDERER_PRESSED : BOX_RENDERER_NORMAL) : BOX_RENDERER_DISABLED;
		bindComponentsSheet();
		box.render(this, x + offsetX, y + offsetY, width, height, borderColor);
		renderContents(minecraft, offsetX, offsetY, mouseX, mouseY, pressed);
	}

	protected abstract void renderContents(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY, boolean pressed);

}
