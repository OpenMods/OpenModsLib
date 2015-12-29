package openmods.network;

import net.minecraft.util.BlockPos;

public class DimCoord {
	public final int dimension;
	public final BlockPos blockPos;

	public DimCoord(int dimension, BlockPos blockPos) {
		this.dimension = dimension;
		this.blockPos = blockPos;
	}

}
