package openmods.entity.renderer;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import openmods.entity.EntityBlock;

public class EntityBlockRenderer extends Render<EntityBlock> {

	public EntityBlockRenderer(RenderManager renderManagerIn) {
		super(renderManagerIn);
		this.shadowSize = 0.5F;
	}

	@Override
	public void doRender(EntityBlock entity, double x, double y, double z, float yaw, float partialTicks) {
		final IBlockState blockState = entity.getBlockState();

		if (blockState.getRenderType() != EnumBlockRenderType.INVISIBLE) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(x - 0.5, y, z + 0.5);
			GlStateManager.disableLighting();

			bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

			Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlockBrightness(blockState, entity.getBrightness());

			GlStateManager.enableLighting();
			GlStateManager.popMatrix();
			super.doRender(entity, x, y, z, yaw, partialTicks);
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityBlock entity) {
		return TextureMap.LOCATION_BLOCKS_TEXTURE;
	}

}
