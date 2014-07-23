package openmods.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Container;
import openmods.gui.component.BaseComposite;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public abstract class ComponentGui extends GuiContainer {

	protected final BaseComposite root;

	public ComponentGui(Container container, int width, int height) {
		super(container);
		this.xSize = width;
		this.ySize = height;
		root = createRoot();
	}

	protected abstract BaseComposite createRoot();

	@Override
	protected void mouseClicked(int x, int y, int button) {
		super.mouseClicked(x, y, button);
		if (root.isMouseOver(x - this.guiLeft, y - this.guiTop)) root.mouseDown(x - this.guiLeft, y - this.guiTop, button);
	}

	@Override
	protected void mouseMovedOrUp(int x, int y, int button) {
		super.mouseMovedOrUp(x, y, button);
		if (root.isMouseOver(x - this.guiLeft, y - this.guiTop)) {
			if (button >= 0) root.mouseUp(x - this.guiLeft, y - this.guiTop, button);
		}
	}

	@Override
	protected void mouseClickMove(int x, int y, int button, long time) {
		super.mouseClickMove(x, y, button, time);
		if (root.isMouseOver(x - this.guiLeft, y - this.guiTop)) root.mouseDrag(x - this.guiLeft, y - this.guiTop, button, time);
	}

	@Override
	protected void keyTyped(char par1, int par2) {
		super.keyTyped(par1, par2);
		root.keyTyped(par1, par2);
	}

	public void preRender(float mouseX, float mouseY) {}

	public void postRender(int mouseX, int mouseY) {}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		preRender(mouseX, mouseY);
		GL11.glPushMatrix();
		GL11.glTranslated(this.guiLeft, this.guiTop, 0);
		root.render(this.mc, 0, 0, mouseX - this.guiLeft, mouseY - this.guiTop);
		GL11.glPopMatrix();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		postRender(mouseX, mouseY);
	}

	@Override
	public void drawScreen(int par1, int par2, float par3) {
		super.drawScreen(par1, par2, par3);
		prepareRenderState();
		GL11.glPushMatrix();
		root.renderOverlay(this.mc, this.guiLeft, this.guiTop, par1 - this.guiLeft, par2 - this.guiTop);
		GL11.glPopMatrix();
		restoreRenderState();
	}

	protected void prepareRenderState() {
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}

	protected void restoreRenderState() {
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		RenderHelper.enableStandardItemLighting();
	}
}
