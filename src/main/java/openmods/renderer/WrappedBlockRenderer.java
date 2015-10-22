package openmods.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import openmods.reflection.ClonerFactory;
import openmods.reflection.ClonerFactory.ICloner;

public abstract class WrappedBlockRenderer<T extends Block> implements IBlockRenderer<T> {

	private static final ICloner<RenderBlocks> CLONER = ClonerFactory.instance.getCloner(RenderBlocks.class);

	private final IBlockRenderer<T> wrapperRenderer;

	public WrappedBlockRenderer(IBlockRenderer<T> wrapperRenderer) {
		this.wrapperRenderer = wrapperRenderer;
	}

	@Override
	public void renderInventoryBlock(T block, int metadata, int modelID, RenderBlocks renderer) {
		final RenderBlocks fixedRenderer = createWrapper(renderer);
		CLONER.clone(renderer, fixedRenderer);
		wrapperRenderer.renderInventoryBlock(block, metadata, modelID, fixedRenderer);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, T block, int modelId, RenderBlocks renderer) {
		final RenderBlocks fixedRenderer = createWrapper(renderer);
		CLONER.clone(renderer, fixedRenderer);
		return wrapperRenderer.renderWorldBlock(world, x, y, z, block, modelId, fixedRenderer);
	}

	protected abstract RenderBlocks createWrapper(RenderBlocks renderer);
}
