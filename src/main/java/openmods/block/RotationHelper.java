package openmods.block;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.utils.BlockNotifyFlags;

public class RotationHelper {

	private final BlockRotationMode mode;
	private final int originalMeta;
	private ForgeDirection rotation;

	private final World world;
	private final int x;
	private final int y;
	private final int z;

	public static boolean rotate(BlockRotationMode mode, World world, int x, int y, int z, ForgeDirection axis) {
		if (mode == BlockRotationMode.NONE) return false;
		return new RotationHelper(mode, world, x, y, z).rotate(axis);
	}

	private RotationHelper(BlockRotationMode mode, World world, int x, int y, int z) {
		this.mode = mode;

		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;

		int meta = world.getBlockMetadata(x, y, z);
		this.originalMeta = (meta & ~mode.mask);
		int dirPart = (meta & mode.mask);

		this.rotation = mode.fromValue(dirPart);
	}

	public boolean rotate(ForgeDirection axis) {
		ForgeDirection newRotation = mode.calculateRotation(rotation, axis);
		if (newRotation != null) {
			setBlockRotation(newRotation);
			return true;
		}

		return false;
	}

	private void setBlockRotation(ForgeDirection dir) {
		final int dirPart = mode.toValue(dir);
		final int newMeta = originalMeta | dirPart;
		world.setBlockMetadataWithNotify(x, y, z, newMeta, BlockNotifyFlags.ALL);
		this.rotation = dir;
	}

}
