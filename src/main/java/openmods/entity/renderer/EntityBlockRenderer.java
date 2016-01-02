package openmods.entity.renderer;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import openmods.entity.EntityBlock;

import org.lwjgl.opengl.GL11;

public class EntityBlockRenderer extends Render<EntityBlock> {

	public EntityBlockRenderer(RenderManager renderManagerIn) {
		super(renderManagerIn);
		this.shadowSize = 0.5F;
	}

	@Override
	public void doRender(EntityBlock entity, double x, double y, double z, float yaw, float partialTicks) {
		final IBlockState blockState = entity.getBlockState();
		final Block block = blockState.getBlock();

		if (block != null && block.getRenderType() == 3) {
			GlStateManager.pushMatrix();
			GlStateManager.translate((float)x, (float)y, (float)z);
			GlStateManager.disableLighting();

			bindTexture(TextureMap.locationBlocksTexture);

			final Tessellator tessellator = Tessellator.getInstance();
			final WorldRenderer wr = tessellator.getWorldRenderer();
			wr.func_181668_a(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

			wr.setTranslation((float)(-x) - 0.5F, (-y), (float)(-z) - 0.5F);

			final BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
			final IBakedModel model = dispatcher.getModelFromBlockState(blockState, entity.worldObj, (BlockPos)null);
			dispatcher.getBlockModelRenderer().renderModel(entity.worldObj, model, blockState, new BlockPos(entity), wr, false);
			wr.setTranslation(0.0D, 0.0D, 0.0D);
			tessellator.draw();

			GlStateManager.enableLighting();
			GlStateManager.popMatrix();
			super.doRender(entity, x, y, z, yaw, partialTicks);
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityBlock entity) {
		return TextureMap.locationBlocksTexture;
	}

}
