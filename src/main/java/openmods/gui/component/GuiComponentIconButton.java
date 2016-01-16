package openmods.gui.component;

import openmods.gui.Icon;

public class GuiComponentIconButton extends GuiComponentButton {

	private final Icon icon;

	public GuiComponentIconButton(int x, int y, int color, Icon icon) {
		super(x, y, icon.width + 4, icon.height + 4, color);
		this.icon = icon;
	}

	@Override
	public void renderContents(int offsetX, int offsetY, int mouseX, int mouseY, boolean pressed) {
		int offset = (buttonEnabled && pressed)? 3 : 2;

		drawSprite(icon, offsetX + x + offset, offsetY + y + offset);
	}
}
