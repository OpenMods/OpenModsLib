package openmods.block;

import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.geometry.BlockTextureTransform;
import openmods.geometry.HalfAxis;
import openmods.geometry.Orientation;
import openmods.renderer.rotations.IRendererSetup;
import openmods.renderer.rotations.RendererSetupProxy;
import openmods.utils.BlockUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public enum BlockRotationMode {
	/**
	 * No rotations - always oriented by world directions
	 */
	NONE(RotationAxis.NO_AXIS) {
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

		@Override
		public IRendererSetup getRenderSetup() {
			return RendererSetupProxy.NULL;
		}
	},
	/**
	 * Two orientations - either N-S or W-E. Top side remains unchanged.
	 * Placement side will become local north or south.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	TWO_DIRECTIONS(RotationAxis.THREE_AXIS, Orientation.ZN_YP, Orientation.XP_YP) {
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
				case DOWN:
					return (currentOrientation == Orientation.ZN_YP)? Orientation.XP_YP : Orientation.ZN_YP;
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

		@Override
		public IRendererSetup getRenderSetup() {
			return RendererSetupProxy.proxy.getVanillaRenderer();
		}
	},
	/**
	 * Three orientations: N-S, W-E, T-B.
	 * Placement side will become local top or bottom.
	 * Tool rotation will set top direction to clicked side.
	 */
	THREE_DIRECTIONS(RotationAxis.THREE_AXIS, Orientation.XP_YP, Orientation.YP_XN, Orientation.XP_ZN) {
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

		@Override
		public IRendererSetup getRenderSetup() {
			return RendererSetupProxy.proxy.getTweakedRenderer();
		}
	},
	/**
	 * Rotate around Y in for directions: N,S,W,E.
	 * Placement side will become local north.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	FOUR_DIRECTIONS(RotationAxis.THREE_AXIS, Orientation.XP_YP, Orientation.ZN_YP, Orientation.XN_YP, Orientation.ZP_YP) {
		private Orientation directionToOrientation(ForgeDirection side) {
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

		@Override
		public IRendererSetup getRenderSetup() {
			return RendererSetupProxy.proxy.getVanillaRenderer();
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

		@Override
		public IRendererSetup getRenderSetup() {
			return RendererSetupProxy.proxy.getTweakedRenderer();
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

		@Override
		public IRendererSetup getRenderSetup() {
			return RendererSetupProxy.proxy.getTweakedRenderer();
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

		public Orientation directionToOrientation(ForgeDirection localTop) {
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
		public Orientation getPlacementOrientationFromSurface(ForgeDirection side) {
			return directionToOrientation(side);
		}

		@Override
		public Orientation getPlacementOrientationFromEntity(EntityPlayer player) {
			ForgeDirection playerDir = BlockUtils.get2dOrientation(player).getOpposite();
			if (player.rotationPitch > 45.5F) {
				return Orientation.lookupYZ(HalfAxis.POS_Y, HalfAxis.fromDirection(playerDir));
			} else if (player.rotationPitch < -45.5F) {
				return Orientation.lookupYZ(HalfAxis.NEG_Y, HalfAxis.fromDirection(playerDir));
			} else {
				return Orientation.lookupYZ(HalfAxis.fromDirection(playerDir), HalfAxis.NEG_Y);
			}
		}

		@Override
		public Orientation calculateToolRotation(Orientation currentOrientation, ForgeDirection axis) {
			switch (axis) {
				case NORTH:
				case SOUTH:
				case EAST:
				case WEST:
					return Orientation.lookupYZ(HalfAxis.fromDirection(axis), HalfAxis.NEG_Y);
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

		@Override
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST).mirrorU(ForgeDirection.DOWN);
		}

		@Override
		public IRendererSetup getRenderSetup() {
			return RendererSetupProxy.proxy.getTweakedRenderer();
		}

	};

	private static final int MAX_ORIENTATIONS = 16;

	private BlockRotationMode(ForgeDirection[] rotations, Orientation... validOrientations) {
		this.rotationAxes = rotations;
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

	public final Set<Orientation> validDirections;

	public final int bitCount;

	public final int mask;

	public final BlockTextureTransform textureTransform;

	protected abstract BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder);

	public abstract IRendererSetup getRenderSetup();

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

	public Orientation getInventoryRenderOrientation() {
		return Orientation.XP_YP;
	}
}