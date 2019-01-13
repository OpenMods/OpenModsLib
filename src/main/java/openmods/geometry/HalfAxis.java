package openmods.geometry;

import net.minecraft.util.EnumFacing;

public enum HalfAxis {
	NEG_X("XN", EnumFacing.WEST),
	NEG_Y("YN", EnumFacing.DOWN),
	NEG_Z("ZN", EnumFacing.NORTH),
	POS_X("XP", EnumFacing.EAST),
	POS_Y("YP", EnumFacing.UP),
	POS_Z("ZP", EnumFacing.SOUTH);

	public final int x;
	public final int y;
	public final int z;

	public final EnumFacing dir;

	public final String shortName;

	HalfAxis(String shortName, EnumFacing dir) {
		this.x = dir.getFrontOffsetX();
		this.y = dir.getFrontOffsetY();
		this.z = dir.getFrontOffsetZ();

		this.dir = dir;
		this.shortName = shortName;
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

	public static HalfAxis fromEnumFacing(EnumFacing dir) {
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