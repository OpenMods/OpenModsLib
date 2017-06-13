package openmods.geometry;

import net.minecraft.util.EnumFacing;
import org.junit.Assert;
import org.junit.Test;

public class LocalDirectionsTest {

	@Test
	public void testForNorthAndUp() {
		LocalDirections dirs = LocalDirections.fromFrontAndTop(EnumFacing.NORTH, EnumFacing.UP);
		Assert.assertEquals(dirs.front, EnumFacing.NORTH);
		Assert.assertEquals(dirs.back, EnumFacing.SOUTH);

		Assert.assertEquals(dirs.top, EnumFacing.UP);
		Assert.assertEquals(dirs.bottom, EnumFacing.DOWN);

		Assert.assertEquals(dirs.right, EnumFacing.EAST);
		Assert.assertEquals(dirs.left, EnumFacing.WEST);
	}

	@Test
	public void testForUpAndNorth() {
		LocalDirections dirs = LocalDirections.fromFrontAndTop(EnumFacing.UP, EnumFacing.NORTH);
		Assert.assertEquals(dirs.front, EnumFacing.UP);
		Assert.assertEquals(dirs.back, EnumFacing.DOWN);

		Assert.assertEquals(dirs.top, EnumFacing.NORTH);
		Assert.assertEquals(dirs.bottom, EnumFacing.SOUTH);

		Assert.assertEquals(dirs.right, EnumFacing.WEST);
		Assert.assertEquals(dirs.left, EnumFacing.EAST);
	}

}
