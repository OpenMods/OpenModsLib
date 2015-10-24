package openmods.geometry;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.base.Preconditions;

public enum Orientation {
	XN_YN(HalfAxis.NEG_X, HalfAxis.NEG_Y),
	XN_YP(HalfAxis.NEG_X, HalfAxis.POS_Y),
	XN_ZN(HalfAxis.NEG_X, HalfAxis.NEG_Z),
	XN_ZP(HalfAxis.NEG_X, HalfAxis.POS_Z),

	XP_YN(HalfAxis.POS_X, HalfAxis.NEG_Y),
	XP_YP(HalfAxis.POS_X, HalfAxis.POS_Y),
	XP_ZN(HalfAxis.POS_X, HalfAxis.NEG_Z),
	XP_ZP(HalfAxis.POS_X, HalfAxis.POS_Z),

	YN_ZP(HalfAxis.NEG_Y, HalfAxis.POS_Z),
	YN_XN(HalfAxis.NEG_Y, HalfAxis.NEG_X),
	YN_XP(HalfAxis.NEG_Y, HalfAxis.POS_X),
	YN_ZN(HalfAxis.NEG_Y, HalfAxis.NEG_Z),

	YP_XN(HalfAxis.POS_Y, HalfAxis.NEG_X),
	YP_XP(HalfAxis.POS_Y, HalfAxis.POS_X),
	YP_ZN(HalfAxis.POS_Y, HalfAxis.NEG_Z),
	YP_ZP(HalfAxis.POS_Y, HalfAxis.POS_Z),

	ZN_XP(HalfAxis.NEG_Z, HalfAxis.POS_X),
	ZN_XN(HalfAxis.NEG_Z, HalfAxis.NEG_X),
	ZN_YP(HalfAxis.NEG_Z, HalfAxis.POS_Y),
	ZN_YN(HalfAxis.NEG_Z, HalfAxis.NEG_Y),

	ZP_XN(HalfAxis.POS_Z, HalfAxis.NEG_X),
	ZP_XP(HalfAxis.POS_Z, HalfAxis.POS_X),
	ZP_YN(HalfAxis.POS_Z, HalfAxis.NEG_Y),
	ZP_YP(HalfAxis.POS_Z, HalfAxis.POS_Y);

	public static final Orientation[] VALUES = values();

	private static final TIntObjectMap<Orientation> LOOKUP = new TIntObjectHashMap<Orientation>(VALUES.length);

	private static final Orientation[][] ROTATIONS = new Orientation[VALUES.length][HalfAxis.VALUES.length];

	private static int lookupKey(HalfAxis x, HalfAxis y, HalfAxis z) {
		return (x.ordinal() << 6) | (y.ordinal() << 3) | (z.ordinal() << 0);
	}

	static {
		for (Orientation o : VALUES) {
			final int key = lookupKey(o.x, o.y, o.z);
			final Orientation prev = LOOKUP.put(key, o);
			Preconditions.checkState(prev == null, "Key %s duplicate: %s->%s", key, prev, o);
		}

		for (Orientation o : VALUES) {
			final int i = o.ordinal();
			ROTATIONS[i][HalfAxis.POS_X.ordinal()] = lookupNotNull(o.x, o.z.negate(), o.y);
			ROTATIONS[i][HalfAxis.NEG_X.ordinal()] = lookupNotNull(o.x, o.z, o.y.negate());

			ROTATIONS[i][HalfAxis.POS_Y.ordinal()] = lookupNotNull(o.z, o.y, o.x.negate());
			ROTATIONS[i][HalfAxis.NEG_Y.ordinal()] = lookupNotNull(o.z.negate(), o.y, o.x);

			ROTATIONS[i][HalfAxis.POS_Z.ordinal()] = lookupNotNull(o.y.negate(), o.x, o.z);
			ROTATIONS[i][HalfAxis.NEG_Z.ordinal()] = lookupNotNull(o.y, o.x.negate(), o.z);
		}
	}

	public static Orientation lookup(HalfAxis x, HalfAxis y, HalfAxis z) {
		final int key = lookupKey(x, y, z);
		return LOOKUP.get(key);
	}

	private static Orientation lookupNotNull(HalfAxis x, HalfAxis y, HalfAxis z) {
		Orientation v = lookup(x, y, z);
		if (v == null) throw new NullPointerException(x + ":" + y + ":" + z);
		return v;
	}

	public static Orientation rotateAround(Orientation orientation, HalfAxis axis) {
		return ROTATIONS[orientation.ordinal()][axis.ordinal()];
	}

	public Orientation rotateAround(HalfAxis axis) {
		return rotateAround(this, axis);
	}

	public final HalfAxis x; // +X, east

	public final HalfAxis y; // +Y, top

	public final HalfAxis z; // +Z, south

	private final ForgeDirection[] localToGlobalDirections = new ForgeDirection[ForgeDirection.values().length];
	private final ForgeDirection[] globalToLocalDirections = new ForgeDirection[ForgeDirection.values().length];

	private void addDirectionMapping(ForgeDirection local, ForgeDirection global) {
		localToGlobalDirections[local.ordinal()] = global;
		globalToLocalDirections[global.ordinal()] = local;
	}

	private void addDirectionMappings(ForgeDirection local, ForgeDirection global) {
		addDirectionMapping(local, global);
		addDirectionMapping(local.getOpposite(), global.getOpposite());
	}

	private Orientation(HalfAxis x, HalfAxis y) {
		this.x = x;
		this.y = y;
		this.z = x.cross(y);

		addDirectionMappings(ForgeDirection.EAST, x.toDirection());
		addDirectionMappings(ForgeDirection.UP, y.toDirection());
		addDirectionMappings(ForgeDirection.SOUTH, z.toDirection());
		addDirectionMapping(ForgeDirection.UNKNOWN, ForgeDirection.UNKNOWN);
	}

	public ForgeDirection localToGlobalDirection(ForgeDirection local) {
		return localToGlobalDirections[local.ordinal()];
	}

	public ForgeDirection globalToLocalDirection(ForgeDirection global) {
		return globalToLocalDirections[global.ordinal()];
	}

	public double transformX(double x, double y, double z) {
		return this.x.x * x + this.y.x * y + this.z.x * z;
	}

	public int transformX(int x, int y, int z) {
		return this.x.x * x + this.y.x * y + this.z.x * z;
	}

	public double transformY(double x, double y, double z) {
		return this.x.y * x + this.y.y * y + this.z.y * z;
	}

	public int transformY(int x, int y, int z) {
		return this.x.y * x + this.y.y * y + this.z.y * z;
	}

	public double transformZ(double x, double y, double z) {
		return this.x.z * x + this.y.z * y + this.z.z * z;
	}

	public int transformZ(int x, int y, int z) {
		return this.x.z * x + this.y.z * y + this.z.z * z;
	}

}