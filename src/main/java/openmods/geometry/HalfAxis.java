package openmods.geometry;

import net.minecraftforge.common.util.ForgeDirection;

public enum HalfAxis {
	NEG_X(ForgeDirection.WEST),
	NEG_Y(ForgeDirection.DOWN),
	NEG_Z(ForgeDirection.NORTH),
	POS_X(ForgeDirection.EAST),
	POS_Y(ForgeDirection.UP),
	POS_Z(ForgeDirection.SOUTH);

	public final int x;
	public final int y;
	public final int z;

	public final ForgeDirection dir;

	private HalfAxis(ForgeDirection dir) {
		this.x = dir.offsetX;
		this.y = dir.offsetY;
		this.z = dir.offsetZ;

		this.dir = dir;
	}

	public static final HalfAxis[] VALUES = values();

	private static final HalfAxis _ZERO = null;

	private static final HalfAxis[][] CROSS_PRODUCTS = new HalfAxis[][] {
			{ _ZERO, POS_Z, NEG_Y, _ZERO, NEG_Z, POS_Y }, // NEG_X
			{ NEG_Z, _ZERO, POS_X, POS_Z, _ZERO, NEG_X }, // NEG_Y
			{ POS_Y, NEG_X, _ZERO, NEG_Y, POS_X, _ZERO }, // NEG_Z
			{ _ZERO, NEG_Z, POS_Y, _ZERO, POS_Z, NEG_Y }, // POS_X
			{ POS_Z, _ZERO, NEG_X, NEG_Z, _ZERO, POS_X }, // POS_Y
			{ NEG_Y, POS_X, _ZERO, POS_Y, NEG_X, _ZERO }, // POS_Z
	};

	public static HalfAxis cross(HalfAxis a, HalfAxis b) {
		return CROSS_PRODUCTS[a.ordinal()][b.ordinal()];
	}

	public HalfAxis cross(HalfAxis other) {
		return cross(this, other);
	}

	private static final HalfAxis[] NEGATIONS = new HalfAxis[] {
			/* NEG_X = */POS_X,
			/* NEG_Y = */POS_Y,
			/* NEG_Z = */POS_Z,
			/* POS_X = */NEG_X,
			/* POS_Y = */NEG_Y,
			/* POS_Z = */NEG_Z,
	};

	public static HalfAxis negate(HalfAxis axis) {
		return NEGATIONS[axis.ordinal()];
	}

	public HalfAxis negate() {
		return negate(this);
	}

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