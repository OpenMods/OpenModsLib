package openmods.block;

import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.geometry.BlockTextureTransform;
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
		public boolean isValid(ForgeDirection dir) {
			return true;
		}

		@Override
		public ForgeDirection fromValue(int value) {
			return ForgeDirection.UNKNOWN;
		}

		@Override
		public int toValue(ForgeDirection dir) {
			return 0;
		}

		@Override
		public ForgeDirection getPlacementDirectionFromSurface(ForgeDirection side) {
			return ForgeDirection.UNKNOWN;
		}

		@Override
		public ForgeDirection getPlacementDirectionFromEntity(EntityPlayer player) {
			return ForgeDirection.UNKNOWN;
		}

		@Override
		public ForgeDirection calculateRotation(ForgeDirection direction, ForgeDirection axis) {
			return null;
		}

		@Override
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST);
		}

		@Override
		public Orientation getBlockOrientation(ForgeDirection direction) {
			return Orientation.XP_YP;
		}
	},
	/**
	 * Two orientations - either N-S or W-E. Top side remains unchanged.
	 * Placement side will become local north or south.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	TWO_DIRECTIONS(RotationAxis.THREE_AXIS, AxisYRotation.instance, 1, ForgeDirection.WEST, ForgeDirection.NORTH) {
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

		private ForgeDirection narrowDirection(final ForgeDirection normalDir) {
			switch (normalDir) {
				case EAST:
				case WEST:
					return ForgeDirection.WEST;
				case NORTH:
				case SOUTH:
					return ForgeDirection.NORTH;
				default:
					return ForgeDirection.UNKNOWN;
			}
		}

		@Override
		public ForgeDirection getPlacementDirectionFromSurface(ForgeDirection side) {
			return narrowDirection(side);
		}

		@Override
		public ForgeDirection getPlacementDirectionFromEntity(EntityPlayer player) {
			final ForgeDirection playerOrientation = BlockUtils.get2dOrientation(player);
			return narrowDirection(playerOrientation);
		}

		@Override
		public ForgeDirection calculateRotation(ForgeDirection direction, ForgeDirection axis) {
			switch (axis) {
				case UP:
				case DOWN: {
					switch (direction) {
						case EAST:
						case WEST:
							return ForgeDirection.NORTH;
						case NORTH:
						case SOUTH:
						default:
							return ForgeDirection.WEST;
					}
				}
				case NORTH:
				case SOUTH:
					return ForgeDirection.NORTH;
				case EAST:
				case WEST:
					return ForgeDirection.WEST;
				default:
					return null;
			}
		}

		@Override
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST);
		}

		@Override
		public Orientation getBlockOrientation(ForgeDirection localNorth) {
			switch (localNorth) {
				case NORTH:
				case SOUTH:
				default:
					return Orientation.XP_YP;
				case EAST:
				case WEST:
					return Orientation.ZN_YP;
			}
		}

	},
	/**
	 * Three orientations: N-S, W-E, T-B.
	 * Placement side will become local top or bottom.
	 * Tool rotation will set top direction to clicked side.
	 */
	THREE_DIRECTIONS(RotationAxis.THREE_AXIS, TopRotation.instance, 2, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.UP) {
		@Override
		public ForgeDirection fromValue(int value) {
			switch (value & 3) {
				case 0:
					return ForgeDirection.UP;
				case 1:
					return ForgeDirection.WEST;
				case 2:
					return ForgeDirection.NORTH;
				default:
					return ForgeDirection.UNKNOWN;
			}
		}

		@Override
		public int toValue(ForgeDirection dir) {
			switch (dir) {
				case UP:
				case DOWN:
					return 0;
				case WEST:
				case EAST:
					return 1;
				case NORTH:
				case SOUTH:
					return 2;
				default:
					throw new IllegalArgumentException(dir.name());
			}
		}

		private ForgeDirection narrowDirection(ForgeDirection dir) {
			switch (dir) {
				case EAST:
				case WEST:
					return ForgeDirection.WEST;
				case NORTH:
				case SOUTH:
					return ForgeDirection.NORTH;
				case UP:
				case DOWN:
					return ForgeDirection.UP;
				default:
					return ForgeDirection.UNKNOWN;
			}
		}

		@Override
		public ForgeDirection getPlacementDirectionFromSurface(ForgeDirection side) {
			return narrowDirection(side);
		}

		@Override
		public ForgeDirection getPlacementDirectionFromEntity(EntityPlayer player) {
			final ForgeDirection normalDir = BlockUtils.get3dOrientation(player);
			return narrowDirection(normalDir);
		}

		@Override
		public ForgeDirection calculateRotation(ForgeDirection direction, ForgeDirection axis) {
			switch (axis) {
				case EAST:
				case WEST:
					return ForgeDirection.WEST;
				case NORTH:
				case SOUTH:
					return ForgeDirection.NORTH;
				case UP:
				case DOWN:
					return ForgeDirection.UP;
				default:
					return null;
			}
		}

		@Override
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST).mirrorU(ForgeDirection.DOWN);
		}

		@Override
		public Orientation getBlockOrientation(ForgeDirection localTop) {
			switch (localTop) {
				case NORTH:
				case SOUTH:
					return Orientation.XP_ZN;
				case EAST:
				case WEST:
					return Orientation.YP_XN;
				case UP:
				case DOWN:
				default:
					return Orientation.XP_YP;
			}
		}
	},
	/**
	 * Rotate around Y in for directions: N,S,W,E.
	 * Placement side will become local north.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	FOUR_DIRECTIONS(RotationAxis.THREE_AXIS, AxisYRotation.instance, 2, ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST) {
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

		@Override
		public ForgeDirection getPlacementDirectionFromSurface(ForgeDirection side) {
			switch (side) {
				case NORTH:
				case EAST:
				case SOUTH:
				case WEST:
					return side;
				default:
					return ForgeDirection.UNKNOWN;
			}
		}

		@Override
		public ForgeDirection getPlacementDirectionFromEntity(EntityPlayer player) {
			return BlockUtils.get2dOrientation(player).getOpposite();
		}

		@Override
		public ForgeDirection calculateRotation(ForgeDirection direction, ForgeDirection axis) {
			switch (axis) {
				case UP:
				case DOWN:
					return direction.getRotation(axis);
				case NORTH:
				case SOUTH:
				case EAST:
				case WEST:
					return axis;
				default:
					return null;
			}
		}

		@Override
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST);
		}

		@Override
		public Orientation getBlockOrientation(ForgeDirection localNorth) {
			switch (localNorth) {
				case NORTH:
				default:
					return Orientation.XP_YP;
				case EAST:
					return Orientation.ZP_YP;
				case SOUTH:
					return Orientation.XN_YP;
				case WEST:
					return Orientation.ZN_YP;
			}
		}
	},
	/**
	 * Rotations in every direction.
	 * Placement side will become local top.
	 * Tool rotation will set top to clicked side.
	 */
	SIX_DIRECTIONS(RotationAxis.THREE_AXIS, TopRotation.instance, 3, ForgeDirection.VALID_DIRECTIONS) {
		@Override
		public ForgeDirection fromValue(int value) {
			return ForgeDirection.getOrientation(value & 7);
		}

		@Override
		public int toValue(ForgeDirection dir) {
			return dir.ordinal();
		}

		@Override
		public ForgeDirection getPlacementDirectionFromSurface(ForgeDirection side) {
			return side;
		}

		@Override
		public ForgeDirection getPlacementDirectionFromEntity(EntityPlayer player) {
			return BlockUtils.get3dOrientation(player).getOpposite();
		}

		@Override
		public ForgeDirection calculateRotation(ForgeDirection direction, ForgeDirection axis) {
			return axis;
		}

		@Override
		protected BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder) {
			return builder.mirrorU(ForgeDirection.NORTH).mirrorU(ForgeDirection.EAST).mirrorU(ForgeDirection.DOWN);
		}

		@Override
		public Orientation getBlockOrientation(ForgeDirection localTop) {
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
	};

	private BlockRotationMode(ForgeDirection[] rotations, IRendererSetup rendererSetup, int bitCount, ForgeDirection... validDirections) {
		this.rotations = rotations;
		this.rendererSetup = rendererSetup;
		this.validDirections = ImmutableSet.copyOf(validDirections);
		this.bitCount = bitCount;
		this.mask = (1 << bitCount) - 1;

		textureTransform = setupTextureTransform(BlockTextureTransform.builder()).build();
	}

	public final ForgeDirection[] rotations;

	public final IRendererSetup rendererSetup;

	public final Set<ForgeDirection> validDirections;

	public final int bitCount;

	public final int mask;

	public final BlockTextureTransform textureTransform;

	public boolean isValid(ForgeDirection dir) {
		return validDirections.contains(dir);
	}

	protected abstract BlockTextureTransform.Builder setupTextureTransform(BlockTextureTransform.Builder builder);

	public abstract ForgeDirection fromValue(int value);

	public abstract int toValue(ForgeDirection dir);

	public abstract ForgeDirection getPlacementDirectionFromSurface(ForgeDirection side);

	public abstract ForgeDirection getPlacementDirectionFromEntity(EntityPlayer player);

	public abstract ForgeDirection calculateRotation(ForgeDirection direction, ForgeDirection axis);

	public abstract Orientation getBlockOrientation(ForgeDirection direction);

	public ForgeDirection mapWorldToBlockSide(ForgeDirection rotation, ForgeDirection side) {
		return getBlockOrientation(rotation).globalToLocalDirection(side);
	}
}