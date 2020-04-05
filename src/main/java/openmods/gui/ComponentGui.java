package openmods.gui;

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
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import openmods.gui.component.BaseComposite;
import org.lwjgl.opengl.GL11;

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
				return minecraft.getTextureMap().getAtlasSprite(location.toString());
			}

			@Override
			public AtlasTexture getBlocksTextureMap() {
				return minecraft.getTextureMap();
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
			public void drawItemStackTooltip(@Nonnull ItemStack stack, int x, int y) {
				ComponentGui.this.renderTooltip(stack, x, y);
			}

			@Override
			public void drawHoveringText(List<String> textLines, int x, int y) {
				ComponentGui.this.renderTooltip(textLines, x, y);
			}

			@Override
			public void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
				ComponentGui.this.fillGradient(left, top, right, bottom, startColor, endColor);
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
	public void removed() {
		super.removed();
		minecraft.keyboardListener.enableRepeatEvents(false);
	}

	protected abstract BaseComposite createRoot();

	@Override
	public void tick() {
		super.tick();

		if (root.isTicking()) root.tick();
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (!super.mouseClicked(x, y, button)) {
			// TODO 1.14 double!?
			if (root.isMouseOver((int)x - this.guiLeft, (int)y - this.guiTop)) {
				root.mouseDown((int)x - this.guiLeft, (int)y - this.guiTop, button);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		if (!super.mouseReleased(x, y, button)) {
			if (root.isMouseOver((int)x - this.guiLeft, (int)y - this.guiTop)) {
				if (button >= 0) {
					root.mouseUp((int)x - this.guiLeft, (int)y - this.guiTop, button);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
		if (!super.mouseDragged(x, y, button, dx, dy)) {
			if (root.isMouseOver((int)x - this.guiLeft, (int)y - this.guiTop)) {
				root.mouseDrag((int)x - this.guiLeft, (int)y - this.guiTop, button, (int)dx, (int)dy);
			}
		}
		return false;
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if (!super.charTyped(typedChar, keyCode)) {
			root.keyTyped(typedChar, keyCode);
			return true;
		}
		return false;
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
	public void render(int mouseX, int mouseY, float partialTicks) {
		this.renderBackground();
		super.render(mouseX, mouseY, partialTicks);
		renderOverlay(mouseX, mouseY);
		renderHoveredToolTip(mouseX, mouseY);
	}

	private void renderOverlay(int mouseX, int mouseY) {
		prepareRenderState();
		GL11.glPushMatrix();
		root.renderOverlay(this.guiLeft, this.guiTop, mouseX - this.guiLeft, mouseY - this.guiTop);
		GL11.glPopMatrix();
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
