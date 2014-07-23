package openmods.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

public class GuiComponentIconButton extends GuiComponentButton {

	private ResourceLocation texture;
	private final IIcon icon;

	public GuiComponentIconButton(int x, int y, int color, IIcon icon) {
		super(x, y, icon.getIconWidth() + 4, icon.getIconHeight() + 4, color);
		this.icon = icon;
	}

	public GuiComponentIconButton(int x, int y, int color, IIcon icon, ResourceLocation texture) {
		this(x, y, color, icon);
		this.texture = texture;
	}

	@Override
	public void renderContents(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY, boolean pressed) {
		if (texture != null) minecraft.renderEngine.bindTexture(texture);

		int offset = (buttonEnabled && pressed)? 3 : 2;

		drawTexturedModelRectFromIcon(offsetX + x + offset, offsetY + y + offset, icon, icon.getIconWidth(), icon.getIconHeight());
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}
}
