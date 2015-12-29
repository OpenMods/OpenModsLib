package openmods.gui.component;

import openmods.gui.IComponentParent;
import openmods.gui.Icon;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiComponentSpriteButton extends GuiComponentSprite {

	private final Icon hoverIcon;

	public GuiComponentSpriteButton(IComponentParent parent, int x, int y, Icon icon, Icon hoverIcon) {
		super(parent, x, y, icon);
		this.hoverIcon = hoverIcon;
	}

	@Override
	protected void doRender(int offsetX, int offsetY, int mouseX, int mouseY) {
		if (icon == null) { return; }

		GL11.glColor3f(r, g, b);
		boolean mouseOver = isMouseOver(mouseX, mouseY);
		boolean pressed = mouseOver && Mouse.isButtonDown(0);
		int offset = pressed? 1 : 0;
		Icon useIcon = hoverIcon != null && mouseOver? hoverIcon : icon;
		drawSprite(useIcon, offsetX + x + offset, offsetY + y + offset);
	}
}
