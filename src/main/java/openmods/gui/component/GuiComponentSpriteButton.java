package openmods.gui.component;

import com.mojang.blaze3d.platform.GlStateManager;
import openmods.gui.Icon;

public class GuiComponentSpriteButton extends GuiComponentSprite {

	private final Icon hoverIcon;

	public GuiComponentSpriteButton(int x, int y, Icon icon, Icon hoverIcon) {
		super(x, y, icon);
		this.hoverIcon = hoverIcon;
	}

	@Override
	protected void doRender(int offsetX, int offsetY, int mouseX, int mouseY) {
		if (icon == null) { return; }

		GlStateManager.color3f(r, g, b);
		boolean mouseOver = isMouseOver(mouseX, mouseY);
		boolean pressed = mouseOver && parent.getMinecraft().mouseHelper.isLeftDown();
		int offset = pressed? 1 : 0;
		Icon useIcon = hoverIcon != null && mouseOver? hoverIcon : icon;
		drawSprite(useIcon, offsetX + x + offset, offsetY + y + offset);
	}
}
