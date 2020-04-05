package openmods.entity.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.util.ResourceLocation;
import openmods.entity.EntityBlock;

public class EntityBlockRenderer extends EntityRenderer<EntityBlock> {

	public EntityBlockRenderer(EntityRendererManager renderManagerIn) {
		super(renderManagerIn);
		this.shadowSize = 0.5F;
	}

	@Override
	public void doRender(EntityBlock entity, double x, double y, double z, float yaw, float partialTicks) {
		final BlockState blockState = entity.getBlockState();

		if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
			GlStateManager.pushMatrix();
			GlStateManager.translated(x - 0.5, y, z + 0.5);
			GlStateManager.disableLighting();

			bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);

			Minecraft.getInstance().getBlockRendererDispatcher().renderBlockBrightness(blockState, entity.getBrightness());

			GlStateManager.enableLighting();
			GlStateManager.popMatrix();
			super.doRender(entity, x, y, z, yaw, partialTicks);
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityBlock entity) {
		return AtlasTexture.LOCATION_BLOCKS_TEXTURE;
	}

}
