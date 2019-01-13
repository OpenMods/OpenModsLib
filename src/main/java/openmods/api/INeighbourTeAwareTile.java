package openmods.api;

import net.minecraft.util.math.BlockPos;

public interface INeighbourTeAwareTile {
	void onNeighbourTeChanged(BlockPos pos);
}
