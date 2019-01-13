package openmods.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Coord implements Cloneable {
	public final int x;
	public final int y;
	public final int z;

	public Coord(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Coord(double x, double y, double z) {
		this.x = MathHelper.floor(x);
		this.y = MathHelper.floor(y);
		this.z = MathHelper.floor(z);
	}

	public Coord(int[] coords) {
		this(coords[0], coords[1], coords[2]);
	}

	public Coord(BlockPos pos) {
		this(pos.getX(), pos.getY(), pos.getZ());
	}

	public Coord(Vec3d vec) {
		this(vec.x, vec.y, vec.z);
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

	public BlockPos asBlockPos() {
		return new BlockPos(x, y, z);
	}

	public Vec3d asVector() {
		return new Vec3d(x, y, z);
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

	public boolean isAbove(Coord pos) {
		return pos != null && y > pos.y;
	}

	public boolean isBelow(Coord pos) {
		return pos != null && y < pos.y;
	}

	public boolean isNorthOf(Coord pos) {
		return pos != null && z < pos.z;
	}

	public boolean isSouthOf(Coord pos) {
		return pos != null && z > pos.z;
	}

	public boolean isEastOf(Coord pos) {
		return pos != null && x > pos.x;
	}

	public boolean isWestOf(Coord pos) {
		return pos != null && x < pos.x;
	}

	public boolean isXAligned(Coord pos) {
		return pos != null && x == pos.x;
	}

	public boolean isYAligned(Coord pos) {
		return pos != null && y == pos.y;
	}

	public boolean isZAligned(Coord pos) {
		return pos != null && z == pos.z;
	}
}
