package openmods.geometry;

import net.minecraft.util.math.Vec3d;
import org.junit.Assert;
import org.junit.Test;

public class BlockSpaceTransformTest {

	private static final double DELTA = Double.MIN_VALUE;

	public void testCoordinates(Orientation orientation, double worldX, double worldY, double worldZ, double blockX, double blockY, double blockZ) {
		{
			final Vec3d v = BlockSpaceTransform.instance.mapWorldToBlock(orientation, worldX, worldY, worldZ);
			Assert.assertEquals(blockX, v.xCoord, DELTA);
			Assert.assertEquals(blockY, v.yCoord, DELTA);
			Assert.assertEquals(blockZ, v.zCoord, DELTA);
		}

		{
			final Vec3d v = BlockSpaceTransform.instance.mapBlockToWorld(orientation, blockX, blockY, blockZ);
			Assert.assertEquals(worldX, v.xCoord, DELTA);
			Assert.assertEquals(worldY, v.yCoord, DELTA);
			Assert.assertEquals(worldZ, v.zCoord, DELTA);
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
		final double worldX = offsetCoord(orientation.transformX(x, y, z));
		final double worldY = offsetCoord(orientation.transformY(x, y, z));
		final double worldZ = offsetCoord(orientation.transformZ(x, y, z));

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
		testCoordinates(Orientation.XN_YN, 0, 0, 0, 1, 1, 0);
		testCoordinates(Orientation.XN_YN, 0, 0, 1, 1, 1, 1);
		testCoordinates(Orientation.XN_YN, 0, 1, 0, 1, 0, 0);
		testCoordinates(Orientation.XN_YN, 0, 1, 1, 1, 0, 1);
		testCoordinates(Orientation.XN_YN, 1, 0, 0, 0, 1, 0);
		testCoordinates(Orientation.XN_YN, 1, 0, 1, 0, 1, 1);
		testCoordinates(Orientation.XN_YN, 1, 1, 0, 0, 0, 0);
		testCoordinates(Orientation.XN_YN, 1, 1, 1, 0, 0, 1);
	}

	@Test
	public void testTopSouth() {
		testCoordinates(Orientation.XP_YP, 0, 0, 0, 0, 0, 0);
		testCoordinates(Orientation.XP_YP, 0, 0, 1, 0, 0, 1);
		testCoordinates(Orientation.XP_YP, 0, 1, 0, 0, 1, 0);
		testCoordinates(Orientation.XP_YP, 0, 1, 1, 0, 1, 1);
		testCoordinates(Orientation.XP_YP, 1, 0, 0, 1, 0, 0);
		testCoordinates(Orientation.XP_YP, 1, 0, 1, 1, 0, 1);
		testCoordinates(Orientation.XP_YP, 1, 1, 0, 1, 1, 0);
		testCoordinates(Orientation.XP_YP, 1, 1, 1, 1, 1, 1);
	}

	@Test
	public void testNorthTop() {
		testCoordinates(Orientation.XP_ZN, 0, 0, 0, 0, 1, 0);
		testCoordinates(Orientation.XP_ZN, 0, 0, 1, 0, 0, 0);
		testCoordinates(Orientation.XP_ZN, 0, 1, 0, 0, 1, 1);
		testCoordinates(Orientation.XP_ZN, 0, 1, 1, 0, 0, 1);
		testCoordinates(Orientation.XP_ZN, 1, 0, 0, 1, 1, 0);
		testCoordinates(Orientation.XP_ZN, 1, 0, 1, 1, 0, 0);
		testCoordinates(Orientation.XP_ZN, 1, 1, 0, 1, 1, 1);
		testCoordinates(Orientation.XP_ZN, 1, 1, 1, 1, 0, 1);
	}

	@Test
	public void testSouthTop() {
		testCoordinates(Orientation.XP_ZP, 0, 0, 0, 0, 0, 1);
		testCoordinates(Orientation.XP_ZP, 0, 0, 1, 0, 1, 1);
		testCoordinates(Orientation.XP_ZP, 0, 1, 0, 0, 0, 0);
		testCoordinates(Orientation.XP_ZP, 0, 1, 1, 0, 1, 0);
		testCoordinates(Orientation.XP_ZP, 1, 0, 0, 1, 0, 1);
		testCoordinates(Orientation.XP_ZP, 1, 0, 1, 1, 1, 1);
		testCoordinates(Orientation.XP_ZP, 1, 1, 0, 1, 0, 0);
		testCoordinates(Orientation.XP_ZP, 1, 1, 1, 1, 1, 0);
	}

	@Test
	public void testWestSouth() {
		testCoordinates(Orientation.YP_XN, 0, 0, 0, 0, 1, 0);
		testCoordinates(Orientation.YP_XN, 0, 0, 1, 0, 1, 1);
		testCoordinates(Orientation.YP_XN, 0, 1, 0, 1, 1, 0);
		testCoordinates(Orientation.YP_XN, 0, 1, 1, 1, 1, 1);
		testCoordinates(Orientation.YP_XN, 1, 0, 0, 0, 0, 0);
		testCoordinates(Orientation.YP_XN, 1, 0, 1, 0, 0, 1);
		testCoordinates(Orientation.YP_XN, 1, 1, 0, 1, 0, 0);
		testCoordinates(Orientation.YP_XN, 1, 1, 1, 1, 0, 1);
	}

	@Test
	public void testEastSouth() {
		testCoordinates(Orientation.YN_XP, 0, 0, 0, 1, 0, 0);
		testCoordinates(Orientation.YN_XP, 0, 0, 1, 1, 0, 1);
		testCoordinates(Orientation.YN_XP, 0, 1, 0, 0, 0, 0);
		testCoordinates(Orientation.YN_XP, 0, 1, 1, 0, 0, 1);
		testCoordinates(Orientation.YN_XP, 1, 0, 0, 1, 1, 0);
		testCoordinates(Orientation.YN_XP, 1, 0, 1, 1, 1, 1);
		testCoordinates(Orientation.YN_XP, 1, 1, 0, 0, 1, 0);
		testCoordinates(Orientation.YN_XP, 1, 1, 1, 0, 1, 1);
	}

	@Test
	public void testTopNorth() {
		testCoordinates(Orientation.XN_YP, 0, 0, 0, 1, 0, 1);
		testCoordinates(Orientation.XN_YP, 0, 0, 1, 1, 0, 0);
		testCoordinates(Orientation.XN_YP, 0, 1, 0, 1, 1, 1);
		testCoordinates(Orientation.XN_YP, 0, 1, 1, 1, 1, 0);
		testCoordinates(Orientation.XN_YP, 1, 0, 0, 0, 0, 1);
		testCoordinates(Orientation.XN_YP, 1, 0, 1, 0, 0, 0);
		testCoordinates(Orientation.XN_YP, 1, 1, 0, 0, 1, 1);
		testCoordinates(Orientation.XN_YP, 1, 1, 1, 0, 1, 0);
	}

	@Test
	public void testTopWest() {
		testCoordinates(Orientation.ZP_YP, 0, 0, 0, 0, 0, 1);
		testCoordinates(Orientation.ZP_YP, 0, 0, 1, 1, 0, 1);
		testCoordinates(Orientation.ZP_YP, 0, 1, 0, 0, 1, 1);
		testCoordinates(Orientation.ZP_YP, 0, 1, 1, 1, 1, 1);
		testCoordinates(Orientation.ZP_YP, 1, 0, 0, 0, 0, 0);
		testCoordinates(Orientation.ZP_YP, 1, 0, 1, 1, 0, 0);
		testCoordinates(Orientation.ZP_YP, 1, 1, 0, 0, 1, 0);
		testCoordinates(Orientation.ZP_YP, 1, 1, 1, 1, 1, 0);
	}

	@Test
	public void testTopEast() {
		testCoordinates(Orientation.ZN_YP, 0, 0, 0, 1, 0, 0);
		testCoordinates(Orientation.ZN_YP, 0, 0, 1, 0, 0, 0);
		testCoordinates(Orientation.ZN_YP, 0, 1, 0, 1, 1, 0);
		testCoordinates(Orientation.ZN_YP, 0, 1, 1, 0, 1, 0);
		testCoordinates(Orientation.ZN_YP, 1, 0, 0, 1, 0, 1);
		testCoordinates(Orientation.ZN_YP, 1, 0, 1, 0, 0, 1);
		testCoordinates(Orientation.ZN_YP, 1, 1, 0, 1, 1, 1);
		testCoordinates(Orientation.ZN_YP, 1, 1, 1, 0, 1, 1);
	}
}
