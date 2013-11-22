package openmods.utils;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.ForgeDirection;

public class Coord {
	public int x;
	public int y;
	public int z;

	public Coord() {}

	public Coord(ForgeDirection direction) {
		x = direction.offsetX;
		y = direction.offsetY;
		z = direction.offsetZ;
	}

	public Coord(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void offset(ForgeDirection direction) {
		x += direction.offsetX;
		y += direction.offsetY;
		z += direction.offsetZ;
	}

	public void set(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public int hashCode() {
		return (x + 128) << 16 | (y + 128) << 8 | (z + 128);
	}

	@Override
	public boolean equals(Object that) {
		if (!(that instanceof Coord)) { return false; }
		Coord otherCoord = (Coord)that;
		return otherCoord.x == x && otherCoord.y == y && otherCoord.z == z;
	}

	@Override
	public String toString() {
		return String.format("%s,%s,%s", x, y, z);
	}

	public void offset(int ox, int oy, int oz) {
		x += ox;
		y += oy;
		z += oz;
	}

	public void setFrom(Coord copy) {
		x = copy.x;
		y = copy.y;
		z = copy.z;
	}

	@Override
	public Coord clone() {
		return new Coord(x, y, z);
	}
	
	public Coord add(Coord pos){
		return new Coord(x + pos.x, y + pos.y, z + pos.z);
	}
	
	public Coord add(int ai[]){
		return new Coord(x + ai[0], y + ai[1], z + ai[2]);
	}
	
	public Coord substract(Coord pos){
		return new Coord(x - pos.x, y - pos.y, z - pos.z);
	}
	
	public Coord substract(int ai[]){
		return new Coord(x - ai[0], y - ai[1], z - ai[2]);
	}
	
	public Coord getAdjacentCoord(ForgeDirection fd){
		return getOffsetCoord(fd, 1);
	}
	
	public Coord getOffsetCoord(ForgeDirection fd, int distance){
		return new Coord(x + (fd.offsetX * distance), y + (fd.offsetY * distance), z + (fd.offsetZ * distance));
	}
	
	public Coord[] getDirectlyAdjacentCoords(){
		return getDirectlyAdjacentCoords(true);
	}
	
	public Coord[] getDirectlyAdjacentCoords(boolean includeBelow){
		Coord[] adjacents;
		if (includeBelow)
			adjacents = new Coord[6];
		else
			adjacents = new Coord[5];
		
		adjacents[0] = getAdjacentCoord(ForgeDirection.UP);
		adjacents[1] = getAdjacentCoord(ForgeDirection.NORTH);
		adjacents[2] = getAdjacentCoord(ForgeDirection.EAST);
		adjacents[3] = getAdjacentCoord(ForgeDirection.SOUTH);
		adjacents[4] = getAdjacentCoord(ForgeDirection.WEST);
		
		if (includeBelow)
			adjacents[5] = getAdjacentCoord(ForgeDirection.DOWN);
		
		return adjacents;
	}
	
	public Coord[] getAdjacentCoords(){
		return getAdjacentCoords(true, true);
	}
	
	public Coord[] getAdjacentCoords(boolean includeBelow, boolean includeDiagonal){
		if (!includeDiagonal)
			return getDirectlyAdjacentCoords(includeBelow);
		
		Coord[] adjacents = new Coord[(includeBelow ? 26 : 17)];
		
		int index = 0;
		
		for (int xl = -1; xl < 1; xl++)
			for (int zl = -1; zl < 1; zl++)
				for (int yl = (includeBelow ? -1 : 0); yl < 1; yl++)
					if (xl != 0 || zl != 0 || yl != 0)
						adjacents[index++] = new Coord(x + xl, y + yl, z + zl);
		
		return adjacents;
	}
	
	public boolean isAbove(Coord pos){
		return pos != null ? y > pos.y : false;
	}
	
	public boolean isBelow(Coord pos){
		return pos != null ? y < pos.y : false;
	}
	
	public boolean isNorthOf(Coord pos){
		return pos != null ? z < pos.z : false;
	}
	
	public boolean isSouthOf(Coord pos){
		return pos != null ? z > pos.z : false;
	}
	
	public boolean isEastOf(Coord pos){
		return pos != null ? x > pos.x : false;
	}
	
	public boolean isWestOf(Coord pos){
		return pos != null ? x < pos.x : false;
	}
	
	public boolean isXAligned(Coord pos){
		return pos != null ? x == pos.x : false;
	}
	
	public boolean isYAligned(Coord pos){
		return pos != null ? y == pos.y : false;
	}
	
	public boolean isZAligned(Coord pos){
		return pos != null ? z == pos.z : false;
	}
	
	public boolean isAirBlock(World world){
		return world.isAirBlock(x, y, z);
	}
	
	public boolean isBlockNormalCube(World world){
		return world.isBlockNormalCube(x, y, z);
	}
	
	public boolean isBlockOpaqueCube(World world){
		return world.isBlockOpaqueCube(x, y, z);
	}
	
	public boolean isWood(World world){
		Block block = Block.blocksList[this.getBlockID(world)];
		return block != null && block.isWood(world, x, y, z);
	}
	
	public boolean isLeaves(World world){
		Block block = Block.blocksList[this.getBlockID(world)];
		return block != null && block.isLeaves(world, x, y, z);
	}
	
	public int getBlockID(World world){
		return world.getBlockId(x, y, z);
	}
	
	public int getBlockMetadata(World world){
		return world.getBlockMetadata(x, y, z);
	}
	
	public BiomeGenBase getBiomeGenBase(World world){
		return world.getBiomeGenForCoords(x, z);
	}
	
	public static boolean moveBlock(World world, Coord src, Coord tgt, boolean allowBlockReplacement){
		if (!world.isRemote && !src.isAirBlock(world) && (tgt.isAirBlock(world) || allowBlockReplacement)){
			int blockID = src.getBlockID(world);
			int metadata = src.getBlockMetadata(world);
			
			world.setBlock(tgt.x, tgt.y, tgt.z, blockID, metadata, BlockNotifyType.ALL);
			
			if (world.blockHasTileEntity(src.x, src.y, src.z)){
				TileEntity te = world.getBlockTileEntity(src.x, src.y, src.z);
				if (te != null){
					NBTTagCompound nbt = new NBTTagCompound();
					te.writeToNBT(nbt);
					
					nbt.setInteger("x", tgt.x);
					nbt.setInteger("y", tgt.y);
					nbt.setInteger("z", tgt.z);
					
					te = world.getBlockTileEntity(tgt.x, tgt.y, tgt.z);
					if (te != null)
						te.readFromNBT(nbt);
				}
			}
			
			world.setBlockToAir(src.x, src.y, src.z);
			return true;
		}
		return false;
	}
	
	public boolean moveBlockToHereFrom(World world, Coord src, boolean allowBlockReplacement){
		return moveBlock(world, src, this, allowBlockReplacement);
	}
	
	public boolean moveBlockFromHereTo(World world, Coord tgt, boolean allowBlockReplacement){
		return moveBlock(world, this, tgt, allowBlockReplacement);
	}
}
