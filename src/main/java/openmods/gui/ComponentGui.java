package openmods.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import openmods.gui.component.BaseComposite;

// TODO 1.14 Move to IGuiEventListener
public abstract class ComponentGui<T extends Container> extends ContainerScreen<T> {

	protected final BaseComposite root;

	public ComponentGui(T container, PlayerInventory inv, ITextComponent title, int width, int height) {
		super(container, inv, title);
		this.xSize = width;
		this.ySize = height;
		this.root = createRoot();
	}

	private IComponentParent createParent() {
		return new IComponentParent() {

			@Override
			public SoundHandler getSoundHandler() {
				return minecraft.getSoundHandler();
			}

			@Override
			public Minecraft getMinecraft() {
				return minecraft;
			}

			@Override
			public TextureAtlasSprite getIcon(ResourceLocation location) {
				return minecraft.getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE).apply(location);
			}

			@Override
			public AtlasTexture getBlocksTextureMap() {
				return minecraft.getModelManager().getAtlasTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
			}

			@Override
			public ItemRenderer getItemRenderer() {
				return itemRenderer;
			}

			@Override
			public FontRenderer getFontRenderer() {
				return font;
			}

			@Override
			public void drawItemStackTooltip(MatrixStack matrixStack, @Nonnull ItemStack stack, int x, int y) {
				ComponentGui.this.renderTooltip(matrixStack, stack, x, y);
			}

			@Override
			public void drawHoveringText(MatrixStack matrixStack, List<? extends IReorderingProcessor> textLines, int x, int y) {
				ComponentGui.this.renderTooltip(matrixStack, textLines, x, y);
			}

			@Override
			public void drawGradientRect(MatrixStack matrixStack, int left, int top, int right, int bottom, int startColor, int endColor) {
				ComponentGui.this.fillGradient(matrixStack, left, top, right, bottom, startColor, endColor);
			}

			@Override
			public void bindTexture(ResourceLocation texture) {
				minecraft.getTextureManager().bindTexture(texture);
			}
		};
	}

	@Override
	public void init() {
		super.init();
		root.init(createParent());
		minecraft.keyboardListener.enableRepeatEvents(true);
	}

	@Override
	public void onClose() {
		super.onClose();
		minecraft.keyboardListener.enableRepeatEvents(false);
	}

	protected abstract BaseComposite createRoot();

	@Override
	public void tick() {
		super.tick();

		if (root.isTicking()) {
			root.tick();
		}
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		// TODO 1.14 double!?
		if (root.isMouseOver((int)x - this.guiLeft, (int)y - this.guiTop)) {
			if (root.mouseDown((int)x - this.guiLeft, (int)y - this.guiTop, button)) {
				return true;
			}
		}
		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		if (root.isMouseOver((int)x - this.guiLeft, (int)y - this.guiTop)) {
			if (button >= 0) {
				root.mouseUp((int)x - this.guiLeft, (int)y - this.guiTop, button);
				return true;
			}
		}
		return super.mouseReleased(x, y, button);
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
		if (root.isMouseOver((int)x - this.guiLeft, (int)y - this.guiTop)) {
			root.mouseDrag((int)x - this.guiLeft, (int)y - this.guiTop, button, (int)dx, (int)dy);
		}
		return super.mouseDragged(x, y, button, dx, dy);
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if (!super.charTyped(typedChar, keyCode)) {
			root.keyTyped(typedChar, keyCode);
			return true;
		}
		return false;
	}

	public void preRender(float mouseX, float mouseY) {
	}

	public void postRender(int mouseX, int mouseY) {
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float f, int mouseX, int mouseY) {
		preRender(mouseX, mouseY);
		matrixStack.push();
		matrixStack.translate(this.guiLeft, this.guiTop, 0);
		root.render(matrixStack, 0, 0, mouseX - this.guiLeft, mouseY - this.guiTop);
		matrixStack.pop();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
		postRender(mouseX, mouseY);
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		renderOverlay(matrixStack, mouseX, mouseY);
		// TODO 1.16 what was that doing?
		// renderHoveredToolTip(mouseX, mouseY);
	}

	private void renderOverlay(MatrixStack matrixStack, int mouseX, int mouseY) {
		prepareRenderState();
		matrixStack.push();
		root.renderOverlay(matrixStack, this.guiLeft, this.guiTop, mouseX - this.guiLeft, mouseY - this.guiTop);
		matrixStack.pop();
		restoreRenderState();
	}

	protected void prepareRenderState() {
		GlStateManager.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();
	}

	protected void restoreRenderState() {
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableLighting();
		GlStateManager.disableDepthTest();
		RenderHelper.enableStandardItemLighting();
	}
}
