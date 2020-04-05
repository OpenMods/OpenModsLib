package openmods.gui.component;

import com.mojang.blaze3d.platform.GlStateManager;
import javax.annotation.Nonnull;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class GuiComponentItemStackSpinner extends BaseComponent {

	@Nonnull
	private final ItemStack stack;
	private float rotationY = 0f;

	public GuiComponentItemStackSpinner(int x, int y, @Nonnull ItemStack stack) {
		super(x, y);
		this.stack = stack;
	}

	@Override
	public int getWidth() {
		return 64;
	}

	@Override
	public int getHeight() {
		return 64;
	}

	@Override
	public void renderOverlay(int offsetX, int offsetY, int mouseX, int mouseY) {
		GL11.glPushMatrix();
		GlStateManager.enableDepthTest();
		float scale = 30.0f;
		GL11.glTranslated(offsetX + x + (scale / 2), offsetY + y + (scale / 2), scale);
		GL11.glScaled(scale, -scale, scale);
		rotationY += 0.6f;
		GL11.glRotatef(20, 1, 0, 0);
		GL11.glRotatef(rotationY, 0, 1, 0);
		GlStateManager.color3f(1, 1, 1);
		renderItem(stack);
		GlStateManager.disableDepthTest();
		GL11.glPopMatrix();
	}

	private void renderItem(ItemStack itemStack) {
		parent.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);

		final ItemRenderer itemRenderer = parent.getItemRenderer();
		final ItemModelMesher itemModelMesher = itemRenderer.getItemModelMesher();
		final IBakedModel model = itemModelMesher.getItemModel(stack);

		GlStateManager.disableLighting();
		GlStateManager.enableRescaleNormal();
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
		GlStateManager.pushMatrix();
		itemRenderer.renderItem(stack, model);
		GlStateManager.cullFace(GlStateManager.CullFace.BACK);
		GlStateManager.popMatrix();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableBlend();

		GlStateManager.enableLighting();
	}
}
