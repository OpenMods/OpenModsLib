package openmods.gui.component;

import openmods.gui.Icon;

public class GuiComponentIconButton extends GuiComponentButton {

	public static final int BORDER_SIZE = 4;

	private final Icon icon;

	public GuiComponentIconButton(int x, int y, int color, Icon icon) {
		super(x, y, icon.width + BORDER_SIZE, icon.height + BORDER_SIZE, color);
		this.icon = icon;
	}

	@Override
	public void renderContents(int offsetX, int offsetY, int mouseX, int mouseY, boolean pressed) {
		int offset = (buttonEnabled && pressed)? 3 : 2;

		drawSprite(icon, offsetX + x + offset, offsetY + y + offset);
	}
}
