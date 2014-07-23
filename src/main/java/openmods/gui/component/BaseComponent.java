package openmods.gui.component;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import openmods.gui.listener.*;
import openmods.utils.TextureUtils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public abstract class BaseComponent extends Gui {

	private static final int CRAZY_1 = 0x505000FF;
	private static final int CRAZY_2 = (CRAZY_1 & 0xFEFEFE) >> 1 | CRAZY_1 & -0xFF000000;
	private static final int CRAZY_3 = 0xF0100010;

	public final static ResourceLocation TEXTURE_SHEET = new ResourceLocation("openmodslib", "textures/gui/components.png");

	public static void bindComponentsSheet() {
		TextureUtils.bindTextureToClient(TEXTURE_SHEET);
	}

	protected int x;
	protected int y;
	protected boolean enabled = true;

	private IKeyTypedListener keyListener;
	private IMouseDownListener mouseDownListener;
	private IMouseUpListener mouseUpListener;
	private IMouseDragListener mouseDragListener;

	public BaseComponent(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public abstract int getWidth();

	public abstract int getHeight();

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isMouseOver(int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + getWidth() && mouseY >= y && mouseY < y + getHeight();
	}

	public void setListener(IKeyTypedListener keyListener) {
		this.keyListener = keyListener;
	}

	public void setListener(IMouseDownListener mouseDownListener) {
		this.mouseDownListener = mouseDownListener;
	}

	public void setListener(IMouseUpListener mouseUpListener) {
		this.mouseUpListener = mouseUpListener;
	}

	public void setListener(IMouseDragListener mouseDragListener) {
		this.mouseDragListener = mouseDragListener;
	}

	public abstract void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY);

	public abstract void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY);

	public void keyTyped(char keyChar, int keyCode) {
		if (keyListener != null) keyListener.componentKeyTyped(this, keyChar, keyCode);
	}

	public void mouseDown(int mouseX, int mouseY, int button) {
		if (mouseDownListener != null) mouseDownListener.componentMouseDown(this, mouseX, mouseY, button);
	}

	public void mouseUp(int mouseX, int mouseY, int button) {
		if (mouseUpListener != null) mouseUpListener.componentMouseUp(this, mouseX, mouseY, button);
	}

	public void mouseDrag(int mouseX, int mouseY, int button, /* love you */long time) {
		if (mouseDragListener != null) mouseDragListener.componentMouseDrag(this, mouseX, mouseY, button, time);
	}

	protected void drawHoveringText(List<String> lines, int x, int y, FontRenderer font) {
		final int lineCount = lines.size();
		if (lineCount == 0) return;

		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		int width = 0;

		for (String s : lines) {
			int l = font.getStringWidth(s);
			if (l > width) width = l;
		}

		final int left = x + 12;
		int top = y - 12;

		int height = 8;
		if (lineCount > 1) height += 2 + (lineCount - 1) * 10;

		this.zLevel = 350.0F;

		drawGradientRect(left - 3, top - 4, left + width + 3, top - 3, CRAZY_3, CRAZY_3);
		drawGradientRect(left - 3, top + height + 3, left + width + 3, top + height + 4, CRAZY_3, CRAZY_3);
		drawGradientRect(left - 3, top - 3, left + width + 3, top + height + 3, CRAZY_3, CRAZY_3);
		drawGradientRect(left - 4, top - 3, left - 3, top + height + 3, CRAZY_3, CRAZY_3);
		drawGradientRect(left + width + 3, top - 3, left + width + 4, top + height + 3, CRAZY_3, CRAZY_3);

		drawGradientRect(left - 3, top - 3 + 1, left - 3 + 1, top + height + 3 - 1, CRAZY_1, CRAZY_2);
		drawGradientRect(left + width + 2, top - 3 + 1, left + width + 3, top + height + 3 - 1, CRAZY_1, CRAZY_2);
		drawGradientRect(left - 3, top - 3, left + width + 3, top - 3 + 1, CRAZY_1, CRAZY_1);
		drawGradientRect(left - 3, top + height + 2, left + width + 3, top + height + 3, CRAZY_2, CRAZY_2);

		for (int i = 0; i < lineCount; ++i) {
			String line = lines.get(i);
			font.drawStringWithShadow(line, left, top, -1);
			if (i == 0) top += 2;
			top += 10;
		}

		this.zLevel = 0.0F;
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
	}
}
