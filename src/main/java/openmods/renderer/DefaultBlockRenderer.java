package openmods.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.Log;
import openmods.block.OpenBlock;
import openmods.renderer.rotations.IRendererSetup;
import openmods.tileentity.OpenTileEntity;
import openmods.utils.CachedFactory;
import openmods.utils.render.RenderUtils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class DefaultBlockRenderer implements IBlockRenderer<Block> {

	private final CachedFactory<OpenBlock, TileEntity> inventoryTileEntities = new CachedFactory<OpenBlock, TileEntity>() {
		@Override
		protected TileEntity create(OpenBlock key) {
			return key.createTileEntityForRender();
		}
	};

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
		if (!(block instanceof OpenBlock)) {
			RenderUtils.renderInventoryBlock(renderer, block, ForgeDirection.EAST);
			return;
		}

		final OpenBlock openBlock = (OpenBlock)block;

		try {
			GL11.glEnable(GL12.GL_RESCALE_NORMAL);
			GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
			if (openBlock.shouldRenderTesrInInventory()) {
				TileEntity te = inventoryTileEntities.getOrCreate(openBlock);
				if (te instanceof OpenTileEntity) ((OpenTileEntity)te).prepareForInventoryRender(block, metadata);
				GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
				GL11.glPushMatrix();
				GL11.glTranslated(-0.5, -0.5, -0.5);
				TileEntityRendererDispatcher.instance.renderTileEntityAt(te, 0.0D, 0.0D, 0.0D, 0.0F);
				GL11.glPopMatrix();
				GL11.glPopAttrib();
			}

			if (openBlock.shouldRenderBlock()) {
				final ForgeDirection rotation = openBlock.getInventoryRenderRotation();

				openBlock.setBoundsBasedOnRotation(rotation);

				final IRendererSetup setup = openBlock.getRotationMode().rendererSetup;
				final RenderBlocks localRenderer = setup.enter(rotation, renderer);
				RenderUtils.renderInventoryBlock(localRenderer, block, rotation);
				setup.exit(localRenderer);
			}
		} catch (Exception e) {
			Log.severe(e, "Error during block '%s' rendering", block.getUnlocalizedName());
		}
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		if (!(block instanceof OpenBlock)) return renderer.renderStandardBlock(block, x, y, z);

		final OpenBlock openBlock = (OpenBlock)block;

		if (openBlock.shouldRenderBlock()) {
			final int metadata = world.getBlockMetadata(x, y, z);
			final IRendererSetup setup = openBlock.getRotationMode().rendererSetup;

			final ForgeDirection rotation = openBlock.getRotation(metadata);
			final RenderBlocks localRenderer = setup.enter(rotation, renderer);
			boolean wasRendered = localRenderer.renderStandardBlock(openBlock, x, y, z);
			setup.exit(localRenderer);
			return wasRendered;
		}
		return false;
	}

}
