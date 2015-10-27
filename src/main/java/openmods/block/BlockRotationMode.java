package openmods.block;

import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.geometry.BlockTextureTransform;
import openmods.geometry.HalfAxis;
import openmods.geometry.Orientation;
import openmods.renderer.rotations.AxisYRotation;
import openmods.renderer.rotations.IRendererSetup;
import openmods.renderer.rotations.TopRotation;
import openmods.utils.BlockUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public enum BlockRotationMode {
	/**
	 * No rotations - always oriented by world directions
	 */
	NONE(RotationAxis.NO_AXIS, IRendererSetup.NULL) {
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
		public Orientation getPlacementOrientationFromSurface(ForgeDirection side) {
			return Orientation.XP_YP;
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(EntityPlayer player) {
			return Orientation.XP_YP;
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, ForgeDirection axis) {
			return null;
		}

		@Override
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST);
		}
	},
	/**
	 * Two orientations - either N-S or W-E. Top side remains unchanged.
	 * Placement side will become local north or south.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	TWO_DIRECTIONS(RotationAxis.THREE_AXIS, AxisYRotation.instance, Orientation.ZN_YP, Orientation.XP_YP) {
		private Orientation directionToOrientation(final ForgeDirection normalDir) {
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
		public Orientation getPlacementOrientationFromSurface(ForgeDirection side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(EntityPlayer player) {
			final ForgeDirection playerOrientation = BlockUtils.get2dOrientation(player);
			return directionToOrientation(playerOrientation);
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, ForgeDirection axis) {
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
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST);
		}
	},
	/**
	 * Three orientations: N-S, W-E, T-B.
	 * Placement side will become local top or bottom.
	 * Tool rotation will set top direction to clicked side.
	 */
	THREE_DIRECTIONS(RotationAxis.THREE_AXIS, TopRotation.instance, Orientation.XP_YP, Orientation.YP_XN, Orientation.XP_ZN) {
		private Orientation directionToOrientation(ForgeDirection dir) {
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
		public Orientation getPlacementOrientationFromSurface(ForgeDirection side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(EntityPlayer player) {
			final ForgeDirection normalDir = BlockUtils.get3dOrientation(player);
			return directionToOrientation(normalDir);
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, ForgeDirection axis) {
			return directionToOrientation(axis);
		}

		@Override
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST).mirrorU(ForgeDirection.DOWN);
		}
	},
	/**
	 * Rotate around Y in for directions: N,S,W,E.
	 * Placement side will become local north.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	FOUR_DIRECTIONS(RotationAxis.THREE_AXIS, AxisYRotation.instance, Orientation.XP_YP, Orientation.ZN_YP, Orientation.XN_YP, Orientation.ZP_YP) {
		private Orientation directionToOrientation(ForgeDirection side) {
			switch (side) {
				case NORTH:
					return Orientation.XP_YP;
				case EAST:
					return Orientation.ZP_YP;
				case SOUTH:
					return Orientation.XN_YP;
				case WEST:
					return Orientation.ZN_YP;
				default:
					return null;
			}
		}

		@Override
		public Orientation getPlacementOrientationFromSurface(ForgeDirection side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(EntityPlayer player) {
			final ForgeDirection side = BlockUtils.get2dOrientation(player).getOpposite();
			return directionToOrientation(side);
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, ForgeDirection axis) {
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
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST);
		}
	},
	/**
	 * Rotations in every direction.
	 * Placement side will become local top.
	 * Tool rotation will set top to clicked side.
	 */
	SIX_DIRECTIONS(RotationAxis.THREE_AXIS, TopRotation.instance, Orientation.XN_YN, Orientation.XP_YP, Orientation.XP_ZN, Orientation.XP_ZP, Orientation.YP_XN, Orientation.YN_XP) {
		public Orientation directionToOrientation(ForgeDirection localTop) {
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
		public Orientation getPlacementOrientationFromSurface(ForgeDirection side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(EntityPlayer player) {
			final ForgeDirection localTop = BlockUtils.get3dOrientation(player).getOpposite();
			return directionToOrientation(localTop);
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, ForgeDirection axis) {
			return directionToOrientation(axis);
		}

		@Override
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST).mirrorU(ForgeDirection.DOWN);
		}
	},

	/**
	 * And now it's time for weird ones...
	 * Three orientations: N-S, W-E, T-B.
	 * Placement side will become local top or bottom.
	 * Side can be rotated in four directions
	 */
	THREE_FOUR_DIRECTIONS(RotationAxis.THREE_AXIS, TopRotation.instance,
			Orientation.XP_YP, Orientation.XN_YP, Orientation.ZP_YP, Orientation.ZN_YP,
			Orientation.YP_XN, Orientation.YN_XN, Orientation.ZP_XN, Orientation.ZN_XN,
			Orientation.XP_ZN, Orientation.XN_ZN, Orientation.YP_ZN, Orientation.YN_ZN) {

		private Orientation directionToOrientation(ForgeDirection dir) {
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
		public Orientation getPlacementOrientationFromSurface(ForgeDirection side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(EntityPlayer player) {
			final ForgeDirection normalDir = BlockUtils.get3dOrientation(player);
			return directionToOrientation(normalDir);
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, ForgeDirection axis) {
			final HalfAxis newTop = HalfAxis.fromDirection(axis);
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
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST).mirrorU(ForgeDirection.DOWN);
		}
	};

	private static final int MAX_ORIENTATIONS = 16;

	private BlockRotationMode(ForgeDirection[] rotations, IRendererSetup rendererSetup, Orientation... validOrientations) {
		this.rotationAxes = rotations;
		this.rendererSetup = rendererSetup;
		this.validDirections = ImmutableSet.copyOf(validOrientations);

		final int count = validOrientations.length;

		Preconditions.checkArgument(this.validDirections.size() == count, "Duplicated directions");

		Preconditions.checkArgument(count <= MAX_ORIENTATIONS, "Too many values: %s", count);

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

	public final ForgeDirection[] rotationAxes;

	public final IRendererSetup rendererSetup;

	public final Set<Orientation> validDirections;

	public final int bitCount;

	public final int mask;

	public final BlockTextureTransform textureTransform;

	protected abstract BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder);

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

	public abstract Orientation getPlacementOrientationFromSurface(ForgeDirection side);

	public abstract Orientation getPlacementOrientationFromEntity(EntityPlayer player);

	public abstract Orientation calculateToolRotation(Orientation currentOrientation, ForgeDirection axis);
}