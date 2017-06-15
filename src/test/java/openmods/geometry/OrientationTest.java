package openmods.geometry;

import java.util.Arrays;
import javax.vecmath.Vector3f;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class OrientationTest {

	@Parameters(name = "{0}")
	public static Iterable<Orientation> orientations() {
		return Arrays.asList(Orientation.values());
	}

	@Parameter
	public Orientation orientation;

	@Test
	public void testFourSameRotations() {
		for (HalfAxis a : HalfAxis.VALUES) {
			Assert.assertEquals(orientation, orientation.rotateAround(a).rotateAround(a).rotateAround(a).rotateAround(a));
		}
	}

	@Test
	public void testCounterRotations() {
		for (HalfAxis a : HalfAxis.VALUES) {
			Assert.assertEquals(orientation, orientation.rotateAround(a).rotateAround(a.negate()));
			Assert.assertEquals(orientation, orientation.rotateAround(a).rotateAround(a).rotateAround(a.negate()).rotateAround(a.negate()));
		}
	}

	private static HalfAxis[] select(HalfAxis... axes) {
		return axes;
	}

	@Test
	public void testRotationAroundX() {
		for (HalfAxis a : select(HalfAxis.POS_X, HalfAxis.NEG_X)) {
			final Orientation r = orientation.rotateAround(a).rotateAround(a);
			Assert.assertEquals(orientation.x, r.x);
			Assert.assertEquals(orientation.y.negate(), r.y);
			Assert.assertEquals(orientation.z.negate(), r.z);
		}
	}

	@Test
	public void testRotationY() {
		for (HalfAxis a : select(HalfAxis.POS_Y, HalfAxis.NEG_Y)) {
			final Orientation r = orientation.rotateAround(a).rotateAround(a);
			Assert.assertEquals(orientation.x.negate(), r.x);
			Assert.assertEquals(orientation.y, r.y);
			Assert.assertEquals(orientation.z.negate(), r.z);
		}
	}

	@Test
	public void testRotationZ() {
		for (HalfAxis a : select(HalfAxis.POS_Z, HalfAxis.NEG_Z)) {
			final Orientation r = orientation.rotateAround(a).rotateAround(a);
			Assert.assertEquals(orientation.x.negate(), r.x);
			Assert.assertEquals(orientation.y.negate(), r.y);
			Assert.assertEquals(orientation.z, r.z);
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
		testSameRotations(orientation, pair(HalfAxis.NEG_Z, HalfAxis.POS_X), pair(HalfAxis.POS_Y, HalfAxis.NEG_Z), pair(HalfAxis.POS_X, HalfAxis.POS_Y));
		testSameRotations(orientation, pair(HalfAxis.POS_Z, HalfAxis.POS_X), pair(HalfAxis.POS_X, HalfAxis.NEG_Y), pair(HalfAxis.NEG_Y, HalfAxis.POS_Z));
		testSameRotations(orientation, pair(HalfAxis.POS_Z, HalfAxis.NEG_X), pair(HalfAxis.POS_Y, HalfAxis.POS_Z), pair(HalfAxis.NEG_X, HalfAxis.POS_Y));
		testSameRotations(orientation, pair(HalfAxis.NEG_Z, HalfAxis.NEG_X), pair(HalfAxis.NEG_Y, HalfAxis.NEG_Z), pair(HalfAxis.NEG_X, HalfAxis.NEG_Y));
		testSameRotations(orientation, pair(HalfAxis.POS_X, HalfAxis.POS_Z), pair(HalfAxis.POS_Z, HalfAxis.POS_Y), pair(HalfAxis.POS_Y, HalfAxis.POS_X));
		testSameRotations(orientation, pair(HalfAxis.POS_X, HalfAxis.NEG_Z), pair(HalfAxis.NEG_Z, HalfAxis.NEG_Y), pair(HalfAxis.NEG_Y, HalfAxis.POS_X));
		testSameRotations(orientation, pair(HalfAxis.NEG_X, HalfAxis.POS_Z), pair(HalfAxis.POS_Z, HalfAxis.NEG_Y), pair(HalfAxis.NEG_Y, HalfAxis.NEG_X));
		testSameRotations(orientation, pair(HalfAxis.NEG_X, HalfAxis.NEG_Z), pair(HalfAxis.POS_Y, HalfAxis.NEG_X), pair(HalfAxis.NEG_Z, HalfAxis.POS_Y));
	}

	@Test
	public void testTransformationConsistency() {
		testTransformationConsistency(0, 0, 0);

		testTransformationConsistency(0, 0, +2);
		testTransformationConsistency(0, +2, 0);
		testTransformationConsistency(+2, 0, 0);

		testTransformationConsistency(0, 0, -2);
		testTransformationConsistency(0, -2, 0);
		testTransformationConsistency(-2, 0, 0);

		testTransformationConsistency(+1, +2, +3);

	}

	private void testTransformationConsistency(int x, int y, int z) {
		final Vector3f input = new Vector3f(x, y, z);

		final Vector3f separate = new Vector3f();
		separate.x = orientation.transformX(x, y, z);
		separate.y = orientation.transformY(x, y, z);
		separate.z = orientation.transformZ(x, y, z);

		final Vector3f common = new Vector3f(input);
		orientation.createLocalToWorldMatrix().transform(common);

		Assert.assertEquals(separate, common);
	}

}
