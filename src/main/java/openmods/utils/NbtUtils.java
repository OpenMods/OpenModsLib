package openmods.utils;

import com.google.common.base.Objects;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.util.Constants;

public class NbtUtils {

	private static final String KEY = "K";
	private static final String VALUE = "V";

	private static final String TAG_Z = "Z";
	private static final String TAG_Y = "Y";
	private static final String TAG_X = "X";

	public static boolean hasCoordinates(NBTTagCompound tag) {
		return tag.hasKey(TAG_X, Constants.NBT.TAG_ANY_NUMERIC) &&
				tag.hasKey(TAG_Y, Constants.NBT.TAG_ANY_NUMERIC) &&
				tag.hasKey(TAG_Z, Constants.NBT.TAG_ANY_NUMERIC);
	}

	public static NBTTagCompound store(NBTTagCompound tag, int x, int y, int z) {
		tag.setInteger(TAG_X, x);
		tag.setInteger(TAG_Y, y);
		tag.setInteger(TAG_Z, z);
		return tag;
	}

	public static NBTTagCompound store(int x, int y, int z) {
		return store(new NBTTagCompound(), x, y, z);
	}

	public static NBTTagCompound store(NBTTagCompound tag, double x, double y, double z) {
		tag.setDouble(TAG_X, x);
		tag.setDouble(TAG_Y, y);
		tag.setDouble(TAG_Z, z);
		return tag;
	}

	public static NBTTagCompound store(double x, double y, double z) {
		return store(new NBTTagCompound(), x, y, z);
	}

	public static NBTTagCompound store(NBTTagCompound tag, Vec3i coords) {
		return store(tag, coords.getX(), coords.getY(), coords.getZ());
	}

	public static NBTTagCompound store(Vec3i coords) {
		return store(new NBTTagCompound(), coords.getX(), coords.getY(), coords.getZ());
	}

	public static NBTTagCompound store(NBTTagCompound tag, Coord coords) {
		return store(tag, coords.x, coords.y, coords.z);
	}

	public static NBTTagCompound store(Coord coords) {
		return store(new NBTTagCompound(), coords.x, coords.y, coords.z);
	}

	public static NBTTagCompound store(NBTTagCompound tag, BlockPos coords) {
		return store(tag, coords.getX(), coords.getY(), coords.getZ());
	}

	public static NBTTagCompound store(BlockPos coords) {
		return store(new NBTTagCompound(), coords.getX(), coords.getY(), coords.getZ());
	}

	public static NBTTagCompound store(NBTTagCompound tag, UUID uuid) {
		tag.setLong("UUIDMost", uuid.getMostSignificantBits());
		tag.setLong("UUIDLeast", uuid.getLeastSignificantBits());
		return tag;
	}

	public static NBTTagCompound store(UUID uuid) {
		return store(new NBTTagCompound(), uuid);
	}

	public static NBTTagCompound store(Vec3d vec) {
		return store(vec.xCoord, vec.yCoord, vec.zCoord);
	}

	public static NBTTagCompound store(NBTTagCompound tag, ResourceLocation location) {
		tag.setString(KEY, location.getResourceDomain());
		tag.setString(VALUE, location.getResourcePath());
		return tag;
	}

	public static NBTTagCompound store(ResourceLocation location) {
		return store(new NBTTagCompound(), location);
	}

	public static Coord readCoord(NBTTagCompound tag) {
		final int x = tag.getInteger(TAG_X);
		final int y = tag.getInteger(TAG_Y);
		final int z = tag.getInteger(TAG_Z);
		return new Coord(x, y, z);
	}

	public static BlockPos readBlockPos(NBTTagCompound tag) {
		final int x = tag.getInteger(TAG_X);
		final int y = tag.getInteger(TAG_Y);
		final int z = tag.getInteger(TAG_Z);
		return new BlockPos(x, y, z);
	}

	public static Vec3d readVec(NBTTagCompound tag) {
		final double x = tag.getDouble(TAG_X);
		final double y = tag.getDouble(TAG_Y);
		final double z = tag.getDouble(TAG_Z);
		return new Vec3d(x, y, z);
	}

	public static UUID readUuid(NBTTagCompound tag) {
		final long most = tag.getLong("UUIDMost");
		final long least = tag.getLong("UUIDLeast");
		return new UUID(most, least);
	}

	public static ResourceLocation readResourceLocation(final NBTTagCompound entry) {
		final String domain = entry.getString(KEY);
		final String path = entry.getString(VALUE);
		final ResourceLocation blockLocation = new ResourceLocation(domain, path);
		return blockLocation;
	}

	public static <T extends Enum<T>> T readEnum(NBTTagCompound tag, String name, Class<T> cls) {
		if (tag.hasKey(name, Constants.NBT.TAG_ANY_NUMERIC)) {
			int ordinal = tag.getInteger(name);
			return EnumUtils.fromOrdinal(cls, ordinal);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> T readEnum(NBTTagCompound tag, String name, T defaultValue) {
		return Objects.firstNonNull(readEnum(tag, name, (Class<T>)defaultValue.getClass()), defaultValue);
	}
}
