package openmods.utils;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

public class FakeBlockAccess implements IBlockAccess {
	private final Block block;
	private final int metadata;
	private final TileEntity tileEntity;

	public FakeBlockAccess(Block block, int metadata, TileEntity tileEntity) {
		this.block = block;
		this.metadata = metadata;
		this.tileEntity = tileEntity;
	}

	public FakeBlockAccess(Block block, int metadata) {
		this(block, metadata, null);
	}

	private static boolean isOrigin(int x, int y, int z) {
		return x == 0 && y == 0 && z == 0;
	}

	@Override
	public Block getBlock(int x, int y, int z) {
		return isOrigin(x, y, z)? block : Blocks.air;
	}

	@Override
	public TileEntity getTileEntity(int x, int y, int z) {
		return isOrigin(x, y, z)? tileEntity : null;
	}

	@Override
	public int getLightBrightnessForSkyBlocks(int x, int y, int z, int p_72802_4_) {
		return 0xF000F0;
	}

	@Override
	public int getBlockMetadata(int x, int y, int z) {
		return isOrigin(x, y, z)? metadata : 0;
	}

	@Override
	public int isBlockProvidingPowerTo(int x, int y, int z, int dir) {
		return 0;
	}

	@Override
	public boolean isAirBlock(int x, int y, int z) {
		return !isOrigin(x, y, z);
	}

	@Override
	public BiomeGenBase getBiomeGenForCoords(int x, int z) {
		return BiomeGenBase.desert;
	}

	@Override
	public int getHeight() {
		return 256;
	}

	@Override
	public boolean extendedLevelsInChunkCache() {
		return false;
	}

	@Override
	public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
		return (isOrigin(x, y, z))? block.isSideSolid(this, x, y, z, side) : _default;
	}
}