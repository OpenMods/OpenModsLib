package openmods.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.LightType;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class FakeBlockAccess implements IBlockDisplayReader {
	private final BlockState state;
	private final TileEntity tileEntity;

	public FakeBlockAccess(BlockState state, TileEntity tileEntity) {
		this.state = state;
		this.tileEntity = tileEntity;
	}

	public FakeBlockAccess(BlockState state) {
		this(state, null);
	}

	public static final BlockPos ORIGIN = new BlockPos(0, 0, 0);

	private static boolean isOrigin(BlockPos blockPos) {
		return ORIGIN.equals(blockPos);
	}

	@Override
	public BlockState getBlockState(BlockPos blockPos) {
		return isOrigin(blockPos)? state : Blocks.AIR.getDefaultState();
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return isOrigin(pos)? state.getFluidState() : Fluids.EMPTY.getDefaultState();
	}

	@Override
	public TileEntity getTileEntity(BlockPos blockPos) {
		return isOrigin(blockPos)? tileEntity : null;
	}

	@Override
	public int getLightSubtracted(BlockPos blockPos, int amount) {
		return 0xF000F0;
	}

	@Override public int getLightFor(LightType type, BlockPos pos) {
		return 0xF000F0;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public float func_230487_a_(Direction p_230487_1_, boolean p_230487_2_) {
		return 1.0f;
	}

	@Override
	public WorldLightManager getLightManager() {
		return null; // Should not be used?
	}

	@Override
	public int getBlockColor(BlockPos blockPosIn, ColorResolver colorResolverIn) {
		return 0xFFFFFFFF;
	}
}