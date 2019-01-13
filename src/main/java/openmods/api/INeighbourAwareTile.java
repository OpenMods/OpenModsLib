package openmods.api;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public interface INeighbourAwareTile {

	void onNeighbourChanged(BlockPos neighbourPos, Block neigbourBlock);
}
