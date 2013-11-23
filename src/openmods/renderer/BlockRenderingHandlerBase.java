package openmods.renderer;

import java.util.Map;

import openmods.Log;
import openmods.block.OpenBlock;
import openmods.tileentity.OpenTileEntity;
import openmods.utils.RenderUtils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.collect.Maps;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.ForgeDirection;

public class BlockRenderingHandlerBase {

	protected final Map<Block, TileEntity> inventoryTileEntities = Maps.newIdentityHashMap();
	protected final Map<Block, IBlockRenderer> blockRenderers = Maps.newIdentityHashMap();

	public TileEntity getTileEntityForBlock(Block block) {
		TileEntity te = inventoryTileEntities.get(block);
		if (te == null) {
			te = block.createTileEntity(Minecraft.getMinecraft().theWorld, 0);
			inventoryTileEntities.put(block, te);
		}
		return te;
	}
	
	protected void doRenderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
		OpenBlock openBlock = null;
		if (block instanceof OpenBlock) {
			openBlock = (OpenBlock)block;
		}
		/**
		 * Deal with special block rendering handlers
		 */
		if (blockRenderers.containsKey(block)) {
			blockRenderers.get(block).renderInventoryBlock(block, metadata, modelID, renderer);
			return;
		}
		TileEntity te = null;
		// if it's an openblock
		if (openBlock != null && openBlock.useTESRForInventory()) {
			// get the TE class for this block
			Class<? extends TileEntity> teClass = openBlock.getTileClass();
			// if we've got a special renderer for it
			if (teClass != null && TileEntityRenderer.instance.specialRendererMap.containsKey(teClass)) {
				// get the cached copy
				te = getTileEntityForBlock(block);
				// if it's an opentileentity, prepare it for inventory rendering
				if (te instanceof OpenTileEntity) {
					((OpenTileEntity)te).prepareForInventoryRender(block, metadata);
				}
			}
		}

		try {
			GL11.glEnable(GL12.GL_RESCALE_NORMAL);
			GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
			if (te != null) {
				GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
				GL11.glPushMatrix();
				GL11.glTranslated(-0.5, -0.5, -0.5);
				TileEntityRenderer.instance.renderTileEntityAt(te, 0.0D, 0.0D, 0.0D, 0.0F);
				GL11.glPopMatrix();
				GL11.glPopAttrib();
			}
			if (openBlock == null || openBlock.shouldRenderBlock()) {
				ForgeDirection rotation = ForgeDirection.EAST;
				if (block instanceof OpenBlock) {
					rotation = openBlock.getInventoryRenderRotation();
					openBlock.setBoundsBasedOnRotation(rotation);
					RenderUtils.rotateFacesOnRenderer(openBlock, rotation, renderer);
				}
				RenderUtils.renderInventoryBlock(renderer, block, rotation);
				RenderUtils.resetFacesOnRenderer(renderer);
			}
		} catch (Exception e) {
			Log.severe(e, "Error during block '%s' rendering", block.getUnlocalizedName());
		}
	}
	
	protected boolean doRenderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {

		OpenBlock openBlock = null;
		if (block instanceof OpenBlock) {
			openBlock = (OpenBlock)block;
		}
		/* deal with custom block renderers */
		if (blockRenderers.containsKey(block)) {
			return blockRenderers.get(block).renderWorldBlock(world, x, y, z, block, modelId, renderer);
		} else if (openBlock == null || openBlock.shouldRenderBlock()) {
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

}
