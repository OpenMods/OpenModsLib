package openmods.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
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
	public void renderOverlay(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		matrixStack.push();
		GlStateManager.enableDepthTest();
		float scale = 30.0f;
		matrixStack.translate(offsetX + x + (scale / 2), offsetY + y + (scale / 2), scale);
		matrixStack.scale(scale, -scale, scale);
		rotationY += 0.6f;
		matrixStack.rotate(new Quaternion(Vector3f.XP, 20, true));
		matrixStack.rotate(new Quaternion(Vector3f.YP, rotationY, true));
		GlStateManager.color4f(1, 1, 1, 1);
		renderItem(matrixStack, stack);
		GlStateManager.disableDepthTest();
		matrixStack.pop();
	}

	private void renderItem(final MatrixStack matrixStack, ItemStack itemStack) {
		parent.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);

		final ItemRenderer itemRenderer = parent.getItemRenderer();
		final ItemModelMesher itemModelMesher = itemRenderer.getItemModelMesher();
		final IBakedModel model = itemModelMesher.getItemModel(stack);

		GlStateManager.disableLighting();
		GlStateManager.enableRescaleNormal();
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
		matrixStack.push();

		IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
		itemRenderer.renderItem(stack, ItemCameraTransforms.TransformType.GUI, false, matrixStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, model);
		buffer.finish();

		matrixStack.pop();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableBlend();

		GlStateManager.enableLighting();
	}
}
