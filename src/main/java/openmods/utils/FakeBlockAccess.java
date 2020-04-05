package openmods.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

public class FakeBlockAccess implements IEnviromentBlockReader {
	private final BlockState state;
	private final TileEntity tileEntity;

	private static final ResourceLocation BIOME_DESERT = new ResourceLocation("desert");
	private final Biome biome = ForgeRegistries.BIOMES.getValue(BIOME_DESERT);

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
	public Biome getBiome(BlockPos pos) {
		return biome;
	}

	@Override public int getLightFor(LightType type, BlockPos pos) {
		return 0xF000F0;
	}

	@Override public IFluidState getFluidState(BlockPos pos) {
		return Fluids.EMPTY.getDefaultState();
	}
}