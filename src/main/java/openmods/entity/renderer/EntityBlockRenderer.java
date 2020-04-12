package openmods.entity.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import openmods.entity.EntityBlock;

public class EntityBlockRenderer extends EntityRenderer<EntityBlock> {

	public EntityBlockRenderer(EntityRendererManager renderManagerIn) {
		super(renderManagerIn);
		this.shadowSize = 0.5F;
	}

	@Override
	public void render(EntityBlock entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
		final BlockState blockState = entity.getBlockState();

		if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
			matrixStack.push();
			matrixStack.translate(-0.5, -0.5, -0.5);
			GlStateManager.disableLighting();

			Minecraft.getInstance().getBlockRendererDispatcher().renderBlock(blockState, matrixStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);

			GlStateManager.enableLighting();
			matrixStack.pop();
			super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
		}
	}

	@Override
	public ResourceLocation getEntityTexture(EntityBlock entity) {
		return AtlasTexture.LOCATION_BLOCKS_TEXTURE;
	}

}
