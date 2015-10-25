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

import com.google.common.collect.ImmutableSet;

public enum BlockRotationMode {
	/**
	 * No rotations - always oriented by world directions
	 */
	NONE(RotationAxis.NO_AXIS, IRendererSetup.NULL, 0) {
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
	TWO_DIRECTIONS(RotationAxis.THREE_AXIS, AxisYRotation.instance, 1, Orientation.XP_YP, Orientation.ZN_YP) {
		@Override
		public Orientation fromValue(int value) {
			return ((value & 1) == 0)? Orientation.ZN_YP : Orientation.XP_YP;
		}

		@Override
		public int toValue(Orientation dir) {
			switch (dir) {
				default:
				case ZN_YP:
					return 0;
				case XP_YP:
					return 1;
			}
		}

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
	THREE_DIRECTIONS(RotationAxis.THREE_AXIS, TopRotation.instance, 2, Orientation.XP_ZN, Orientation.YP_XN, Orientation.XP_YP) {
		@Override
		public Orientation fromValue(int value) {
			switch (value & 3) {
				case 0:
				default:
					return Orientation.XP_YP;
				case 1:
					return Orientation.YP_XN;
				case 2:
					return Orientation.XP_ZN;

			}
		}

		@Override
		public int toValue(Orientation dir) {
			switch (dir) {
				case XP_YP:
				default:
					return 0;
				case YP_XN:
					return 1;
				case XP_ZN:
					return 2;
			}
		}

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
	FOUR_DIRECTIONS(RotationAxis.THREE_AXIS, AxisYRotation.instance, 2, Orientation.XP_YP, Orientation.XN_YP, Orientation.ZP_YP, Orientation.ZN_YP) {
		@Override
		public Orientation fromValue(int value) {
			switch (value & 3) {
				case 0:
					return Orientation.XN_YP;
				default:
				case 1:
					return Orientation.ZP_YP;
				case 2:
					return Orientation.XP_YP;
				case 3:
					return Orientation.ZN_YP;
			}
		}

		@Override
		public int toValue(Orientation dir) {
			switch (dir) {
				case XP_YP:
					return 2;
				case ZN_YP:
					return 3;
				case XN_YP:
					return 0;
				case ZP_YP:
					return 1;
				default:
					throw new IllegalArgumentException(dir.name());
			}
		}

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
	SIX_DIRECTIONS(RotationAxis.THREE_AXIS, TopRotation.instance, 3, Orientation.XN_YN, Orientation.YN_XP, Orientation.XP_ZN, Orientation.XP_ZP, Orientation.YP_XN, Orientation.XP_YP) {
		@Override
		public Orientation fromValue(int value) {
			switch (value) {
				default:
				case 0:
					return Orientation.XN_YN;
				case 1:
					return Orientation.XP_YP;
				case 2:
					return Orientation.XP_ZN;
				case 3:
					return Orientation.XP_ZP;
				case 4:
					return Orientation.YP_XN;
				case 5:
					return Orientation.YN_XP;
			}
		}

		@Override
		public int toValue(Orientation dir) {
			switch (dir) {
				case XN_YN:
				default:
					return 0;
				case XP_YP:
					return 1;
				case XP_ZN:
					return 2;
				case XP_ZP:
					return 3;
				case YP_XN:
					return 4;
				case YN_XP:
					return 5;
			}
		}

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
	};

	private BlockRotationMode(ForgeDirection[] rotations, IRendererSetup rendererSetup, int bitCount, Orientation... validOrientations) {
		this.rotationAxes = rotations;
		this.rendererSetup = rendererSetup;
		this.validDirections = ImmutableSet.copyOf(validOrientations);
		this.bitCount = bitCount;
		this.mask = (1 << bitCount) - 1;

		textureTransform = setupTextureTransform(BlockTextureTransform.builder()).build();
	}

	public final ForgeDirection[] rotationAxes;

	public final IRendererSetup rendererSetup;

	public final Set<Orientation> validDirections;

	public final int bitCount;

	public final int mask;

	public final BlockTextureTransform textureTransform;

	protected abstract BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder);

	public abstract Orientation fromValue(int value);

	public abstract int toValue(Orientation dir);

	public abstract Orientation getPlacementOrientationFromSurface(ForgeDirection side);

	public abstract Orientation getPlacementOrientationFromEntity(EntityPlayer player);

	public boolean isPlacementValid(Orientation dir) {
		return validDirections.contains(dir);
	}

	public abstract Orientation calculateToolRotation(Orientation currentOrientation, ForgeDirection axis);
}