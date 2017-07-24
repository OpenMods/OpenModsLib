package openmods.api;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public interface INeighbourAwareTile {

	public void onNeighbourChanged(BlockPos pos, Block block);
}
