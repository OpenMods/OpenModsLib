package openmods.utils;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

public class TagUtils {

	private static final String TAG_Z = "Z";
	private static final String TAG_Y = "Y";
	private static final String TAG_X = "X";

	public static NBTTagCompound store(int x, int y, int z) {
		NBTTagCompound result = new NBTTagCompound();
		result.setInteger(TAG_X, x);
		result.setInteger(TAG_Y, y);
		result.setInteger(TAG_Z, z);
		return result;
	}

	public static NBTTagCompound store(double x, double y, double z) {
		NBTTagCompound result = new NBTTagCompound();
		result.setDouble(TAG_X, x);
		result.setDouble(TAG_Y, y);
		result.setDouble(TAG_Z, z);
		return result;
	}

	public static NBTTagCompound store(Coord coords) {
		return store(coords.x, coords.y, coords.z);
	}

	public static NBTTagCompound store(Vec3 vec) {
		return store(vec.xCoord, vec.yCoord, vec.zCoord);
	}

	public static Coord readCoord(NBTTagCompound tag) {
		final int x = tag.getInteger(TAG_X);
		final int y = tag.getInteger(TAG_Y);
		final int z = tag.getInteger(TAG_Z);
		return new Coord(x, y, z);
	}

	public static Vec3 readVec(NBTTagCompound tag) {
		final double x = tag.getDouble(TAG_X);
		final double y = tag.getDouble(TAG_Y);
		final double z = tag.getDouble(TAG_Z);
		return new Vec3(x, y, z);
	}
}
