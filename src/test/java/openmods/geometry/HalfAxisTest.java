package openmods.geometry;

import org.junit.Assert;
import org.junit.Test;

public class HalfAxisTest {

	// just a sanity check
	@Test
	public void testCross() {
		for (HalfAxis a : HalfAxis.values())
			for (HalfAxis b : HalfAxis.values()) {
				final HalfAxis pp = HalfAxis.cross(a, b);
				final HalfAxis pn = HalfAxis.cross(b, a);

				if (a == b || a == b.negate()) {
					Assert.assertNull(pp);
					Assert.assertNull(pn);
				} else {
					Assert.assertEquals(pp, pn.negate());

					final int x = a.y * b.z - a.z * b.y;
					final int y = a.z * b.x - a.x * b.z;
					final int z = a.x * b.y - a.y * b.x;

					Assert.assertEquals(pp.x, x);
					Assert.assertEquals(pp.y, y);
					Assert.assertEquals(pp.z, z);
				}
			}
	}

}
