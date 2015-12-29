package openmods.gui.component;

import net.minecraft.client.gui.FontRenderer;
import openmods.gui.IComponentParent;

public class GuiComponentTextButton extends GuiComponentButton {

	private String text;

	public GuiComponentTextButton(IComponentParent parent, int x, int y, int width, int height, int color, String text) {
		super(parent, x, y, width, height, color);
		this.text = text;
	}

	public GuiComponentTextButton(IComponentParent parent, int x, int y, int width, int height, int color) {
		this(parent, x, y, width, height, color, "");
	}

	public GuiComponentTextButton setText(String buttonText) {
		this.text = buttonText;
		return this;
	}

	@Override
	public void renderContents(int offsetX, int offsetY, int mouseX, int mouseY, boolean pressed) {
		final FontRenderer fontRenderer = parent.getFontRenderer();
		int textWidth = fontRenderer.getStringWidth(text);
		int offX = ((width - textWidth) / 2) + 1;
		int offY = 3;
		if (buttonEnabled && pressed) {
			offY++;
			offX++;
		}
		fontRenderer.drawString(text, offsetX + x + offX, offsetY + y + offY, 4210752);
	}
}
