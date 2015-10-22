package openmods.renderer;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.block.OpenBlock;
import openmods.renderer.rotations.IRendererSetup;

public class RotatedBlockRenderer<T extends OpenBlock> implements IBlockRenderer<T> {

	private final IBlockRenderer<T> wrapperRenderer;

	public RotatedBlockRenderer(IBlockRenderer<T> wrapperRenderer) {
		this.wrapperRenderer = wrapperRenderer;
	}

	@Override
	public void renderInventoryBlock(T block, int metadata, int modelID, RenderBlocks renderer) {
		final ForgeDirection rotation = block.getInventoryRenderRotation();

		final IRendererSetup setup = block.getRotationMode().rendererSetup;
		final RenderBlocks localRenderer = setup.enter(rotation, renderer);
		wrapperRenderer.renderInventoryBlock(block, metadata, modelID, localRenderer);
		setup.exit(localRenderer);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, T block, int modelId, RenderBlocks renderer) {
		final int metadata = world.getBlockMetadata(x, y, z);
		final ForgeDirection rotation = block.getRotation(metadata);

		final IRendererSetup setup = block.getRotationMode().rendererSetup;
		final RenderBlocks localRenderer = setup.enter(rotation, renderer);
		boolean wasRendered = wrapperRenderer.renderWorldBlock(world, x, y, z, block, modelId, localRenderer);
		setup.exit(localRenderer);
		return wasRendered;
	}

	public static <T extends OpenBlock> IBlockRenderer<T> wrap(IBlockRenderer<T> renderer) {
		return new RotatedBlockRenderer<T>(renderer);
	}
}
