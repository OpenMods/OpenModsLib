package openmods.geometry;

import net.minecraft.util.Direction;
import org.junit.Assert;
import org.junit.Test;

public class LocalDirectionsTest {

	@Test
	public void testForNorthAndUp() {
		LocalDirections dirs = LocalDirections.fromFrontAndTop(Direction.NORTH, Direction.UP);
		Assert.assertEquals(dirs.front, Direction.NORTH);
		Assert.assertEquals(dirs.back, Direction.SOUTH);

		Assert.assertEquals(dirs.top, Direction.UP);
		Assert.assertEquals(dirs.bottom, Direction.DOWN);

		Assert.assertEquals(dirs.right, Direction.EAST);
		Assert.assertEquals(dirs.left, Direction.WEST);
	}

	@Test
	public void testForUpAndNorth() {
		LocalDirections dirs = LocalDirections.fromFrontAndTop(Direction.UP, Direction.NORTH);
		Assert.assertEquals(dirs.front, Direction.UP);
		Assert.assertEquals(dirs.back, Direction.DOWN);

		Assert.assertEquals(dirs.top, Direction.NORTH);
		Assert.assertEquals(dirs.bottom, Direction.SOUTH);

		Assert.assertEquals(dirs.right, Direction.WEST);
		Assert.assertEquals(dirs.left, Direction.EAST);
	}

}
