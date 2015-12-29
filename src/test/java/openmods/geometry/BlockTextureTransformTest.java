package openmods.geometry;

import net.minecraft.util.EnumFacing;
import openmods.geometry.BlockTextureTransform.TexCoords;
import openmods.geometry.BlockTextureTransform.WorldCoords;

import org.junit.Assert;
import org.junit.Test;

public class BlockTextureTransformTest {

	private static final double DELTA = 0.00001;

	private static void testInversion(BlockTextureTransform t, EnumFacing dir, double x, double y, double z) {
		TexCoords c = t.worldVecToTextureCoords(dir, x, y, z);
		WorldCoords r = t.textureCoordsToWorldVec(dir, c.u, c.v, c.h);
		Assert.assertEquals(x, r.x, DELTA);
		Assert.assertEquals(y, r.y, DELTA);
		Assert.assertEquals(z, r.z, DELTA);
	}

	public void testInversion(BlockTextureTransform t, EnumFacing dir) {
		testInversion(t, dir, 0, 0, 0);
		testInversion(t, dir, 1, 0, 0);
		testInversion(t, dir, 0, 1, 0);
		testInversion(t, dir, 1, 1, 0);
		testInversion(t, dir, 0, 0, 1);
		testInversion(t, dir, 1, 0, 1);
		testInversion(t, dir, 0, 1, 1);
		testInversion(t, dir, 1, 1, 1);
	}

	public void testInversion(BlockTextureTransform t) {
		for (EnumFacing dir : EnumFacing.VALUES)
			testInversion(t, dir);
	}

	@Test
	public void testDefaultInversions() {
		testInversion(BlockTextureTransform.builder().build());
	}

	@Test
	public void testInversionAfterCWRotation() {
		for (EnumFacing dir : EnumFacing.VALUES)
			testInversion(BlockTextureTransform.builder().rotateCW(dir).build());
	}

	@Test
	public void testInversionAfterCCWRotation() {
		for (EnumFacing dir : EnumFacing.VALUES)
			testInversion(BlockTextureTransform.builder().rotateCCW(dir).build());
	}

	@Test
	public void testInversionAfterUWSwapRotation() {
		for (EnumFacing dir : EnumFacing.VALUES)
			testInversion(BlockTextureTransform.builder().swapUV(dir).build());
	}

	@Test
	public void testInversionAfterMirrorURotation() {
		for (EnumFacing dir : EnumFacing.VALUES)
			testInversion(BlockTextureTransform.builder().mirrorU(dir).build());
	}

	@Test
	public void testInversionAfterMirrorUVRotation() {
		for (EnumFacing dir : EnumFacing.VALUES)
			testInversion(BlockTextureTransform.builder().mirrorUV(dir).build());
	}

	@Test
	public void testInversionAfterMirrorVRotation() {
		for (EnumFacing dir : EnumFacing.VALUES)
			testInversion(BlockTextureTransform.builder().mirrorV(dir).build());
	}

	private static void testIdentity(EnumFacing dir, double x, double y, double z, BlockTextureTransform a, BlockTextureTransform b) {
		{
			final TexCoords at = a.worldVecToTextureCoords(dir, x, y, z);
			final TexCoords bt = b.worldVecToTextureCoords(dir, x, y, z);
			Assert.assertEquals(at.u, bt.u, DELTA);
			Assert.assertEquals(at.v, bt.v, DELTA);
			Assert.assertEquals(at.h, bt.h, DELTA);
		}

		{
			final WorldCoords aw = a.textureCoordsToWorldVec(dir, x, y, z);
			final WorldCoords bw = b.textureCoordsToWorldVec(dir, x, y, z);
			Assert.assertEquals(aw.x, bw.x, DELTA);
			Assert.assertEquals(aw.y, bw.y, DELTA);
			Assert.assertEquals(aw.z, bw.z, DELTA);
		}
	}

	private static void testIdentity(final BlockTextureTransform a, final BlockTextureTransform b) {
		for (EnumFacing dir : EnumFacing.VALUES) {
			testIdentity(dir, 0, 0, 0, a, b);
			testIdentity(dir, 0, 1, 0, a, b);
			testIdentity(dir, 1, 1, 0, a, b);
			testIdentity(dir, 1, 0, 0, a, b);
		}
	}

