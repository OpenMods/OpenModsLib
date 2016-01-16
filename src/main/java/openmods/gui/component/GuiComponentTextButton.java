package openmods.gui.component;

import net.minecraft.client.gui.FontRenderer;

public class GuiComponentTextButton extends GuiComponentButton {

	private String text;

	public GuiComponentTextButton(int x, int y, int width, int height, int color, String text) {
		super(x, y, width, height, color);
		this.text = text;
	}

	public GuiComponentTextButton(int x, int y, int width, int height, int color) {
		this(x, y, width, height, color, "");
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
