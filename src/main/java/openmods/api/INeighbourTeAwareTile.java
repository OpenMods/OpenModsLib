package openmods.api;

import net.minecraft.util.math.BlockPos;

public interface INeighbourTeAwareTile {
	public void onNeighbourTeChanged(BlockPos pos);
}
