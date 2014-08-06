package openmods.api;

import net.minecraft.block.Block;

public interface INeighbourAwareTile {

	public void onNeighbourChanged(Block block);
}
