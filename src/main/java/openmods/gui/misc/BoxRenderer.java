package openmods.gui.misc;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.AbstractGui;
import openmods.utils.render.RenderUtils;

//TODO single tesselator call plz
public class BoxRenderer {
	private final int u;
	private final int v;

	public BoxRenderer(int u, int v) {
		this.u = u;
		this.v = v;
	}

	// 4x4 pixels starting at 0,0
	protected void renderTopLeftCorner(AbstractGui gui, MatrixStack matrixStack) {
		gui.blit(matrixStack, 0, 0, u, v, 4, 4);
	}

	// 3x3 pixels starting at 5,0
	protected void renderTopRightCorner(AbstractGui gui, MatrixStack matrixStack, int width) {
		gui.blit(matrixStack, width - 3, 0, u + 5, v, 3, 3);
	}

	// 3x3 pixels starting at 11,0
	protected void renderBottomLeftCorner(AbstractGui gui, MatrixStack matrixStack, int height) {
		gui.blit(matrixStack, 0, height - 3, u + 11, v, 3, 3);
	}

	// 4x4 pixels starting at 15,0
	protected void renderBottomRightCorner(AbstractGui gui, MatrixStack matrixStack, int width, int height) {
		gui.blit(matrixStack, width - 4, height - 4, u + 15, v, 4, 4);
	}

	// 1x3 pixels starting at 14,0
	protected void renderBottomEdge(AbstractGui gui, MatrixStack matrixStack, int width, int height) {
		matrixStack.push();
		matrixStack.translate(3, height - 3, 0);
		matrixStack.scale(width - 7, 1, 0);
		gui.blit(matrixStack, 0, 0, u + 14, v, 1, 3);
		matrixStack.pop();
	}

	// 1x3 pixels starting at 4,0
	protected void renderTopEdge(AbstractGui gui, MatrixStack matrixStack, int width) {
		matrixStack.push();
		matrixStack.translate(4, 0, 0);
		matrixStack.scale(width - 7, 1, 0);
		gui.blit(matrixStack, 0, 0, u + 4, v, 1, 3);
		matrixStack.pop();
	}

	// 3x1 pixels starting at 0,4
	protected void renderLeftEdge(AbstractGui gui, MatrixStack matrixStack, int height) {
		matrixStack.push();
		matrixStack.translate(0, 4, 0);
		matrixStack.scale(1, height - 7, 0);
		gui.blit(matrixStack, 0, 0, u, v + 4, 3, 1);
		matrixStack.pop();
	}

	// 3x1 pixels starting at 8,0
	protected void renderRightEdge(AbstractGui gui, MatrixStack matrixStack, int width, int height) {
		matrixStack.push();
		matrixStack.translate(width - 3, 3, 0);
		matrixStack.scale(1, height - 7, 0);
		gui.blit(matrixStack, 0, 0, u + 8, v, 3, 1);
		matrixStack.pop();
	}

	// 1x1 pixels starting at 19,0
	protected void renderBackground(AbstractGui gui, MatrixStack matrixStack, int width, int height) {
		matrixStack.push();
		matrixStack.translate(2, 2, 0);
		matrixStack.scale(width - 4, height - 4, 0);
		gui.blit(matrixStack, 0, 0, u + 19, v, 1, 1);
		matrixStack.pop();
	}

	public void render(AbstractGui gui, MatrixStack matrixStack, int x, int y, int width, int height, int color) {
		RenderUtils.setColor(color);

		matrixStack.push();
		matrixStack.translate(x, y, 0);
		renderBackground(gui, matrixStack, width, height);
		renderTopEdge(gui, matrixStack, width);
		renderBottomEdge(gui, matrixStack, width, height);
		renderLeftEdge(gui, matrixStack, height);
		renderRightEdge(gui, matrixStack, width, height);

		renderTopLeftCorner(gui, matrixStack);
		renderTopRightCorner(gui, matrixStack, width);
		renderBottomLeftCorner(gui, matrixStack, height);
		renderBottomRightCorner(gui, matrixStack, width, height);
		matrixStack.pop();

		GlStateManager.color4f(1, 1, 1, 1);
	}

}
