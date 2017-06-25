package openmods.geometry;

import org.junit.Assert;
import org.junit.Test;

public class Permutation2dTestSuite {

	void testApply(Permutation2d transform, int... expected) {
		int[] actual = new int[expected.length];
		for (int i = 0; i < expected.length; i++)
			actual[i] = transform.apply(i);

		Assert.assertArrayEquals(expected, actual);
	}

	@Test
	public void testIdentityCompose() {
		final Permutation2d identity = Permutation2d.identity(2, 3);
		Assert.assertEquals(identity, identity.compose(identity));
	}

	@Test
	public void testMirrorHorizontal() {
		{
			final Permutation2d mirror = Permutation2d.identity(2, 3).mirrorHorizontal();
			testApply(mirror, 4, 5, 2, 3, 0, 1);
		}

		{
			final Permutation2d mirror = Permutation2d.identity(3, 2).mirrorHorizontal();
			testApply(mirror, 3, 4, 5, 0, 1, 2);
		}
	}

	@Test
	public void testMirrorHorizontalCompose() {
		{
			final Permutation2d mirror = Permutation2d.identity(2, 3).mirrorHorizontal().mirrorHorizontal();
			final Permutation2d identity = Permutation2d.identity(2, 3);
			Assert.assertEquals(identity, mirror);
		}

		{
			final Permutation2d mirror = Permutation2d.identity(2, 3).mirrorHorizontal();
			final Permutation2d identity = Permutation2d.identity(2, 3);
			Assert.assertEquals(identity, identity.compose(mirror).compose(mirror));
		}

		{
			final Permutation2d mirror = Permutation2d.identity(3, 2).mirrorHorizontal().mirrorHorizontal();
			final Permutation2d identity = Permutation2d.identity(3, 2);
			Assert.assertEquals(identity, mirror);
		}

		{
			final Permutation2d mirror = Permutation2d.identity(3, 2).mirrorHorizontal();
			final Permutation2d identity = Permutation2d.identity(3, 2);
			Assert.assertEquals(identity, identity.compose(mirror).compose(mirror));
		}
	}

	@Test
	public void testMirrorVertical() {
		{
			final Permutation2d mirror = Permutation2d.identity(2, 3).mirrorVertical();
			testApply(mirror, 1, 0, 3, 2, 5, 4);
		}

		{
			final Permutation2d mirror = Permutation2d.identity(3, 2).mirrorVertical();
			testApply(mirror, 2, 1, 0, 5, 4, 3);
		}
	}

	@Test
	public void testMirrorVerticalCompose() {
		{
			final Permutation2d mirror = Permutation2d.identity(2, 3).mirrorVertical().mirrorVertical();
			final Permutation2d identity = Permutation2d.identity(2, 3);
			Assert.assertEquals(identity, mirror);
		}

		{
			final Permutation2d mirror = Permutation2d.identity(2, 3).mirrorVertical();
			final Permutation2d identity = Permutation2d.identity(2, 3);
			Assert.assertEquals(identity, identity.compose(mirror).compose(mirror));
		}

		{
			final Permutation2d mirror = Permutation2d.identity(3, 2).mirrorVertical().mirrorVertical();
			final Permutation2d identity = Permutation2d.identity(3, 2);
			Assert.assertEquals(identity, mirror);
		}

		{
			final Permutation2d mirror = Permutation2d.identity(3, 2).mirrorVertical();
			final Permutation2d identity = Permutation2d.identity(3, 2);
			Assert.assertEquals(identity, identity.compose(mirror).compose(mirror));
		}
	}

	@Test
	public void testMirrorVerticalHorizontalCompose() {
		{
			final Permutation2d mirrorVH = Permutation2d.identity(2, 3).mirrorVertical().mirrorHorizontal();
			final Permutation2d mirrorHV = Permutation2d.identity(2, 3).mirrorHorizontal().mirrorVertical();
			Assert.assertEquals(mirrorHV, mirrorVH);
			testApply(mirrorHV, 5, 4, 3, 2, 1, 0);
		}

		{
			final Permutation2d mirrorVH = Permutation2d.identity(3, 2).mirrorVertical().mirrorHorizontal();
			final Permutation2d mirrorHV = Permutation2d.identity(3, 2).mirrorHorizontal().mirrorVertical();
			Assert.assertEquals(mirrorHV, mirrorVH);
			testApply(mirrorHV, 5, 4, 3, 2, 1, 0);
		}
	}

	@Test
	public void testTransposition() {
		{
			final Permutation2d transpose = Permutation2d.identity(2, 3).transpose();
			Assert.assertEquals(3, transpose.width);
			Assert.assertEquals(2, transpose.height);
			testApply(transpose, 0, 2, 4, 1, 3, 5);
		}

		{
			final Permutation2d transpose = Permutation2d.identity(3, 2).transpose();
			Assert.assertEquals(2, transpose.width);
			Assert.assertEquals(3, transpose.height);
			testApply(transpose, 0, 3, 1, 4, 2, 5);
		}
	}

	@Test
	public void testTranspositionComposition() {
		{
			final Permutation2d transpose = Permutation2d.identity(2, 3).transpose().transpose();
			final Permutation2d identity = Permutation2d.identity(2, 3);
			Assert.assertEquals(identity, transpose);
		}

		{
			final Permutation2d transpose = Permutation2d.identity(3, 2).transpose().transpose();
			final Permutation2d identity = Permutation2d.identity(3, 2);
			Assert.assertEquals(identity, transpose);
		}
	}