	@Test
	public void testIdentities() {
		for (EnumFacing dir : EnumFacing.VALUES) {
			{
				final BlockTextureTransform normal = BlockTextureTransform.builder().build();

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().rotateCCW(dir).rotateCCW(dir).rotateCCW(dir).rotateCCW(dir).build();
					testIdentity(normal, modified);
				}

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().rotateCW(dir).rotateCW(dir).rotateCW(dir).rotateCW(dir).build();
					testIdentity(normal, modified);
				}

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().rotateCW(dir).rotateCCW(dir).build();
					testIdentity(normal, modified);
				}

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().rotateCCW(dir).rotateCW(dir).build();
					testIdentity(normal, modified);
				}

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().swapUV(dir).swapUV(dir).build();
					testIdentity(normal, modified);
				}

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().swapUV(dir).swapUV(dir).build();
					testIdentity(normal, modified);
				}

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().mirrorU(dir).mirrorU(dir).build();
					testIdentity(normal, modified);
				}

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().mirrorV(dir).mirrorV(dir).build();
					testIdentity(normal, modified);
				}

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().mirrorV(dir).mirrorV(dir).build();
					testIdentity(normal, modified);
				}

				{
					final BlockTextureTransform modified = BlockTextureTransform.builder().mirrorUV(dir).mirrorU(dir).mirrorV(dir).build();
					testIdentity(normal, modified);
				}
			}

			{
				final BlockTextureTransform modifiedA = BlockTextureTransform.builder().rotateCW(dir).rotateCW(dir).build();
				final BlockTextureTransform modifiedB = BlockTextureTransform.builder().rotateCCW(dir).rotateCCW(dir).build();
				testIdentity(modifiedA, modifiedB);
			}

			{
				final BlockTextureTransform modifiedA = BlockTextureTransform.builder().mirrorU(dir).mirrorV(dir).build();
				{
					final BlockTextureTransform modifiedB = BlockTextureTransform.builder().rotateCCW(dir).rotateCCW(dir).build();
					testIdentity(modifiedA, modifiedB);
				}

				{
					final BlockTextureTransform modifiedB = BlockTextureTransform.builder().rotateCW(dir).rotateCW(dir).build();
					testIdentity(modifiedA, modifiedB);
				}
			}

			{
				final BlockTextureTransform modifiedA = BlockTextureTransform.builder().swapUV(dir).mirrorU(dir).build();
				final BlockTextureTransform modifiedB = BlockTextureTransform.builder().rotateCW(dir).build();
				testIdentity(modifiedA, modifiedB);
			}

			{
				final BlockTextureTransform modifiedA = BlockTextureTransform.builder().swapUV(dir).mirrorV(dir).build();
				final BlockTextureTransform modifiedB = BlockTextureTransform.builder().rotateCCW(dir).build();
				testIdentity(modifiedA, modifiedB);
			}
		}
	}

	public static void checkCoordinates(EnumFacing direction, double x, double y, double z, double u, double v, double h) {
		// standard Minecraft model
		final BlockTextureTransform mapper = BlockTextureTransform.builder().mirrorU(EnumFacing.NORTH).mirrorU(EnumFacing.EAST).build();

		{
			final TexCoords t = mapper.worldVecToTextureCoords(direction, x, y, z);
			Assert.assertEquals(u, t.u, DELTA);
			Assert.assertEquals(v, t.v, DELTA);
			Assert.assertEquals(h, t.h, DELTA);
		}

		{
			final WorldCoords w = mapper.textureCoordsToWorldVec(direction, u, v, h);
			Assert.assertEquals(x, w.x, DELTA);
			Assert.assertEquals(y, w.y, DELTA);
			Assert.assertEquals(z, w.z, DELTA);
		}
	}

	@Test
	public void testDefaultNorthMapping() {
		checkCoordinates(EnumFacing.NORTH, 1, 1, 0, 0, 0, 0);
		checkCoordinates(EnumFacing.NORTH, 0, 1, 0, 1, 0, 0);
		checkCoordinates(EnumFacing.NORTH, 1, 0, 0, 0, 1, 0);
		checkCoordinates(EnumFacing.NORTH, 0, 0, 0, 1, 1, 0);
		checkCoordinates(EnumFacing.NORTH, 0.1, 0.1, 0.1, 0.9, 0.9, -0.1);
	}

	@Test
	public void testDefaultSouthMapping() {
		checkCoordinates(EnumFacing.SOUTH, 0, 1, 1, 0, 0, 0);
		checkCoordinates(EnumFacing.SOUTH, 1, 1, 1, 1, 0, 0);
		checkCoordinates(EnumFacing.SOUTH, 0, 0, 1, 0, 1, 0);
		checkCoordinates(EnumFacing.SOUTH, 1, 0, 1, 1, 1, 0);
		checkCoordinates(EnumFacing.SOUTH, 0.1, 0.1, 1.1, 0.1, 0.9, 0.1);
	}

	@Test
	public void testDefaultEastMapping() {
		checkCoordinates(EnumFacing.EAST, 1, 1, 1, 0, 0, 0);
		checkCoordinates(EnumFacing.EAST, 1, 1, 0, 1, 0, 0);
		checkCoordinates(EnumFacing.EAST, 1, 0, 1, 0, 1, 0);
		checkCoordinates(EnumFacing.EAST, 1, 0, 0, 1, 1, 0);
		checkCoordinates(EnumFacing.EAST, 1.1, 0.1, 0.1, 0.9, 0.9, 0.1);
	}

	@Test
	public void testDefaultWestMapping() {
		checkCoordinates(EnumFacing.WEST, 0, 1, 0, 0, 0, 0);
		checkCoordinates(EnumFacing.WEST, 0, 1, 1, 1, 0, 0);
		checkCoordinates(EnumFacing.WEST, 0, 0, 0, 0, 1, 0);
		checkCoordinates(EnumFacing.WEST, 0, 0, 1, 1, 1, 0);
		checkCoordinates(EnumFacing.WEST, 0.1, 0.1, 0.1, 0.1, 0.9, -0.1);
	}

	@Test
	public void testDefaultTopMapping() {
		checkCoordinates(EnumFacing.UP, 0, 1, 0, 0, 0, 0);
		checkCoordinates(EnumFacing.UP, 1, 1, 0, 1, 0, 0);
		checkCoordinates(EnumFacing.UP, 0, 1, 1, 0, 1, 0);
		checkCoordinates(EnumFacing.UP, 1, 1, 1, 1, 1, 0);
		checkCoordinates(EnumFacing.UP, 0.1, 1.1, 0.1, 0.1, 0.1, 0.1);
	}

	@Test
	public void testDefaultBottomMapping() {
		checkCoordinates(EnumFacing.DOWN, 0, 0, 0, 0, 0, 0);
		checkCoordinates(EnumFacing.DOWN, 1, 0, 0, 1, 0, 0);
		checkCoordinates(EnumFacing.DOWN, 0, 0, 1, 0, 1, 0);
		checkCoordinates(EnumFacing.DOWN, 1, 0, 1, 1, 1, 0);
		checkCoordinates(EnumFacing.DOWN, 0.1, 0.1, 0.1, 0.1, 0.1, -0.1);
	}

}
