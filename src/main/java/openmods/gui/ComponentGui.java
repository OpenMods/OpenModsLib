package openmods.gui;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import openmods.gui.component.BaseComposite;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.base.Preconditions;

public abstract class ComponentGui extends GuiContainer {

	protected final BaseComposite root;

	protected final IComponentParent componentParent;

	public ComponentGui(Container container, int width, int height) {
		super(container);
		this.xSize = width;
		this.ySize = height;
		this.componentParent = createParent();
		this.root = createRoot(componentParent);
	}

	private IComponentParent createParent() {
		return new IComponentParent() {

			@Override
			public SoundHandler getSoundHandler() {
				return mc.getSoundHandler();
			}

			@Override
			public Minecraft getMinecraft() {
				return mc;
			}

			@Override
			public TextureMap getBlocksTextureMap() {
				return mc.getTextureMapBlocks();
			}

			@Override
			public RenderItem getItemRenderer() {
				return itemRender;
			}

			@Override
			public FontRenderer getFontRenderer() {
				return fontRendererObj;
			}

			@Override
			public void drawItemStackTooltip(ItemStack stack, int x, int y) {
				ComponentGui.this.renderToolTip(stack, x, y);
			}

			@Override
			public void drawHoveringText(List<String> textLines, int x, int y) {
				ComponentGui.this.drawHoveringText(textLines, x, y);
			}

			@Override
			public void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
				ComponentGui.this.drawGradientRect(left, top, right, bottom, startColor, endColor);
			}

			private ResourceLocation currentTexture = null;

			@Override
			public void bindTexture(ResourceLocation texture) {
				Preconditions.checkNotNull(texture);
				if (this.currentTexture == null || this.currentTexture.equals(texture)) {
					mc.renderEngine.bindTexture(texture);
					this.currentTexture = texture;
				}
			}
		};
	}

	protected abstract BaseComposite createRoot(IComponentParent parent);

	@Override
	public void updateScreen() {
		super.updateScreen();

		if (root.isTicking()) root.tick();
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		if (root.isMouseOver(x - this.guiLeft, y - this.guiTop)) root.mouseDown(x - this.guiLeft, y - this.guiTop, button);
	}

	@Override
	protected void mouseReleased(int x, int y, int button) {
		super.mouseReleased(x, y, button);
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
	protected void keyTyped(char par1, int par2) throws IOException {
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
		root.render(0, 0, mouseX - this.guiLeft, mouseY - this.guiTop);
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
		root.renderOverlay(this.guiLeft, this.guiTop, par1 - this.guiLeft, par2 - this.guiTop);
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
