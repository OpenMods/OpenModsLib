package openmods.block;

import java.util.Set;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import openmods.geometry.BlockTextureTransform;
import openmods.geometry.HalfAxis;
import openmods.geometry.Orientation;
import openmods.utils.BlockUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public enum BlockRotationMode {
	/**
	 * No rotations - always oriented by world directions
	 */
	NONE(RotationAxis.NO_AXIS, Orientation.XP_YP) {
		@Override
		public boolean isPlacementValid(Orientation dir) {
			return true;
		}

		@Override
		public Orientation fromValue(int value) {
			return Orientation.XP_YP;
		}

		@Override
		public int toValue(Orientation dir) {
			return 0;
		}

		@Override
		public Orientation getPlacementOrientationFromSurface(BlockPos pos, EnumFacing side) {
			return Orientation.XP_YP;
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(BlockPos pos, EntityLivingBase player) {
			return Orientation.XP_YP;
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, EnumFacing axis) {
			return null;
		}
	},
	/**
	 * Two orientations - either N-S or W-E. Top side remains unchanged.
	 * Placement side will become local north or south.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	TWO_DIRECTIONS(RotationAxis.THREE_AXIS, Orientation.ZN_YP, Orientation.XP_YP) {
		private Orientation directionToOrientation(final EnumFacing normalDir) {
			switch (normalDir) {
				case EAST:
				case WEST:
					return Orientation.ZN_YP;
				case NORTH:
				case SOUTH:
					return Orientation.XP_YP;
				default:
					return null;
			}
		}

		@Override
		public Orientation getPlacementOrientationFromSurface(BlockPos pos, EnumFacing side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(BlockPos pos, EntityLivingBase player) {
			return directionToOrientation(player.getHorizontalFacing());
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, EnumFacing axis) {
			switch (axis) {
				case UP:
					return currentOrientation.rotateAround(HalfAxis.POS_Y);
				case DOWN:
					return currentOrientation.rotateAround(HalfAxis.NEG_Y);
				case NORTH:
				case SOUTH:
				case EAST:
				case WEST:
					return directionToOrientation(axis);
				default:
					return null;
			}
		}
	},
	/**
	 * Three orientations: N-S, W-E, T-B.
	 * Placement side will become local top or bottom.
	 * Tool rotation will set top direction to clicked side.
	 */
	THREE_DIRECTIONS(RotationAxis.THREE_AXIS, Orientation.XP_YP, Orientation.YP_XN, Orientation.XP_ZN) {
		private Orientation directionToOrientation(EnumFacing dir) {
			switch (dir) {
				case EAST:
				case WEST:
					return Orientation.YP_XN;
				case NORTH:
				case SOUTH:
					return Orientation.XP_ZN;
				case UP:
				case DOWN:
				default:
					return Orientation.XP_YP;
			}
		}

		@Override
		public Orientation getPlacementOrientationFromSurface(BlockPos pos, EnumFacing side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(BlockPos pos, EntityLivingBase player) {
			final EnumFacing normalDir = BlockUtils.get3dOrientation(player, pos);
			return directionToOrientation(normalDir);
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, EnumFacing axis) {
			return directionToOrientation(axis);
		}
	},
	/**
	 * Rotate around Y in for directions: N,S,W,E.
	 * Placement side will become local north.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	FOUR_DIRECTIONS(RotationAxis.THREE_AXIS, Orientation.XP_YP, Orientation.ZN_YP, Orientation.XN_YP, Orientation.ZP_YP) {
		private Orientation directionToOrientation(EnumFacing side) {
			switch (side) {
				case SOUTH:
					return Orientation.XP_YP;
				case WEST:
					return Orientation.ZP_YP;
				case NORTH:
					return Orientation.XN_YP;
				case EAST:
					return Orientation.ZN_YP;
				default:
					return null;
			}
		}

		@Override
		public Orientation getPlacementOrientationFromSurface(BlockPos pos, EnumFacing side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(BlockPos pos, EntityLivingBase player) {
			final EnumFacing side = player.getHorizontalFacing().getOpposite();
			return directionToOrientation(side);
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, EnumFacing axis) {
			switch (axis) {
				case UP:
					return currentOrientation.rotateAround(HalfAxis.POS_Y);
				case DOWN:
					return currentOrientation.rotateAround(HalfAxis.NEG_Y);
				case NORTH:
				case SOUTH:
				case EAST:
				case WEST:
					return directionToOrientation(axis);
				default:
					return null;
			}
		}

		@Override
		public Orientation getInventoryRenderOrientation() {
			return Orientation.XN_YP;
		}
	},
	/**
	 * Rotations in every direction.
	 * Placement side will become local top.
	 * Tool rotation will set top to clicked side.
	 */
	SIX_DIRECTIONS(RotationAxis.THREE_AXIS, Orientation.XN_YN, Orientation.XP_YP, Orientation.XP_ZN, Orientation.XP_ZP, Orientation.YP_XN, Orientation.YN_XP) {
		public Orientation directionToOrientation(EnumFacing localTop) {
			switch (localTop) {
				case DOWN:
					return Orientation.XN_YN;
				case EAST:
					return Orientation.YN_XP;
				case NORTH:
					return Orientation.XP_ZN;
				case SOUTH:
					return Orientation.XP_ZP;
				case WEST:
					return Orientation.YP_XN;
				case UP:
				default:
					return Orientation.XP_YP;
			}
		}

		@Override
		public Orientation getPlacementOrientationFromSurface(BlockPos pos, EnumFacing side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(BlockPos pos, EntityLivingBase player) {
			final EnumFacing localTop = BlockUtils.get3dOrientation(player, pos).getOpposite();
			return directionToOrientation(localTop);
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, EnumFacing axis) {
			return directionToOrientation(axis);
		}

		@Override
		public Orientation getInventoryRenderOrientation() {
			return Orientation.YN_XP;
		}
	},

	/**
	 * And now it's time for weird ones...
	 * Three orientations: N-S, W-E, T-B.
	 * Placement side will become local top or bottom.
	 * Side can be rotated in four directions
	 */
	THREE_FOUR_DIRECTIONS(RotationAxis.THREE_AXIS,
			Orientation.XP_YP, Orientation.XN_YP, Orientation.ZP_YP, Orientation.ZN_YP,
			Orientation.YP_XN, Orientation.YN_XN, Orientation.ZP_XN, Orientation.ZN_XN,
			Orientation.XP_ZN, Orientation.XN_ZN, Orientation.YP_ZN, Orientation.YN_ZN) {

		private Orientation directionToOrientation(EnumFacing dir) {
			switch (dir) {
				case EAST:
				case WEST:
					return Orientation.YP_XN;
				case NORTH:
				case SOUTH:
					return Orientation.XP_ZN;
				case UP:
				case DOWN:
				default:
					return Orientation.XP_YP;
			}
		}

		@Override
		public Orientation getPlacementOrientationFromSurface(BlockPos pos, EnumFacing side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(BlockPos pos, EntityLivingBase player) {
			final EnumFacing normalDir = BlockUtils.get3dOrientation(player, pos);
			return directionToOrientation(normalDir);
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, EnumFacing axis) {
			final HalfAxis newTop = HalfAxis.fromEnumFacing(axis);
			final HalfAxis currentTop = currentOrientation.y;

			if (newTop == currentTop) {
				return currentOrientation.rotateAround(HalfAxis.POS_Y);
			} else if (newTop == currentTop.negate()) {
				return currentOrientation.rotateAround(HalfAxis.NEG_Y);
			} else {
				return directionToOrientation(axis);
			}
		}

		@Override
		public Orientation getInventoryRenderOrientation() {
			return Orientation.XN_YP;
		}
	},
	/**
	 * Yet another weird one.
	 * Top side can rotate when oriented up or down.
	 * When top points to cardinal direction, texture top should always align with horizon
	 */
	TWELVE_DIRECTIONS(RotationAxis.THREE_AXIS,
			Orientation.lookupYZ(HalfAxis.NEG_Y, HalfAxis.NEG_Z), // first two TOP/BOTTOM orientation are here for easy migration from SIX_DIRECTIONS
			Orientation.lookupYZ(HalfAxis.POS_Y, HalfAxis.POS_Z),

			Orientation.lookupYZ(HalfAxis.NEG_Z, HalfAxis.NEG_Y),
			Orientation.lookupYZ(HalfAxis.POS_Z, HalfAxis.NEG_Y),
			Orientation.lookupYZ(HalfAxis.NEG_X, HalfAxis.NEG_Y),
			Orientation.lookupYZ(HalfAxis.POS_X, HalfAxis.NEG_Y),

			Orientation.lookupYZ(HalfAxis.NEG_Y, HalfAxis.POS_Z),
			Orientation.lookupYZ(HalfAxis.NEG_Y, HalfAxis.NEG_X),
			Orientation.lookupYZ(HalfAxis.NEG_Y, HalfAxis.POS_X),

			Orientation.lookupYZ(HalfAxis.POS_Y, HalfAxis.NEG_Z),
			Orientation.lookupYZ(HalfAxis.POS_Y, HalfAxis.POS_X),
			Orientation.lookupYZ(HalfAxis.POS_Y, HalfAxis.NEG_X)) {

		public Orientation directionToOrientation(EnumFacing localTop) {
			switch (localTop) {
				case DOWN:
					return Orientation.lookupYZ(HalfAxis.NEG_Y, HalfAxis.NEG_Z);
				case EAST:
					return Orientation.lookupYZ(HalfAxis.POS_X, HalfAxis.NEG_Y);
				case NORTH:
					return Orientation.lookupYZ(HalfAxis.NEG_Z, HalfAxis.NEG_Y);
				case SOUTH:
					return Orientation.lookupYZ(HalfAxis.POS_Z, HalfAxis.NEG_Y);
				case WEST:
					return Orientation.lookupYZ(HalfAxis.NEG_X, HalfAxis.NEG_Y);
				case UP:
				default:
					return Orientation.lookupYZ(HalfAxis.POS_Y, HalfAxis.POS_Z);
			}
		}

		@Override
		public Orientation getPlacementOrientationFromSurface(BlockPos pos, EnumFacing side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(BlockPos pos, EntityLivingBase player) {
			EnumFacing playerDir = player.getHorizontalFacing().getOpposite();
			if (player.rotationPitch > 45.5F) {
				return Orientation.lookupYZ(HalfAxis.POS_Y, HalfAxis.fromEnumFacing(playerDir));
			} else if (player.rotationPitch < -45.5F) {
				return Orientation.lookupYZ(HalfAxis.NEG_Y, HalfAxis.fromEnumFacing(playerDir));
			} else {
				return Orientation.lookupYZ(HalfAxis.fromEnumFacing(playerDir), HalfAxis.NEG_Y);
			}
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, EnumFacing axis) {
			switch (axis) {
				case NORTH:
				case SOUTH:
				case EAST:
				case WEST:
					return Orientation.lookupYZ(HalfAxis.fromEnumFacing(axis), HalfAxis.NEG_Y);
				case UP:
					if (currentOrientation.y != HalfAxis.POS_Y) return Orientation.lookupYZ(HalfAxis.POS_Y, HalfAxis.POS_Z);
					else return currentOrientation.rotateAround(HalfAxis.POS_Y);
				case DOWN:
					if (currentOrientation.y != HalfAxis.NEG_Y) return Orientation.lookupYZ(HalfAxis.NEG_Y, HalfAxis.NEG_Z);
					else return currentOrientation.rotateAround(HalfAxis.POS_Y);
				default:
					return null;
			}
		}

	};

	private static final int MAX_ORIENTATIONS = 16;

	private BlockRotationMode(EnumFacing[] rotations, Orientation... validOrientations) {
		this.rotationAxes = rotations;
		this.validDirections = ImmutableSet.copyOf(validOrientations);

		final int count = validOrientations.length;

		Preconditions.checkArgument(this.validDirections.size() == count, "Duplicated directions");
		Preconditions.checkArgument(count <= MAX_ORIENTATIONS, "Too many values: %s", count);

		this.property = PropertyEnum.create("orientation", Orientation.class, validDirections);

		this.idToOrientation = new Orientation[MAX_ORIENTATIONS];
		this.orientationToId = new int[Orientation.VALUES.length];

		for (int i = 0; i < count; i++) {
			final Orientation orientation = validOrientations[i];
			Preconditions.checkNotNull(orientation);
			idToOrientation[i] = orientation;
			orientationToId[orientation.ordinal()] = i;
		}

		if (count == 0) {
			this.bitCount = 0;
			this.mask = 0;
		} else {
			final int maxValue = count - 1;
			this.bitCount = Integer.SIZE - Integer.numberOfLeadingZeros(maxValue);
			this.mask = (1 << bitCount) - 1;

			for (int i = count; i < idToOrientation.length; i++)
				idToOrientation[i] = idToOrientation[0];
		}

		this.textureTransform = setupTextureTransform(BlockTextureTransform.builder()).build();
	}

	private final Orientation[] idToOrientation;

	private final int[] orientationToId;

	public final EnumFacing[] rotationAxes;

	public final Set<Orientation> validDirections;

	public final int bitCount;

	public final int mask;

	public final BlockTextureTransform textureTransform;

	public final PropertyEnum<Orientation> property;

	protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
		return builder.mirrorU(EnumFacing.NORTH).mirrorU(EnumFacing.EAST).mirrorV(EnumFacing.DOWN);
	}

	public Orientation fromValue(int value) {
		try {
			return idToOrientation[value];
		} catch (IndexOutOfBoundsException e) {
			return idToOrientation[0];
		}
	}

	public int toValue(Orientation dir) {
		try {
			return orientationToId[dir.ordinal()];
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
	}

	public boolean isPlacementValid(Orientation dir) {
		return validDirections.contains(dir);
	}

	public abstract Orientation getPlacementOrientationFromSurface(BlockPos pos, EnumFacing side);

	public abstract Orientation getPlacementOrientationFromEntity(BlockPos pos, EntityLivingBase player);

	public abstract Orientation calculateToolRotation(Orientation currentOrientation, EnumFacing axis);

	public Orientation getInventoryRenderOrientation() {
		return Orientation.XP_YP;
	}
}