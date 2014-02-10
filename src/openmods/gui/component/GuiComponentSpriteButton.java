package openmods.gui.component;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Icon;
import net.minecraft.util.ResourceLocation;

public class GuiComponentSpriteButton extends GuiComponentSprite {

	private final Icon hoverIcon;

	public GuiComponentSpriteButton(int x, int y, Icon icon, Icon hoverIcon, ResourceLocation texture) {
		super(x, y, icon, texture);
		this.hoverIcon = hoverIcon;
	}

	protected void doRender(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (icon == null) { return; }
		if (texture != null) minecraft.renderEngine.bindTexture(texture);
		GL11.glColor3f(r, g, b);
		boolean mouseOver = isMouseOver(mouseX, mouseY);
		boolean pressed = mouseOver && Mouse.isButtonDown(0);
		int offset = pressed? 1 : 0;
		Icon useIcon = hoverIcon != null && mouseOver? hoverIcon : icon;
		drawTexturedModelRectFromIcon(offsetX + x + offset, offsetY + y + offset, useIcon, useIcon.getIconWidth(), useIcon.getIconHeight());
	}
}
