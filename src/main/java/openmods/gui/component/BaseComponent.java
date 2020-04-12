package openmods.gui.component;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import openmods.OpenMods;
import openmods.gui.IComponentParent;
import openmods.gui.Icon;
import openmods.gui.listener.IKeyTypedListener;
import openmods.gui.listener.IMouseDownListener;
import openmods.gui.listener.IMouseDragListener;
import openmods.gui.listener.IMouseUpListener;
import org.lwjgl.opengl.GL11;

public abstract class BaseComponent extends AbstractGui {

	public final static ResourceLocation WIDGETS = OpenMods.location("textures/gui/components.png");

	protected void bindComponentsSheet() {
		parent.bindTexture(WIDGETS);
	}

	protected int x;
	protected int y;
	protected boolean enabled = true;

	private IKeyTypedListener keyListener;
	private IMouseDownListener mouseDownListener;
	private IMouseUpListener mouseUpListener;
	private IMouseDragListener mouseDragListener;

	protected IComponentParent parent;

	public BaseComponent(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void init(IComponentParent parent) {
		this.parent = parent;
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

	public void render(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {}

	public void renderOverlay(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {}

	public void keyTyped(char keyChar, int keyCode) {
		if (keyListener != null) { keyListener.componentKeyTyped(this, keyChar, keyCode); }
	}

	public void mouseDown(int mouseX, int mouseY, int button) {
		if (mouseDownListener != null) { mouseDownListener.componentMouseDown(this, mouseX, mouseY, button); }
	}

	public void mouseUp(int mouseX, int mouseY, int button) {
		if (mouseUpListener != null) { mouseUpListener.componentMouseUp(this, mouseX, mouseY, button); }
	}

	public void mouseDrag(int mouseX, int mouseY, int button, int dx, int dy) {
		if (mouseDragListener != null) { mouseDragListener.componentMouseDrag(this, mouseX, mouseY, button, dx, dy); }
	}

	public boolean isTicking() {
		return false;
	}

	public void tick() {}

	protected void drawItemStack(@Nonnull ItemStack stack, int x, int y, String overlayText) {
		if (stack.isEmpty()) { return; }

		RenderHelper.enableStandardItemLighting();
		final ItemRenderer itemRenderer = parent.getItemRenderer();
		GlStateManager.translatef(0.0F, 0.0F, 32.0F);
		setBlitOffset(200);
		itemRenderer.zLevel = 200.0F;

		FontRenderer font;
		font = stack.getItem().getFontRenderer(stack);
		if (font == null) { font = parent.getFontRenderer(); }

		itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
		itemRenderer.renderItemOverlayIntoGUI(font, stack, x, y, overlayText);
		setBlitOffset(0);
		itemRenderer.zLevel = 0.0F;
		RenderHelper.disableStandardItemLighting();
	}

	protected void drawItemStack(@Nonnull ItemStack stack, int x, int y) {
		if (stack.isEmpty()) { return; }

		RenderHelper.enableStandardItemLighting();
		final ItemRenderer itemRenderer = parent.getItemRenderer();
		GlStateManager.translated(0.0F, 0.0F, 32.0F);
		setBlitOffset(200);
		itemRenderer.zLevel = 200.0F;

		itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
		setBlitOffset(0);
		itemRenderer.zLevel = 0.0F;
		RenderHelper.disableStandardItemLighting();
	}

	protected void drawSprite(Icon icon, final MatrixStack matrixStack, int x, int y, int width, int height) {
		parent.bindTexture(icon.texture);

		// TODO 1.16 blit?
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder worldrenderer = tessellator.getBuffer();
		Matrix4f m = matrixStack.getLast().getMatrix();
		worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		worldrenderer.pos(m, x + 0, y + height, getBlitOffset()).tex(icon.minU, icon.maxV).endVertex();
		worldrenderer.pos(m, x + width, y + height, getBlitOffset()).tex(icon.maxU, icon.maxV).endVertex();
		worldrenderer.pos(m, x + width, y + 0, getBlitOffset()).tex(icon.maxU, icon.minV).endVertex();
		worldrenderer.pos(m, x + 0, y + 0, getBlitOffset()).tex(icon.minU, icon.minV).endVertex();
		tessellator.draw();
	}

	protected void drawSprite(Icon icon, final MatrixStack matrixStack, int x, int y) {
		drawSprite(icon, matrixStack, x, y, icon.width, icon.height);
	}

	protected void drawHoveringText(MatrixStack matrixStack, List<? extends IReorderingProcessor> textLines, int x, int y) {
		parent.drawHoveringText(matrixStack, textLines, x, y);
	}

	protected void drawHoveringText(MatrixStack matrixStack, IReorderingProcessor line, int x, int y) {
		parent.drawHoveringText(matrixStack, ImmutableList.of(line), x, y);
	}
}
