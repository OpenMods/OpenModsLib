package openmods.colors;

import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
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
	public int colorMultiplier(BlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int tintIndex) {
		return tintIndex == 0? color : WHITE;
	}
}