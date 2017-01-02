package openmods.gui.component;

import net.minecraft.client.renderer.GlStateManager;
import openmods.gui.Icon;
import org.lwjgl.input.Mouse;

public class GuiComponentSpriteButton extends GuiComponentSprite {

	private final Icon hoverIcon;

	public GuiComponentSpriteButton(int x, int y, Icon icon, Icon hoverIcon) {
		super(x, y, icon);
		this.hoverIcon = hoverIcon;
	}

	@Override
	protected void doRender(int offsetX, int offsetY, int mouseX, int mouseY) {
		if (icon == null) { return; }

		GlStateManager.color(r, g, b);
		boolean mouseOver = isMouseOver(mouseX, mouseY);
		boolean pressed = mouseOver && Mouse.isButtonDown(0);
		int offset = pressed? 1 : 0;
		Icon useIcon = hoverIcon != null && mouseOver? hoverIcon : icon;
		drawSprite(useIcon, offsetX + x + offset, offsetY + y + offset);
	}
}
