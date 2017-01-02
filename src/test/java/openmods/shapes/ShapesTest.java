package openmods.shapes;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.Collection;
import openmods.utils.Coord;
import org.junit.Assert;
import org.junit.Test;

public class ShapesTest {

	public static Multiset<Coord> generate(IShapeGenerator generator, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		final Multiset<Coord> result = HashMultiset.create();

		generator.generateShape(minX, minY, minZ, maxX, maxY, maxZ, new IShapeable() {
			@Override
			public void setBlock(int x, int y, int z) {
				result.add(new Coord(x, y, z));
			}
		});

		return result;
	}

	public static class BoundingBox {
		public int minX = Integer.MAX_VALUE;
		public int minY = Integer.MAX_VALUE;
		public int minZ = Integer.MAX_VALUE;

		public int maxX = Integer.MIN_VALUE;
		public int maxY = Integer.MIN_VALUE;
		public int maxZ = Integer.MIN_VALUE;

		public void addCoord(Coord coord) {
			minX = Math.min(minX, coord.x);
			minY = Math.min(minY, coord.y);
			minZ = Math.min(minZ, coord.z);

			maxX = Math.max(maxX, coord.x);
			maxY = Math.max(maxY, coord.y);
			maxZ = Math.max(maxZ, coord.z);
		}

		public void check(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			Assert.assertEquals(minX, this.minX);
			Assert.assertEquals(minY, this.minY);
			Assert.assertEquals(minZ, this.minZ);

			Assert.assertEquals(maxX, this.maxX);
			Assert.assertEquals(maxY, this.maxY);
			Assert.assertEquals(maxZ, this.maxZ);
		}
	}

	public static BoundingBox bb(Collection<Coord> coords) {
		BoundingBox bb = new BoundingBox();
		for (Coord coord : coords)
			bb.addCoord(coord);
		return bb;
	}

	public static BoundingBox bb(IShapeGenerator generator, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		final Multiset<Coord> coords = generate(generator, minX, minY, minZ, maxX, maxY, maxZ);
		return bb(coords);
	}

	public static void checkInBB(IShapeGenerator generator, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		bb(generator, minX, minY, minZ, maxX, maxY, maxZ).check(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static void checkZero(IShapeGenerator generator) {
		final Multiset<Coord> coords = generate(generator, 0, 0, 0, 0, 0, 0);
		Assert.assertEquals(1, coords.elementSet().size());
		Assert.assertTrue(coords.contains(new Coord(0, 0, 0)));
	}

	public static void checkBasicBehaviour(IShapeGenerator generator) {
		checkZero(generator);

		checkInBB(generator, -1, -1, -1, 1, 1, 1);
		checkInBB(generator, -2, -2, -2, 2, 2, 2);
		checkInBB(generator, -3, -3, -3, 3, 3, 3);
		checkInBB(generator, -4, -4, -4, 4, 4, 4);
		checkInBB(generator, -5, -5, -5, 5, 5, 5);

		checkInBB(generator, 0, 0, 0, 2 * 1, 2 * 1, 2 * 1);
		checkInBB(generator, 0, 0, 0, 2 * 2, 2 * 2, 2 * 2);
		checkInBB(generator, 0, 0, 0, 2 * 3, 2 * 3, 2 * 3);
		checkInBB(generator, 0, 0, 0, 2 * 4, 2 * 4, 2 * 4);
		checkInBB(generator, 0, 0, 0, 2 * 5, 2 * 5, 2 * 5);

		checkInBB(generator, 0, 0, 0, 3, 3, 3);
		checkInBB(generator, 0, 0, 0, 5, 5, 5);

		checkInBB(generator, 1, 1, 1, 4, 4, 4);
		checkInBB(generator, 2, 2, 2, 7, 7, 7);
	}

	@Test
	public void testEllipsoid() {
		IShapeGenerator generator = new ShapeSphereGenerator();
		checkBasicBehaviour(generator);
	}

	@Test
	public void testCylinder() {
		IShapeGenerator generator = new ShapeCylinderGenerator();
		checkBasicBehaviour(generator);
	}

	@Test
	public void testCube() {
		IShapeGenerator generator = new ShapeCuboidGenerator();
		checkBasicBehaviour(generator);
	}

}
