package openmods.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import openmods.utils.render.FakeIcon;

import org.lwjgl.opengl.GL11;

public class GuiComponentSprite extends BaseComponent {

	protected IIcon icon;
	protected ResourceLocation texture;
	protected float r = 1, g = 1, b = 1;
	protected boolean overlay_mode;

	public boolean isOverlay() {
		return overlay_mode;
	}

	public BaseComponent setOverlayMode(boolean isOverlay) {
		this.overlay_mode = isOverlay;
		return this;
	}

	public static class Sprites {
		public static IIcon hammer = FakeIcon.createSheetIcon(0, 233, 23, 23);
		public static IIcon plus = FakeIcon.createSheetIcon(23, 242, 13, 13);
		public static IIcon result = FakeIcon.createSheetIcon(36, 241, 22, 15);
	}

	public GuiComponentSprite(int x, int y, IIcon icon, ResourceLocation texture) {
		super(x, y);
		this.texture = texture;
		this.icon = icon;
	}

	public GuiComponentSprite setColor(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
		return this;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (!overlay_mode) doRender(minecraft, offsetX, offsetY, mouseX, mouseY);
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (overlay_mode) doRender(minecraft, offsetX, offsetY, mouseX, mouseY);
	}

	protected void doRender(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (icon == null) return;
		if (texture != null) minecraft.renderEngine.bindTexture(texture);
		GL11.glColor3f(r, g, b);
		drawTexturedModelRectFromIcon(offsetX + x, offsetY + y, icon, icon.getIconWidth(), icon.getIconHeight());
	}

	@Override
	public int getWidth() {
		return icon != null? icon.getIconWidth() : 0;
	}

	@Override
	public int getHeight() {
		return icon != null? icon.getIconHeight() : 0;
	}

	public void setIcon(IIcon icon) {
		this.icon = icon;
	}
}
