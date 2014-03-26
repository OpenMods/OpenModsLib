package openmods.block;

import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import openmods.utils.BlockNotifyFlags;

public class RotationHelper {

	public static boolean rotateBlock(OpenBlock block, World world, int x, int y, int z, ForgeDirection axis) {
		switch (block.getRotationMode()) {
			case FOUR_DIRECTIONS:
				return rotateFourDirections(world, x, y, z, axis);
			case SIX_DIRECTIONS:
				return rotateSixDirection(world, x, y, z, axis);
			case TWENTYFOUR_DIRECTIONS:
				return rotateTwentyFourDirections(world, x, y, z, axis);
			case NONE:
			default:
				return false;
		}
	}

	private static void rotate(World world, int x, int y, int z, ForgeDirection axis) {
		ForgeDirection dir = getBlockRotation(world, x, y, z);
		ForgeDirection rotatedDir = dir.getRotation(axis);
		world.setBlockMetadataWithNotify(x, y, z, rotatedDir.ordinal(), BlockNotifyFlags.ALL);
	}

	protected static ForgeDirection getBlockRotation(World world, int x, int y, int z) {
		int currentMeta = world.getBlockMetadata(x, y, z);
		ForgeDirection dir = ForgeDirection.getOrientation(currentMeta);
		return dir;
	}

	private static boolean rotateFourDirections(World world, int x, int y, int z, ForgeDirection axis) {
		switch (axis) {
			case UP:
			case DOWN:
				rotate(world, x, y, z, axis);
				return true;
			default:
				return false;
		}
	}

	private static boolean rotateSixDirection(World world, int x, int y, int z, ForgeDirection axis) {
		rotate(world, x, y, z, axis);
		return true;
	}

	private static boolean rotateTwentyFourDirections(World world, int x, int y, int z, ForgeDirection axis) {
		/*
		 * We never finished implementation of that one, so I'm not doing
		 * guesswork here. One possible way to do it is to rotate side, if axis
		 * == main rotation, and change orientation otherwise
		 */
		return false;
	}

}
