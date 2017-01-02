package openmods.geometry;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

public class OrientationTest {

	@Test
	public void testFourSameRotations() {
		for (Orientation o : Orientation.VALUES)
			for (HalfAxis a : HalfAxis.VALUES) {
				Assert.assertEquals(o, o.rotateAround(a).rotateAround(a).rotateAround(a).rotateAround(a));
			}
	}

	@Test
	public void testCounterRotations() {
		for (Orientation o : Orientation.VALUES)
			for (HalfAxis a : HalfAxis.VALUES) {
				Assert.assertEquals(o, o.rotateAround(a).rotateAround(a.negate()));
				Assert.assertEquals(o, o.rotateAround(a).rotateAround(a).rotateAround(a.negate()).rotateAround(a.negate()));
			}
	}

	private static HalfAxis[] select(HalfAxis... axes) {
		return axes;
	}

	@Test
	public void testRotationAroundX() {
		for (Orientation o : Orientation.VALUES)
			for (HalfAxis a : select(HalfAxis.POS_X, HalfAxis.NEG_X)) {
				final Orientation r = o.rotateAround(a).rotateAround(a);
				Assert.assertEquals(o.x, r.x);
				Assert.assertEquals(o.y.negate(), r.y);
				Assert.assertEquals(o.z.negate(), r.z);
			}
	}

	@Test
	public void testRotationY() {
		for (Orientation o : Orientation.VALUES)
			for (HalfAxis a : select(HalfAxis.POS_Y, HalfAxis.NEG_Y)) {
				final Orientation r = o.rotateAround(a).rotateAround(a);
				Assert.assertEquals(o.x.negate(), r.x);
				Assert.assertEquals(o.y, r.y);
				Assert.assertEquals(o.z.negate(), r.z);
			}
	}

	@Test
	public void testRotationZ() {
		for (Orientation o : Orientation.VALUES)
			for (HalfAxis a : select(HalfAxis.POS_Z, HalfAxis.NEG_Z)) {
				final Orientation r = o.rotateAround(a).rotateAround(a);
				Assert.assertEquals(o.x.negate(), r.x);
				Assert.assertEquals(o.y.negate(), r.y);
				Assert.assertEquals(o.z, r.z);
			}
	}

	private static class TwoRotations {
		public final HalfAxis first;
		public final HalfAxis second;

		public TwoRotations(HalfAxis first, HalfAxis second) {
			this.first = first;
			this.second = second;
		}

		public Orientation apply(Orientation orientation) {
			return orientation.rotateAround(first).rotateAround(second);
		}
	}

	public static TwoRotations pair(HalfAxis first, HalfAxis second) {
		return new TwoRotations(first, second);
	}

	private static void testSameRotations(Orientation orientation, TwoRotations... rotations) {
		final Orientation first = rotations[0].apply(orientation);
		for (int i = 1; i < rotations.length; i++) {
			Orientation next = rotations[i].apply(orientation);
			Assert.assertEquals(first, next);
		}
	}

	@Test
	public void testTwoRotations() {
		for (Orientation o : Orientation.VALUES) {
			testSameRotations(o, pair(HalfAxis.NEG_Z, HalfAxis.POS_X), pair(HalfAxis.POS_Y, HalfAxis.NEG_Z), pair(HalfAxis.POS_X, HalfAxis.POS_Y));
			testSameRotations(o, pair(HalfAxis.POS_Z, HalfAxis.POS_X), pair(HalfAxis.POS_X, HalfAxis.NEG_Y), pair(HalfAxis.NEG_Y, HalfAxis.POS_Z));
			testSameRotations(o, pair(HalfAxis.POS_Z, HalfAxis.NEG_X), pair(HalfAxis.POS_Y, HalfAxis.POS_Z), pair(HalfAxis.NEG_X, HalfAxis.POS_Y));
			testSameRotations(o, pair(HalfAxis.NEG_Z, HalfAxis.NEG_X), pair(HalfAxis.NEG_Y, HalfAxis.NEG_Z), pair(HalfAxis.NEG_X, HalfAxis.NEG_Y));
			testSameRotations(o, pair(HalfAxis.POS_X, HalfAxis.POS_Z), pair(HalfAxis.POS_Z, HalfAxis.POS_Y), pair(HalfAxis.POS_Y, HalfAxis.POS_X));
			testSameRotations(o, pair(HalfAxis.POS_X, HalfAxis.NEG_Z), pair(HalfAxis.NEG_Z, HalfAxis.NEG_Y), pair(HalfAxis.NEG_Y, HalfAxis.POS_X));
			testSameRotations(o, pair(HalfAxis.NEG_X, HalfAxis.POS_Z), pair(HalfAxis.POS_Z, HalfAxis.NEG_Y), pair(HalfAxis.NEG_Y, HalfAxis.NEG_X));
			testSameRotations(o, pair(HalfAxis.NEG_X, HalfAxis.NEG_Z), pair(HalfAxis.POS_Y, HalfAxis.NEG_X), pair(HalfAxis.NEG_Z, HalfAxis.POS_Y));
		}
	}

	// This is code for generation of one of tests.
	// I brute-forced those relations and then started to rotate random things to verify it
	// If anyone knows, if there is more formalized way, please let me know. -boq
	public static void main(String[] argv) {
		final Multimap<Matrix3d, Pair<HalfAxis, HalfAxis>> m = HashMultimap.create();
		final Map<HalfAxis, Matrix3d> ms = Maps.newHashMap();

		// note: we need intrinsic matrices (axis rotation instead of point)
		// remove inverts to get extrinsic relationships
		ms.put(HalfAxis.NEG_X, new Matrix3d(1, 0, 0, 0, 0, +1, 0, -1, 0).invertInplace());
		ms.put(HalfAxis.POS_X, new Matrix3d(1, 0, 0, 0, 0, -1, 0, +1, 0).invertInplace());

		ms.put(HalfAxis.NEG_Y, new Matrix3d(0, 0, -1, 0, 1, 0, +1, 0, 0).invertInplace());
		ms.put(HalfAxis.POS_Y, new Matrix3d(0, 0, +1, 0, 1, 0, -1, 0, 0).invertInplace());

		ms.put(HalfAxis.NEG_Z, new Matrix3d(0, +1, 0, -1, 0, 0, 0, 0, 1).invertInplace());
		ms.put(HalfAxis.POS_Z, new Matrix3d(0, -1, 0, +1, 0, 0, 0, 0, 1).invertInplace());

		for (HalfAxis a : HalfAxis.VALUES)
			for (HalfAxis b : HalfAxis.VALUES) {
				final Matrix3d ma = ms.get(a);
				final Matrix3d mb = ms.get(b);
				m.put(Matrix3d.mul(ma, mb), Pair.of(a, b));
			}

		for (Collection<Pair<HalfAxis, HalfAxis>> v : m.asMap().values()) {
			System.out.println(Joiner.on("=").join(v));
		}
	}
}
