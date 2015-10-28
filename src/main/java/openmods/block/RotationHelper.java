package openmods.block;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.Log;
import openmods.geometry.HalfAxis;
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

	public RotationHelper(BlockRotationMode mode, World world, int x, int y, int z) {
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

	public boolean rotateWithTool(ForgeDirection axis) {
		if (mode == BlockRotationMode.NONE) return false;

		final Orientation newOrientation = mode.calculateToolRotation(orientation, axis);
		if (newOrientation != null) {
			if (mode.isPlacementValid(newOrientation)) {
				return setOrientation(newOrientation);
			} else {
				Log.info("Invalid tool rotation: [%s] %s: (%d,%d,%d): %s->%s", mode, axis, x, y, z, orientation, newOrientation);
			}
		}

		return false;
	}

	public boolean rotateAroundAxis(HalfAxis axis) {
		if (mode == BlockRotationMode.NONE) return false;

		final Orientation newOrientation = orientation.rotateAround(axis);
		if (newOrientation != null) {
			if (mode.isPlacementValid(newOrientation)) {
				return setOrientation(newOrientation);
			} else {
				Log.info("Invalid rotation: [%s] %s: (%d,%d,%d): %s->%s", mode, axis, x, y, z, orientation, newOrientation);
			}
		}

		return false;
	}

	public boolean setOrientation(Orientation newOrientation) {
		if (newOrientation == orientation) return false;

		if (mode.isPlacementValid(newOrientation)) {
			final int dirPart = mode.toValue(newOrientation);
			final int newMeta = originalMeta | dirPart;
			world.setBlockMetadataWithNotify(x, y, z, newMeta, BlockNotifyFlags.ALL);
			this.orientation = newOrientation;
			return true;
		} else {
			Log.info("Invalid orientation change: [%s] (%d,%d,%d): %s->%s", mode, x, y, z, orientation, newOrientation);
			return false;
		}
	}

}
