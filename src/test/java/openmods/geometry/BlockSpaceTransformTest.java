package openmods.geometry;

import net.minecraft.util.Vec3;

import org.junit.Assert;
import org.junit.Test;

public class BlockSpaceTransformTest {

	private static final double DELTA = Double.MIN_VALUE;

	public void testCoordinates(Orientation orientation, double x, double y, double z, double tx, double ty, double tz) {
		{
			final Vec3 v = BlockSpaceTransform.instance.mapWorldToBlock(orientation, x, y, z);
			Assert.assertEquals(tx, v.xCoord, DELTA);
			Assert.assertEquals(ty, v.yCoord, DELTA);
			Assert.assertEquals(tz, v.zCoord, DELTA);
		}

		{
			final Vec3 v = BlockSpaceTransform.instance.mapBlockToWorld(orientation, tx, ty, tz);
			Assert.assertEquals(x, v.xCoord, DELTA);
			Assert.assertEquals(y, v.yCoord, DELTA);
			Assert.assertEquals(z, v.zCoord, DELTA);
		}
	}

	// transform from -1,+1 (centered over 0) to 0,+1 (centered over 0.5) - i.e block space
	private static double offsetCoord(int c) {
		return (c + 1) / 2.0;
	}

	public void testOrientation(Orientation orientation, int x, int y, int z) {
		final double blockX = offsetCoord(x);
		final double blockY = offsetCoord(y);
		final double blockZ = offsetCoord(z);

		// bit of linear algebra. Orientation.[x,y,z] are basis vectors for reoriented space
		final double worldX = offsetCoord(orientation.x.offsetX * x + orientation.y.offsetX * y + orientation.z.offsetX * z);
		final double worldY = offsetCoord(orientation.x.offsetY * x + orientation.y.offsetY * y + orientation.z.offsetY * z);
		final double worldZ = offsetCoord(orientation.x.offsetZ * x + orientation.y.offsetZ * y + orientation.z.offsetZ * z);

		testCoordinates(orientation, worldX, worldY, worldZ, blockX, blockY, blockZ);
	}

	@Test
	public void testAllOrientations() {
		for (Orientation o : Orientation.values()) {
			testOrientation(o, +1, +1, +1);
			testOrientation(o, +1, +1, -1);
			testOrientation(o, +1, -1, +1);
			testOrientation(o, +1, -1, -1);
			testOrientation(o, -1, +1, +1);
			testOrientation(o, -1, +1, -1);
			testOrientation(o, -1, -1, +1);
			testOrientation(o, -1, -1, -1);
		}
	}

	// basic tests for most common coordinates

	@Test
	public void testBottomSouth() {
		testCoordinates(Orientation.BS, 0, 0, 0, 1, 1, 0);
		testCoordinates(Orientation.BS, 0, 0, 1, 1, 1, 1);
		testCoordinates(Orientation.BS, 0, 1, 0, 1, 0, 0);
		testCoordinates(Orientation.BS, 0, 1, 1, 1, 0, 1);
		testCoordinates(Orientation.BS, 1, 0, 0, 0, 1, 0);
		testCoordinates(Orientation.BS, 1, 0, 1, 0, 1, 1);
		testCoordinates(Orientation.BS, 1, 1, 0, 0, 0, 0);
		testCoordinates(Orientation.BS, 1, 1, 1, 0, 0, 1);
	}

	@Test
	public void testTopSouth() {
		testCoordinates(Orientation.TS, 0, 0, 0, 0, 0, 0);
		testCoordinates(Orientation.TS, 0, 0, 1, 0, 0, 1);
		testCoordinates(Orientation.TS, 0, 1, 0, 0, 1, 0);
		testCoordinates(Orientation.TS, 0, 1, 1, 0, 1, 1);
		testCoordinates(Orientation.TS, 1, 0, 0, 1, 0, 0);
		testCoordinates(Orientation.TS, 1, 0, 1, 1, 0, 1);
		testCoordinates(Orientation.TS, 1, 1, 0, 1, 1, 0);
		testCoordinates(Orientation.TS, 1, 1, 1, 1, 1, 1);
	}

	@Test
	public void testNorthTop() {
		testCoordinates(Orientation.NT, 0, 0, 0, 0, 1, 0);
		testCoordinates(Orientation.NT, 0, 0, 1, 0, 0, 0);
		testCoordinates(Orientation.NT, 0, 1, 0, 0, 1, 1);
		testCoordinates(Orientation.NT, 0, 1, 1, 0, 0, 1);
		testCoordinates(Orientation.NT, 1, 0, 0, 1, 1, 0);
		testCoordinates(Orientation.NT, 1, 0, 1, 1, 0, 0);
		testCoordinates(Orientation.NT, 1, 1, 0, 1, 1, 1);
		testCoordinates(Orientation.NT, 1, 1, 1, 1, 0, 1);
	}

