package openmods.geometry;

import net.minecraftforge.common.util.ForgeDirection;
import openmods.geometry.BlockTextureTransform.TexCoords;
import openmods.geometry.BlockTextureTransform.WorldCoords;

import org.junit.Assert;
import org.junit.Test;

public class BlockTextureTransformTest {

	private static void testInversion(ForgeDirection dir, double x, double y, double z) {
		BlockTextureTransform t = new BlockTextureTransform(dir);
		TexCoords c = t.worldVecToTextureCoords(x, y, z);
		WorldCoords r = t.textureCoordsToWorldVec(c.u, c.v, c.z);
		Assert.assertEquals(x, r.x, 0.00001);
		Assert.assertEquals(y, r.y, 0.00001);
		Assert.assertEquals(z, r.z, 0.00001);
	}

	public void testInversion(ForgeDirection dir) {
		testInversion(dir, 0, 0, 0);
		testInversion(dir, 1, 0, 0);
		testInversion(dir, 0, 1, 0);
		testInversion(dir, 1, 1, 0);
		testInversion(dir, 0, 0, 1);
		testInversion(dir, 1, 0, 1);
		testInversion(dir, 0, 1, 1);
		testInversion(dir, 1, 1, 1);
	}

	@Test
	public void testInversions() {
		testInversion(ForgeDirection.NORTH);
		testInversion(ForgeDirection.SOUTH);
		testInversion(ForgeDirection.EAST);
		testInversion(ForgeDirection.WEST);
		testInversion(ForgeDirection.UP);
		testInversion(ForgeDirection.DOWN);
	}
}
