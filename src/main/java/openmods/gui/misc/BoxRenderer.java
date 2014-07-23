package openmods.gui.misc;

import net.minecraft.client.gui.Gui;

import org.lwjgl.opengl.GL11;

public class BoxRenderer {
	private final int u;
	private final int v;

	public BoxRenderer(int u, int v) {
		this.u = u;
		this.v = v;
	}

	// 4x4 pixels starting at 0,0
	protected void renderTopLeftCorner(Gui gui) {
		gui.drawTexturedModalRect(0, 0, u, v, 4, 4);
	}

	// 3x3 pixels starting at 5,0
	protected void renderTopRightCorner(Gui gui, int width) {
		gui.drawTexturedModalRect(width - 3, 0, u + 5, v, 3, 3);
	}

	// 3x3 pixels starting at 11,0
	protected void renderBottomLeftCorner(Gui gui, int height) {
		gui.drawTexturedModalRect(0, height - 3, u + 11, v, 3, 3);
	}

	// 4x4 pixels starting at 15,0
	protected void renderBottomRightCorner(Gui gui, int width, int height) {
		gui.drawTexturedModalRect(width - 4, height - 4, u + 15, v, 4, 4);
	}

	// 1x3 pixels starting at 14,0
	protected void renderBottomEdge(Gui gui, int width, int height) {
		GL11.glPushMatrix();
		GL11.glTranslatef(3, height - 3, 0);
		GL11.glScaled(width - 7, 1, 0);
		gui.drawTexturedModalRect(0, 0, u + 14, v, 1, 3);
		GL11.glPopMatrix();
	}

	// 1x3 pixels starting at 4,0
	protected void renderTopEdge(Gui gui, int width) {
		GL11.glPushMatrix();
		GL11.glTranslatef(4, 0, 0);
		GL11.glScaled(width - 7, 1, 0);
		gui.drawTexturedModalRect(0, 0, u + 4, v, 1, 3);
		GL11.glPopMatrix();
	}

	// 3x1 pixels starting at 0,4
	protected void renderLeftEdge(Gui gui, int height) {
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 4, 0);
		GL11.glScaled(1, height - 7, 0);
		gui.drawTexturedModalRect(0, 0, u, v + 4, 3, 1);
		GL11.glPopMatrix();
	}

	// 3x1 pixels starting at 8,0
	protected void renderRightEdge(Gui gui, int width, int height) {
		GL11.glPushMatrix();
		GL11.glTranslatef(width - 3, 3, 0);
		GL11.glScaled(1, height - 7, 0);
		gui.drawTexturedModalRect(0, 0, u + 8, v, 3, 1);
		GL11.glPopMatrix();
	}

	// 1x1 pixels starting at 19,0
	protected void renderBackground(Gui gui, int width, int height) {
		GL11.glPushMatrix();
		GL11.glTranslatef(2, 2, 0);
		GL11.glScalef(width - 4, height - 4, 0);
		gui.drawTexturedModalRect(0, 0, u + 19, v, 1, 1);
		GL11.glPopMatrix();
	}

	public void render(Gui gui, int x, int y, int width, int height, int color) {
		setColor(color);

		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 0);
		renderBackground(gui, width, height);
		renderTopEdge(gui, width);
		renderBottomEdge(gui, width, height);
		renderLeftEdge(gui, height);
		renderRightEdge(gui, width, height);

		renderTopLeftCorner(gui);
		renderTopRightCorner(gui, width);
		renderBottomLeftCorner(gui, height);
		renderBottomRightCorner(gui, width, height);
		GL11.glPopMatrix();
	}

	private static void setColor(int color) {
		byte r = (byte)(color >> 16);
		byte g = (byte)(color >> 8);
		byte b = (byte)(color);
		GL11.glColor3ub(r, g, b);
	}
}
