package openmods.renderer;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.Log;
import openmods.block.OpenBlock;
import openmods.tileentity.OpenTileEntity;
import openmods.utils.render.RenderUtils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.collect.Maps;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

public abstract class BlockRenderingHandlerBase implements ISimpleBlockRenderingHandler {

	protected final Map<Block, TileEntity> inventoryTileEntities = Maps.newIdentityHashMap();
	protected final Map<Block, IBlockRenderer<Block>> blockRenderers = Maps.newIdentityHashMap();

	@SuppressWarnings("unchecked")
	public <B extends Block> void addRenderer(B block, IBlockRenderer<B> renderer) {
		blockRenderers.put(block, (IBlockRenderer<Block>)renderer);
	}

	public TileEntity getTileEntityForBlock(OpenBlock block) {
		TileEntity te = inventoryTileEntities.get(block);
		if (te == null) {
			te = block.createTileEntityForRender();
			inventoryTileEntities.put(block, te);
		}
		return te;
	}

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
		if (blockRenderers.containsKey(block)) {
			blockRenderers.get(block).renderInventoryBlock(block, metadata, modelID, renderer);
			return;
		}

		OpenBlock openBlock = (block instanceof OpenBlock)? (OpenBlock)block : null;
		TileEntity te = null;
		if (openBlock != null && openBlock.useTESRForInventory()) {
			Class<? extends TileEntity> teClass = openBlock.getTileClass();
			if (teClass != null && TileEntityRendererDispatcher.instance.mapSpecialRenderers.containsKey(teClass)) {
				te = getTileEntityForBlock(openBlock);
				if (te instanceof OpenTileEntity) ((OpenTileEntity)te).prepareForInventoryRender(block, metadata);
			}
		}

		try {
			GL11.glEnable(GL12.GL_RESCALE_NORMAL);
			GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
			if (te != null) {
				GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
				GL11.glPushMatrix();
				GL11.glTranslated(-0.5, -0.5, -0.5);
				TileEntityRendererDispatcher.instance.renderTileEntityAt(te, 0.0D, 0.0D, 0.0D, 0.0F);
				GL11.glPopMatrix();
				GL11.glPopAttrib();
			}

			if (openBlock == null || openBlock.shouldRenderBlock()) {
				ForgeDirection rotation;
				if (openBlock != null) {
					rotation = openBlock.getInventoryRenderRotation();
					openBlock.setBoundsBasedOnRotation(rotation);
					RenderUtils.rotateFacesOnRenderer(openBlock, rotation, renderer);
				} else rotation = ForgeDirection.EAST;

				RenderUtils.renderInventoryBlock(renderer, block, rotation);
				RenderUtils.resetFacesOnRenderer(renderer);
			}
		} catch (Exception e) {
			Log.severe(e, "Error during block '%s' rendering", block.getUnlocalizedName());
		}
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		OpenBlock openBlock = (block instanceof OpenBlock)? (OpenBlock)block : null;

		IBlockRenderer<Block> customRenderer = blockRenderers.get(block);
		if (customRenderer != null) return customRenderer.renderWorldBlock(world, x, y, z, block, modelId, renderer);

		if (openBlock == null || openBlock.shouldRenderBlock()) {
			if (openBlock != null) {
				int metadata = world.getBlockMetadata(x, y, z);
				ForgeDirection rotation = ForgeDirection.getOrientation(metadata);
				RenderUtils.rotateFacesOnRenderer((OpenBlock)block, rotation, renderer);
			}
			renderer.renderStandardBlock(block, x, y, z);
			RenderUtils.resetFacesOnRenderer(renderer);
		}
		return true;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return true;
	}
}
