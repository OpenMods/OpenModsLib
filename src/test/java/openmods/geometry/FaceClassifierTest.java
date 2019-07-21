package openmods.geometry;

import com.google.common.collect.Lists;
import java.util.Optional;
import javax.vecmath.Vector3f;
import net.minecraft.util.Direction;
import org.junit.Assert;
import org.junit.Test;

public class FaceClassifierTest {

	private static void test(FaceClassifier classifier, Direction result, float x, float y, float z) {
		Vector3f v = new Vector3f(x, y, z);
		v.normalize();
		Assert.assertEquals(Optional.of(result), classifier.classify(v));
	}

	private static void test(FaceClassifier classifier, float x, float y, float z) {
		Vector3f v = new Vector3f(x, y, z);
		v.normalize();
		Assert.assertEquals(Optional.empty(), classifier.classify(v));
	}

	@Test
	public void testSingleDirection() {
		final FaceClassifier cut = new FaceClassifier(Lists.newArrayList(Direction.NORTH));
		test(cut, Direction.NORTH, 0, 0, -1);

		test(cut, Direction.NORTH, 0, -1, -1);
		test(cut, Direction.NORTH, 0, +1, -1);
		test(cut, Direction.NORTH, -1, 0, -1);
		test(cut, Direction.NORTH, +1, 0, -1);

		test(cut, Direction.NORTH, +1, +1, -1);
		test(cut, Direction.NORTH, +1, -1, -1);
		test(cut, Direction.NORTH, -1, +1, -1);
		test(cut, Direction.NORTH, -1, -1, -1);

		test(cut, Direction.NORTH, 0, 0, Math.nextUp(-1));
		// test(cut, EnumFacing.NORTH, 0, 0, Math.nextDown(-1)); // since Java 1.8
		test(cut, Direction.NORTH, 0, 0, Float.intBitsToFloat(Float.floatToRawIntBits(-1) + 1));

		test(cut, Direction.NORTH, 0, 5, -1);

		test(cut, 0, 0, +1);

		test(cut, Direction.NORTH, 0, -1, -Float.MIN_VALUE);
		test(cut, 0, -1, 0);
		test(cut, 0, -1, +Float.MIN_VALUE);

		test(cut, Direction.NORTH, 0, +1, -Float.MIN_VALUE);
		test(cut, 0, +1, 0);
		test(cut, 0, +1, +Float.MIN_VALUE);

		test(cut, -1, 0, 0);
		test(cut, +1, 0, 0);
	}

	@Test
	public void testNeigboringDirections() {
		final FaceClassifier cut = new FaceClassifier(Lists.newArrayList(Direction.NORTH, Direction.WEST));

		test(cut, Direction.NORTH, 0, 0, -1);
		test(cut, Direction.WEST, -1, 0, 0);

		test(cut, Direction.NORTH, -2, 0, -1);
		test(cut, Direction.NORTH, 2, 0, -1);

		test(cut, Direction.NORTH, -1, 0, -1);
		test(cut, Direction.NORTH, -1, 0.5f, -1);

		test(cut, 0, 0, +1);
		test(cut, +1, 0, 0);
		test(cut, 0, +1, 0);
		test(cut, 0, -1, 0);
	}

	@Test
	public void testNeigboringDirectionsSwitchedOrder() {
		final FaceClassifier cut = new FaceClassifier(Lists.newArrayList(Direction.WEST, Direction.NORTH));

		test(cut, Direction.NORTH, 0, 0, -1);
		test(cut, Direction.WEST, -1, 0, 0);

		test(cut, Direction.WEST, -2, 0, -1);
		test(cut, Direction.NORTH, 2, 0, -1);

		test(cut, Direction.WEST, -1, 0, -1);
		test(cut, Direction.WEST, -1, 0.5f, -1);

		test(cut, 0, 0, +1);
		test(cut, +1, 0, 0);
		test(cut, 0, +1, 0);
		test(cut, 0, -1, 0);
	}

	@Test
	public void testOppositeDirections() {
		final FaceClassifier cut = new FaceClassifier(Lists.newArrayList(Direction.NORTH, Direction.SOUTH));

		test(cut, Direction.NORTH, 0, 0, -1);
		test(cut, Direction.SOUTH, 0, 0, +1);

		test(cut, +1, 0, 0);
		test(cut, -1, 0, 0);
		test(cut, 0, +1, 0);
		test(cut, 0, -1, 0);
	}

}
