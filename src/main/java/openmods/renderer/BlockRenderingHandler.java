package openmods.renderer;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import com.google.common.collect.Maps;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

public class BlockRenderingHandler implements ISimpleBlockRenderingHandler {

	private final boolean itemsIn3d;

	private final int renderId;

	public BlockRenderingHandler(int renderId) {
		this(renderId, true);
	}

	public BlockRenderingHandler(int renderId, boolean itemsIn3d) {
		this.renderId = renderId;
		this.itemsIn3d = itemsIn3d;
	}

	protected static final IBlockRenderer<Block> DEFAULT_RENDERER = new DefaultBlockRenderer();

	protected final Map<Block, IBlockRenderer<?>> blockRenderers = Maps.newIdentityHashMap();

	public <B extends Block> void addRenderer(B block, IBlockRenderer<? super B> renderer) {
		if (block != null) blockRenderers.put(block, renderer);
	}

	@SuppressWarnings("unchecked")
	protected <B extends Block> IBlockRenderer<B> getRenderer(B block) {
		IBlockRenderer<?> renderer = blockRenderers.get(block);
		return (IBlockRenderer<B>)(renderer != null? renderer : DEFAULT_RENDERER);
	}

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
		getRenderer(block).renderInventoryBlock(block, metadata, modelID, renderer);

	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		return getRenderer(block).renderWorldBlock(world, x, y, z, block, modelId, renderer);
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return itemsIn3d;
	}

	@Override
	public int getRenderId() {
		return renderId;
	}
}
