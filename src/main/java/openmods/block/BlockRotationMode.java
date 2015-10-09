package openmods.block;

import java.util.Set;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.utils.BlockUtils;

import com.google.common.collect.ImmutableSet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum BlockRotationMode {
	/**
	 * No rotations - always oriented by world directions
	 */
	NONE(RotationAxis.NO_AXIS, 0) {
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
		public ForgeDirection mapWorldToBlockSide(ForgeDirection localNorth, ForgeDirection side) {
			return side;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void setupBlockRenderer(RenderBlocks renderer, ForgeDirection rotation) {}
	},
	/**
	 * Two orientations - either N-S or W-E. Top side remains unchanged.
	 * Placement side will become local north or south.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	TWO_DIRECTIONS(RotationAxis.THREE_AXIS, 1, ForgeDirection.WEST, ForgeDirection.NORTH) {
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
		public ForgeDirection mapWorldToBlockSide(ForgeDirection localNorth, ForgeDirection side) {
			switch (localNorth) {
				case NORTH:
				case SOUTH:
				default:
					return side;
				case EAST:
				case WEST:
					return side.getRotation(ForgeDirection.UP);
			}
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void setupBlockRenderer(RenderBlocks renderer, ForgeDirection rotation) {
			switch (rotation) {
				case EAST:
				case WEST:
					renderer.uvRotateTop = 2;
					break;
				case SOUTH:
					renderer.uvRotateTop = 3;
					break;
				default:
					break;
			}
		}
	},
	/**
	 * Three orientations: N-S, W-E, T-B.
	 * Placement side will become local top or bottom.
	 * Tool rotation will set top direction to clicked side.
	 */
	THREE_DIRECTIONS(RotationAxis.THREE_AXIS, 2, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.UP) {
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
		public ForgeDirection mapWorldToBlockSide(ForgeDirection localTop, ForgeDirection side) {
			switch (localTop) {
				case NORTH:
				case SOUTH:
					return side.getRotation(ForgeDirection.WEST);
				case EAST:
				case WEST:
					return side.getRotation(ForgeDirection.NORTH);
				case UP:
				case DOWN:
				default:
					return side;
			}
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void setupBlockRenderer(RenderBlocks renderer, ForgeDirection rotation) {
			switch (rotation) {
				case WEST:
				case EAST:
					renderer.uvRotateTop = 1;
					renderer.uvRotateBottom = 2;
					renderer.uvRotateWest = 1;
					renderer.uvRotateEast = 2;
					break;
				case NORTH:
				case SOUTH:
					renderer.uvRotateNorth = 2;
					renderer.uvRotateSouth = 1;
					break;
				case UP:
				case DOWN:
				default:
					break;

			}
		}

	},
	/**
	 * Rotate around Y in for directions: N,S,W,E.
	 * Placement side will become local north.
	 * Tool rotation will either rotate around Y (if clicked T or B) or set to clicked side (otherwise).
	 */
	FOUR_DIRECTIONS(RotationAxis.THREE_AXIS, 2, ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST) {
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
		public ForgeDirection mapWorldToBlockSide(ForgeDirection localNorth, ForgeDirection side) {
			switch (localNorth) {
				case NORTH:
				default:
					return side;
				case EAST:
					return side.getRotation(ForgeDirection.DOWN);
				case SOUTH:
					return side.getRotation(ForgeDirection.UP).getRotation(ForgeDirection.UP);
				case WEST:
					return side.getRotation(ForgeDirection.UP);
			}
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void setupBlockRenderer(RenderBlocks renderer, ForgeDirection rotation) {
			switch (rotation) {
				case EAST:
					renderer.uvRotateTop = 1;
					break;
				case WEST:
					renderer.uvRotateTop = 2;
					break;
				case SOUTH:
					renderer.uvRotateTop = 3;
					break;
				default:
					break;
			}
		}
	},
	/**
	 * Rotations in every direction.
	 * Placement side will become local top.
	 * Tool rotation will set top to clicked side.
	 */
	SIX_DIRECTIONS(RotationAxis.THREE_AXIS, 3, ForgeDirection.VALID_DIRECTIONS) {
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
		public ForgeDirection mapWorldToBlockSide(ForgeDirection localNorth, ForgeDirection side) {
			switch (localNorth) {
				case DOWN:
					return side.getRotation(ForgeDirection.SOUTH).getRotation(ForgeDirection.SOUTH);
				case EAST:
					return side.getRotation(ForgeDirection.NORTH);
				case NORTH:
					return side.getRotation(ForgeDirection.WEST);
				case SOUTH:
					return side.getRotation(ForgeDirection.EAST);
				case WEST:
					return side.getRotation(ForgeDirection.SOUTH);
				case UP:
				default:
					return side;
			}
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void setupBlockRenderer(RenderBlocks renderer, ForgeDirection rotation) {
			switch (rotation) {
				case DOWN:
					renderer.uvRotateSouth = 3;
					renderer.uvRotateNorth = 3;
					renderer.uvRotateEast = 3;
					renderer.uvRotateWest = 3;
					break;
				case EAST:
					renderer.uvRotateTop = 1;
					renderer.uvRotateBottom = 2;
					renderer.uvRotateWest = 1;
					renderer.uvRotateEast = 2;
					break;
				case NORTH:
					renderer.uvRotateNorth = 2;
					renderer.uvRotateSouth = 1;
					break;
				case SOUTH:
					renderer.uvRotateTop = 3;
					renderer.uvRotateBottom = 3;
					renderer.uvRotateNorth = 1;
					renderer.uvRotateSouth = 2;
					break;
				case UP:
					break;
				case WEST:
					renderer.uvRotateTop = 2;
					renderer.uvRotateBottom = 1;
					renderer.uvRotateWest = 2;
					renderer.uvRotateEast = 1;
					break;
				default:
					break;

			}
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

	public abstract ForgeDirection getPlacementDirectionFromSurface(ForgeDirection side);

	public abstract ForgeDirection getPlacementDirectionFromEntity(EntityPlayer player);

	public abstract ForgeDirection calculateRotation(ForgeDirection direction, ForgeDirection axis);

	public abstract ForgeDirection mapWorldToBlockSide(ForgeDirection rotation, ForgeDirection side);

	@SideOnly(Side.CLIENT)
	public abstract void setupBlockRenderer(RenderBlocks renderer, ForgeDirection rotation);
}