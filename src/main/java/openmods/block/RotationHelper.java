package openmods.block;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.Log;
import openmods.geometry.Orientation;
import openmods.utils.BlockNotifyFlags;

public class RotationHelper {

	private final BlockRotationMode mode;
	private final int originalMeta;
	private Orientation orientation;

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

		this.orientation = mode.fromValue(dirPart);
	}

	public boolean rotate(ForgeDirection axis) {
		final Orientation newOrientation = mode.calculateToolRotation(orientation, axis);
		if (newOrientation != null) {
			if (mode.isPlacementValid(newOrientation)) {
				setBlockOrientation(newOrientation);
				return true;
			} else {
				Log.info("Invalid rotation: %s: %s->%s", axis, orientation, newOrientation);
			}
		}

		return false;
	}

	private void setBlockOrientation(Orientation dir) {
		final int dirPart = mode.toValue(dir);
		final int newMeta = originalMeta | dirPart;
		world.setBlockMetadataWithNotify(x, y, z, newMeta, BlockNotifyFlags.ALL);
		this.orientation = dir;
	}

}
