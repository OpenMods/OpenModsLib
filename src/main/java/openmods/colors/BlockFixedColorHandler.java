package openmods.colors;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;

public class BlockFixedColorHandler implements IBlockColor {
	private static final int WHITE = 0xFFFFFFFF;
	private final int color;

	public BlockFixedColorHandler(int color) {
		this.color = color;
	}

	public BlockFixedColorHandler(ColorMeta color) {
		this.color = color.rgb;
	}

	@Override
	public int getColor(BlockState state, IBlockDisplayReader worldIn, BlockPos pos, int tintIndex) {
		return tintIndex == 0? color : WHITE;
	}
}