	@Test
	public void testSouthTop() {
		testCoordinates(Orientation.SB, 0, 0, 0, 0, 0, 1);
		testCoordinates(Orientation.SB, 0, 0, 1, 0, 1, 1);
		testCoordinates(Orientation.SB, 0, 1, 0, 0, 0, 0);
		testCoordinates(Orientation.SB, 0, 1, 1, 0, 1, 0);
		testCoordinates(Orientation.SB, 1, 0, 0, 1, 0, 1);
		testCoordinates(Orientation.SB, 1, 0, 1, 1, 1, 1);
		testCoordinates(Orientation.SB, 1, 1, 0, 1, 0, 0);
		testCoordinates(Orientation.SB, 1, 1, 1, 1, 1, 0);
	}

	@Test
	public void testWestSouth() {
		testCoordinates(Orientation.WS, 0, 0, 0, 0, 1, 0);
		testCoordinates(Orientation.WS, 0, 0, 1, 0, 1, 1);
		testCoordinates(Orientation.WS, 0, 1, 0, 1, 1, 0);
		testCoordinates(Orientation.WS, 0, 1, 1, 1, 1, 1);
		testCoordinates(Orientation.WS, 1, 0, 0, 0, 0, 0);
		testCoordinates(Orientation.WS, 1, 0, 1, 0, 0, 1);
		testCoordinates(Orientation.WS, 1, 1, 0, 1, 0, 0);
		testCoordinates(Orientation.WS, 1, 1, 1, 1, 0, 1);
	}

	@Test
	public void testEastSouth() {
		testCoordinates(Orientation.ES, 0, 0, 0, 1, 0, 0);
		testCoordinates(Orientation.ES, 0, 0, 1, 1, 0, 1);
		testCoordinates(Orientation.ES, 0, 1, 0, 0, 0, 0);
		testCoordinates(Orientation.ES, 0, 1, 1, 0, 0, 1);
		testCoordinates(Orientation.ES, 1, 0, 0, 1, 1, 0);
		testCoordinates(Orientation.ES, 1, 0, 1, 1, 1, 1);
		testCoordinates(Orientation.ES, 1, 1, 0, 0, 1, 0);
		testCoordinates(Orientation.ES, 1, 1, 1, 0, 1, 1);
	}

	@Test
	public void testTopNorth() {
		testCoordinates(Orientation.TN, 0, 0, 0, 1, 0, 1);
		testCoordinates(Orientation.TN, 0, 0, 1, 1, 0, 0);
		testCoordinates(Orientation.TN, 0, 1, 0, 1, 1, 1);
		testCoordinates(Orientation.TN, 0, 1, 1, 1, 1, 0);
		testCoordinates(Orientation.TN, 1, 0, 0, 0, 0, 1);
		testCoordinates(Orientation.TN, 1, 0, 1, 0, 0, 0);
		testCoordinates(Orientation.TN, 1, 1, 0, 0, 1, 1);
		testCoordinates(Orientation.TN, 1, 1, 1, 0, 1, 0);
	}

	@Test
	public void testTopWest() {
		testCoordinates(Orientation.TW, 0, 0, 0, 0, 0, 1);
		testCoordinates(Orientation.TW, 0, 0, 1, 1, 0, 1);
		testCoordinates(Orientation.TW, 0, 1, 0, 0, 1, 1);
		testCoordinates(Orientation.TW, 0, 1, 1, 1, 1, 1);
		testCoordinates(Orientation.TW, 1, 0, 0, 0, 0, 0);
		testCoordinates(Orientation.TW, 1, 0, 1, 1, 0, 0);
		testCoordinates(Orientation.TW, 1, 1, 0, 0, 1, 0);
		testCoordinates(Orientation.TW, 1, 1, 1, 1, 1, 0);
	}

	@Test
	public void testTopEast() {
		testCoordinates(Orientation.TE, 0, 0, 0, 1, 0, 0);
		testCoordinates(Orientation.TE, 0, 0, 1, 0, 0, 0);
		testCoordinates(Orientation.TE, 0, 1, 0, 1, 1, 0);
		testCoordinates(Orientation.TE, 0, 1, 1, 0, 1, 0);
		testCoordinates(Orientation.TE, 1, 0, 0, 1, 0, 1);
		testCoordinates(Orientation.TE, 1, 0, 1, 0, 0, 1);
		testCoordinates(Orientation.TE, 1, 1, 0, 1, 1, 1);
		testCoordinates(Orientation.TE, 1, 1, 1, 0, 1, 1);
	}
}
