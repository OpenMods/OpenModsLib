package openmods.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

public interface IBlockRenderer<B extends Block> {
	public void renderInventoryBlock(B block, int metadata, int modelID, RenderBlocks renderer);

	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, B block, int modelId, RenderBlocks renderer);
}