	@Test
	public void testCwRotation() {
		{
			final Permutation2d rotate = Permutation2d.identity(3, 4).rotateCW();
			Assert.assertEquals(4, rotate.width);
			Assert.assertEquals(3, rotate.height);
			testApply(rotate, 9, 6, 3, 0, 10, 7, 4, 1, 11, 8, 5, 2);
		}

		{
			final Permutation2d rotate = Permutation2d.identity(4, 3).rotateCW();
			Assert.assertEquals(3, rotate.width);
			Assert.assertEquals(4, rotate.height);
			testApply(rotate, 8, 4, 0, 9, 5, 1, 10, 6, 2, 11, 7, 3);
		}
	}

	@Test
	public void testCcwRotation() {
		{
			final Permutation2d rotate = Permutation2d.identity(3, 4).rotateCCW();
			Assert.assertEquals(4, rotate.width);
			Assert.assertEquals(3, rotate.height);
			testApply(rotate, 2, 5, 8, 11, 1, 4, 7, 10, 0, 3, 6, 9);
		}

		{
			final Permutation2d rotate = Permutation2d.identity(4, 3).rotateCCW();
			Assert.assertEquals(3, rotate.width);
			Assert.assertEquals(4, rotate.height);
			testApply(rotate, 3, 7, 11, 2, 6, 10, 1, 5, 9, 0, 4, 8);
		}
	}

	@Test
	public void testRotation1vs1() {
		{
			final Permutation2d identity = Permutation2d.identity(3, 4);
			final Permutation2d r1 = Permutation2d.identity(3, 4).rotateCCW().rotateCW();
			final Permutation2d r2 = Permutation2d.identity(3, 4).rotateCW().rotateCCW();
			Assert.assertEquals(identity, r1);
			Assert.assertEquals(identity, r2);
		}

		{
			final Permutation2d identity = Permutation2d.identity(4, 3);
			final Permutation2d r1 = Permutation2d.identity(4, 3).rotateCCW().rotateCW();
			final Permutation2d r2 = Permutation2d.identity(4, 3).rotateCW().rotateCCW();
			Assert.assertEquals(identity, r1);
			Assert.assertEquals(identity, r2);
		}
	}

	@Test
	public void testRotation2vs2() {

		{
			final Permutation2d r1 = Permutation2d.identity(3, 4).rotateCCW().rotateCCW();
			final Permutation2d r2 = Permutation2d.identity(3, 4).rotateCW().rotateCW();
			Assert.assertEquals(r1, r2);
		}

		{
			final Permutation2d r1 = Permutation2d.identity(4, 3).rotateCCW().rotateCCW();
			final Permutation2d r2 = Permutation2d.identity(4, 3).rotateCW().rotateCW();
			Assert.assertEquals(r1, r2);
		}
	}

	@Test
	public void testRotation3vs1() {
		{
			final Permutation2d r1 = Permutation2d.identity(3, 4).rotateCCW().rotateCCW().rotateCCW();
			final Permutation2d r2 = Permutation2d.identity(3, 4).rotateCW();
			Assert.assertEquals(r1, r2);
		}

		{
			final Permutation2d r1 = Permutation2d.identity(4, 3).rotateCCW().rotateCCW().rotateCCW();
			final Permutation2d r2 = Permutation2d.identity(4, 3).rotateCW();
			Assert.assertEquals(r1, r2);
		}
	}

	@Test
	public void testFourRotations() {
		{
			final Permutation2d identity = Permutation2d.identity(3, 4);
			final Permutation2d r1 = Permutation2d.identity(3, 4).rotateCCW().rotateCCW().rotateCCW().rotateCCW();
			final Permutation2d r2 = Permutation2d.identity(3, 4).rotateCW().rotateCW().rotateCW().rotateCW();
			Assert.assertEquals(identity, r1);
			Assert.assertEquals(identity, r2);
		}

		{
			final Permutation2d identity = Permutation2d.identity(4, 3);
			final Permutation2d r1 = Permutation2d.identity(4, 3).rotateCCW().rotateCCW().rotateCCW().rotateCCW();
			final Permutation2d r2 = Permutation2d.identity(4, 3).rotateCW().rotateCW().rotateCW().rotateCW();
			Assert.assertEquals(identity, r1);
			Assert.assertEquals(identity, r2);
		}
	}

	@Test
	public void testReversion() {
		{
			final Permutation2d reverse = Permutation2d.identity(3, 4).reverse();
			Assert.assertEquals(3, reverse.width);
			Assert.assertEquals(4, reverse.height);
			testApply(reverse, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0);
		}

		{
			final Permutation2d reverse = Permutation2d.identity(4, 3).reverse();
			Assert.assertEquals(4, reverse.width);
			Assert.assertEquals(3, reverse.height);
			testApply(reverse, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0);
		}
	}

	@Test
	public void testReversionComposition() {
		{
			final Permutation2d transpose = Permutation2d.identity(3, 4).reverse().reverse();
			final Permutation2d identity = Permutation2d.identity(3, 4);
			Assert.assertEquals(identity, transpose);
		}

		{
			final Permutation2d transpose = Permutation2d.identity(4, 3).reverse().reverse();
			final Permutation2d identity = Permutation2d.identity(4, 3);
			Assert.assertEquals(identity, transpose);
		}
	}
}
