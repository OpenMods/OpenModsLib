package openmods.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

public class FakeBlockAccess implements IBlockAccess {
	private final BlockState state;
	private final TileEntity tileEntity;

	private static final ResourceLocation BIOME_DESERT = new ResourceLocation("desert");
	private final Biome biome = Biome.REGISTRY.getObject(BIOME_DESERT);

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
	public TileEntity getTileEntity(BlockPos blockPos) {
		return isOrigin(blockPos)? tileEntity : null;
	}

	@Override
	public int getCombinedLight(BlockPos blockPos, int p_72802_4_) {
		return 0xF000F0;
	}

	@Override
	public int getStrongPower(BlockPos pos, Direction direction) {
		return 0;
	}

	@Override
	public boolean isAirBlock(BlockPos blockPos) {
		return !isOrigin(blockPos);
	}

	@Override
	public Biome getBiome(BlockPos pos) {
		return biome;
	}

	@Override
	public boolean isSideSolid(BlockPos blockPos, Direction side, boolean _default) {
		return (isOrigin(blockPos))? state.isSideSolid(this, blockPos, side) : _default;
	}

	@Override
	public WorldType getWorldType() {
		return WorldType.DEFAULT;
	}
}