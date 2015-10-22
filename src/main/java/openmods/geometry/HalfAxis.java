package openmods.geometry;

import net.minecraftforge.common.util.ForgeDirection;

public enum HalfAxis {
	NEG_X, NEG_Y, NEG_Z, POS_X, POS_Y, POS_Z;

	public static HalfAxis fromDirection(ForgeDirection dir) {
		switch (dir) {
			case EAST:
				return POS_X;
			case WEST:
				return NEG_X;
			case NORTH:
				return NEG_Z;
			case SOUTH:
				return POS_Z;
			case DOWN:
				return NEG_Y;
			case UP:
				return POS_Y;
			default:
				throw new IllegalArgumentException(dir.toString());
		}
	}
}