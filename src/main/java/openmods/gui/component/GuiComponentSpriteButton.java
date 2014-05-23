package openmods.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiComponentSpriteButton extends GuiComponentSprite {

	private final IIcon hoverIcon;

	public GuiComponentSpriteButton(int x, int y, IIcon icon, IIcon hoverIcon, ResourceLocation texture) {
		super(x, y, icon, texture);
		this.hoverIcon = hoverIcon;
	}

	@Override
	protected void doRender(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (icon == null) { return; }
		if (texture != null) minecraft.renderEngine.bindTexture(texture);
		GL11.glColor3f(r, g, b);
		boolean mouseOver = isMouseOver(mouseX, mouseY);
		boolean pressed = mouseOver && Mouse.isButtonDown(0);
		int offset = pressed? 1 : 0;
		IIcon useIcon = hoverIcon != null && mouseOver? hoverIcon : icon;
		drawTexturedModelRectFromIcon(offsetX + x + offset, offsetY + y + offset, useIcon, useIcon.getIconWidth(), useIcon.getIconHeight());
	}
}
