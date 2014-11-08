package openmods.block;

import java.util.Set;

import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.ImmutableSet;

class Constants {
	public static final ForgeDirection[] NO_AXIS = {};
	public static final ForgeDirection[] SINGLE_AXIS = { ForgeDirection.UP, ForgeDirection.DOWN };
	public static final ForgeDirection[] THREE_AXIS = ForgeDirection.VALID_DIRECTIONS;
}

public enum BlockRotationMode {
	NONE(Constants.NO_AXIS, 0) {
		@Override
		public boolean isValid(ForgeDirection dir) {
			return false;
		}

		@Override
		public ForgeDirection fromValue(int value) {
			return ForgeDirection.UNKNOWN;
		}

		@Override
		public int toValue(ForgeDirection dir) {
			return 0;
		}
	},
	TWO_DIRECTIONS(Constants.SINGLE_AXIS, 1, ForgeDirection.WEST, ForgeDirection.NORTH) {
		@Override
		public ForgeDirection fromValue(int value) {
			return ((value & 1) == 0)? ForgeDirection.WEST : ForgeDirection.NORTH;
		}

		@Override
		public int toValue(ForgeDirection dir) {
			switch (dir) {
				case WEST:
				case EAST:
					return 0;
				case NORTH:
				case SOUTH:
					return 1;
				default:
					throw new IllegalArgumentException(dir.name());
			}
		}
	},
	FOUR_DIRECTIONS(Constants.SINGLE_AXIS, 2, ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST) {
		@Override
		public ForgeDirection fromValue(int value) {
			switch (value & 3) {
				case 0:
					return ForgeDirection.NORTH;
				case 1:
					return ForgeDirection.WEST;
				case 2:
					return ForgeDirection.SOUTH;
				case 3:
					return ForgeDirection.EAST;
				default:
					return ForgeDirection.UNKNOWN;
			}
		}

		@Override
		public int toValue(ForgeDirection dir) {
			switch (dir) {
				case NORTH:
					return 0;
				case WEST:
					return 1;
				case SOUTH:
					return 2;
				case EAST:
					return 3;
				default:
					throw new IllegalArgumentException(dir.name());
			}
		}
	},
	SIX_DIRECTIONS(Constants.THREE_AXIS, 3, ForgeDirection.VALID_DIRECTIONS) {
		@Override
		public ForgeDirection fromValue(int value) {
			return ForgeDirection.getOrientation(value & 7);
		}

		@Override
		public int toValue(ForgeDirection dir) {
			return dir.ordinal();
		}
	};

	private BlockRotationMode(ForgeDirection[] rotations, int bitCount, ForgeDirection... allowedDirections) {
		this.rotations = rotations;
		this.allowedDirections = ImmutableSet.copyOf(allowedDirections);
		this.bitCount = bitCount;
		this.mask = (1 << bitCount) - 1;
	}

	public final ForgeDirection[] rotations;

	private final Set<ForgeDirection> allowedDirections;

	public final int bitCount;

	public final int mask;

	public boolean isValid(ForgeDirection dir) {
		return allowedDirections.contains(dir);
	}

	public abstract ForgeDirection fromValue(int value);

	public abstract int toValue(ForgeDirection dir);
}