package openmods.utils;

import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraftforge.common.util.ForgeDirection;

public class Coord implements Cloneable {
	public final int x;
	public final int y;
	public final int z;

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

	public Coord(double x, double y, double z) {
		this.x = MathHelper.floor_double(x);
		this.y = MathHelper.floor_double(y);
		this.z = MathHelper.floor_double(z);
	}

	public Coord(int[] coords) {
		this.x = coords[0];
		this.y = coords[1];
		this.z = coords[2];
	}

	public Coord(ChunkPosition pos) {
		this.x = pos.chunkPosX;
		this.y = pos.chunkPosY;
		this.z = pos.chunkPosZ;
	}

	public Coord offset(ForgeDirection direction) {
		return new Coord(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ);
	}

	public Coord offset(int ox, int oy, int oz) {
		return new Coord(x + ox, y + oy, z + oz);
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

	@Override
	public Coord clone() {
		return new Coord(x, y, z);
	}

	public ChunkPosition asChunkPosition() {
		return new ChunkPosition(x, y, z);
	}

	public Coord add(Coord other) {
		return new Coord(x + other.x, y + other.y, z + other.z);
	}

	public Coord substract(Coord other) {
		return new Coord(x - other.x, y - other.y, z - other.z);
	}

	public int lengthSq() {
		return x * x + y * y + z * z;
	}

	public double length() {
		return Math.sqrt(lengthSq());
	}

	public Coord getAdjacentCoord(ForgeDirection fd) {
		return getOffsetCoord(fd, 1);
	}

	public Coord getOffsetCoord(ForgeDirection fd, int distance) {
		return new Coord(x + (fd.offsetX * distance), y + (fd.offsetY * distance), z + (fd.offsetZ * distance));
	}

	public Coord[] getDirectlyAdjacentCoords() {
		return getDirectlyAdjacentCoords(true);
	}

	public Coord[] getDirectlyAdjacentCoords(boolean includeBelow) {
		Coord[] adjacents;
		if (includeBelow) adjacents = new Coord[6];
		else adjacents = new Coord[5];

		adjacents[0] = getAdjacentCoord(ForgeDirection.UP);
		adjacents[1] = getAdjacentCoord(ForgeDirection.NORTH);
		adjacents[2] = getAdjacentCoord(ForgeDirection.EAST);
		adjacents[3] = getAdjacentCoord(ForgeDirection.SOUTH);
		adjacents[4] = getAdjacentCoord(ForgeDirection.WEST);

		if (includeBelow) adjacents[5] = getAdjacentCoord(ForgeDirection.DOWN);

		return adjacents;
	}

	public Coord[] getAdjacentCoords() {
		return getAdjacentCoords(true, true);
	}

	public Coord[] getAdjacentCoords(boolean includeBelow, boolean includeDiagonal) {
		if (!includeDiagonal) return getDirectlyAdjacentCoords(includeBelow);

		Coord[] adjacents = new Coord[(includeBelow? 26 : 17)];

		int index = 0;

		for (int xl = -1; xl < 1; xl++)
			for (int zl = -1; zl < 1; zl++)
				for (int yl = (includeBelow? -1 : 0); yl < 1; yl++)
					if (xl != 0 || zl != 0 || yl != 0) adjacents[index++] = new Coord(x + xl, y + yl, z + zl);

		return adjacents;
	}

	public boolean isAbove(Coord pos) {
		return pos != null? y > pos.y : false;
	}

	public boolean isBelow(Coord pos) {
		return pos != null? y < pos.y : false;
	}

	public boolean isNorthOf(Coord pos) {
		return pos != null? z < pos.z : false;
	}

	public boolean isSouthOf(Coord pos) {
		return pos != null? z > pos.z : false;
	}

	public boolean isEastOf(Coord pos) {
		return pos != null? x > pos.x : false;
	}

	public boolean isWestOf(Coord pos) {
		return pos != null? x < pos.x : false;
	}

	public boolean isXAligned(Coord pos) {
		return pos != null? x == pos.x : false;
	}

	public boolean isYAligned(Coord pos) {
		return pos != null? y == pos.y : false;
	}

	public boolean isZAligned(Coord pos) {
		return pos != null? z == pos.z : false;
	}
}
