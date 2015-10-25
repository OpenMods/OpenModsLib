package openmods.renderer;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import openmods.block.OpenBlock;
import openmods.geometry.Orientation;
import openmods.renderer.rotations.IRendererSetup;

public class RotatedBlockRenderer<T extends OpenBlock> implements IBlockRenderer<T> {

	private final IBlockRenderer<T> wrapperRenderer;

	public RotatedBlockRenderer(IBlockRenderer<T> wrapperRenderer) {
		this.wrapperRenderer = wrapperRenderer;
	}

	@Override
	public void renderInventoryBlock(T block, int metadata, int modelID, RenderBlocks renderer) {
		final Orientation orientation = block.getInventoryRenderOrientation();
		final int renderMetadata = block.getInventoryRenderMetadata(metadata);

		final IRendererSetup setup = block.getRotationMode().rendererSetup;
		final RenderBlocks localRenderer = setup.enter(orientation, renderMetadata, renderer);
		wrapperRenderer.renderInventoryBlock(block, renderMetadata, modelID, localRenderer);
		setup.exit(localRenderer);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, T block, int modelId, RenderBlocks renderer) {
		final int metadata = world.getBlockMetadata(x, y, z);
		final Orientation orientation = block.getOrientation(metadata);

		final IRendererSetup setup = block.getRotationMode().rendererSetup;
		final RenderBlocks localRenderer = setup.enter(orientation, metadata, renderer);
		boolean wasRendered = wrapperRenderer.renderWorldBlock(world, x, y, z, block, modelId, localRenderer);
		setup.exit(localRenderer);
		return wasRendered;
	}

	public static <T extends OpenBlock> IBlockRenderer<T> wrap(IBlockRenderer<T> renderer) {
		return new RotatedBlockRenderer<T>(renderer);
	}
}
