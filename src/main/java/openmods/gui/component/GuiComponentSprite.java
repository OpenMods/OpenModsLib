package openmods.gui.component;

import net.minecraft.client.renderer.GlStateManager;
import openmods.gui.IComponentParent;
import openmods.gui.Icon;

public class GuiComponentSprite extends BaseComponent {

	protected Icon icon;
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
		public static Icon hammer = Icon.createSheetIcon(WIDGETS, 0, 233, 23, 23);
		public static Icon plus = Icon.createSheetIcon(WIDGETS, 23, 242, 13, 13);
		public static Icon result = Icon.createSheetIcon(WIDGETS, 36, 241, 22, 15);
	}

	public GuiComponentSprite(IComponentParent parent, int x, int y, Icon icon) {
		super(parent, x, y);
		this.icon = icon;
	}

	public GuiComponentSprite setColor(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
		return this;
	}

	@Override
	public void render(int offsetX, int offsetY, int mouseX, int mouseY) {
		if (!overlay_mode) doRender(offsetX, offsetY, mouseX, mouseY);
	}

	@Override
	public void renderOverlay(int offsetX, int offsetY, int mouseX, int mouseY) {
		if (overlay_mode) doRender(offsetX, offsetY, mouseX, mouseY);
	}

	protected void doRender(int offsetX, int offsetY, int mouseX, int mouseY) {
		if (icon == null) return;

		GlStateManager.color(r, g, b);
		drawSprite(icon, offsetX + x, offsetY + y);
	}

	@Override
	public int getWidth() {
		return icon != null? icon.width : 0;
	}

	@Override
	public int getHeight() {
		return icon != null? icon.height : 0;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}
}
