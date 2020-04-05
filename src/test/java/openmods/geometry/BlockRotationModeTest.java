package openmods.geometry;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.util.Direction;
import openmods.block.BlockRotationMode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

public class BlockRotationModeTest {

	private static final Set<Direction> HORIZONTALS = Arrays.stream(Direction.values()).filter(d -> d.getAxis().isHorizontal()).collect(Collectors.toSet());

	@RunWith(Parameterized.class)
	public static class BasicProperties {
		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(BlockRotationMode.values());
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void testSurfacePlacementConsistency() {
			for (Direction facing : Direction.values()) {
				Orientation orientation = mode.getOrientationFacing(facing);
				if (orientation != null)
					Assert.assertTrue("Orientation " + orientation + " is not valid",
							mode.getValidDirections().contains(orientation));
			}
		}

		@Test
		public void testLocalDirections() {
			for (Direction facing : Direction.values()) {
				Orientation orientation = mode.getOrientationFacing(facing);
				if (orientation != null)
					Assert.assertNotNull("Orientation " + orientation + " is not valid",
							mode.getLocalDirections(orientation));
			}
		}

		@Test
		public void testSerializationConsistency() {
			for (Orientation o : mode.getValidDirections()) {
				final int value = mode.toValue(o);
				Assert.assertTrue(o.toString(), value >= 0);
				Assert.assertTrue(o.toString(), value < mode.getValidDirections().size());

				final Orientation deserialized = mode.fromValue(value);
				Assert.assertEquals(o.toString(), o, deserialized);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class OnlyHorizontalsSupported {
		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.FOUR_DIRECTIONS,
					BlockRotationMode.TWO_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void testHorizontals() {
			for (Direction facing : HORIZONTALS) {
				Orientation orientation = mode.getOrientationFacing(facing);
				Assert.assertNotNull(facing.toString(), orientation);
			}
		}

		@Test
		public void testUp() {
			Orientation orientation = mode.getOrientationFacing(Direction.UP);
			Assert.assertNull(orientation);
		}

		@Test
		public void testDown() {
			Orientation orientation = mode.getOrientationFacing(Direction.DOWN);
			Assert.assertNull(orientation);
		}
	}

	@RunWith(Parameterized.class)
	public static class AllDirectionsSupported {
		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.NONE,
					BlockRotationMode.THREE_DIRECTIONS,
					BlockRotationMode.SIX_DIRECTIONS,
					BlockRotationMode.THREE_FOUR_DIRECTIONS,
					BlockRotationMode.TWELVE_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void testAllDirectionsNonNull() {
			for (Direction facing : Direction.values()) {
				Orientation orientation = mode.getOrientationFacing(facing);
				Assert.assertNotNull(facing.toString(), orientation);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class TopIsHorizontalForTopAndBottomFacings {
		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.THREE_DIRECTIONS,
					BlockRotationMode.SIX_DIRECTIONS,
					BlockRotationMode.THREE_FOUR_DIRECTIONS,
					BlockRotationMode.TWELVE_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		private static void testTopIsHorizontalForVerticalFacing(BlockRotationMode mode, Direction facing) {
			final Orientation orientation = mode.getOrientationFacing(facing);
			Assert.assertNotNull(facing.toString(), orientation);
			Assert.assertTrue(HORIZONTALS.contains(mode.getTop(orientation)));
		}

		@Test
		public void testTopIsHorizontalForUpFacing() {
			testTopIsHorizontalForVerticalFacing(mode, Direction.UP);
		}

		@Test
		public void testTopIsHorizontalForDownFacing() {
			testTopIsHorizontalForVerticalFacing(mode, Direction.DOWN);
		}
	}

	@RunWith(Parameterized.class)
	public static class SurfacePlacementFrontWeakConsistency {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.TWO_DIRECTIONS,
					BlockRotationMode.THREE_DIRECTIONS,
					BlockRotationMode.THREE_FOUR_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		// front should be on the same axis as placement side
		@Test
		public void testSurfacePlacementFrontWeakConsistency() {
			for (Direction facing : Direction.values()) {
				Orientation orientation = mode.getOrientationFacing(facing);
				if (orientation != null) {
					Assert.assertEquals("Orientation " + orientation + " has invalid placement/front combination",
							facing.getAxis(), mode.getFront(orientation).getAxis());
				}
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class SurfacePlacementFrontStrongConsistency {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.FOUR_DIRECTIONS,
					BlockRotationMode.SIX_DIRECTIONS,
					BlockRotationMode.TWELVE_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		// front should be on the same direction as placement side
		@Test
		public void testSurfacePlacementFrontStrongConsistency() {
			for (Direction facing : Direction.values()) {
				Orientation orientation = mode.getOrientationFacing(facing);
				if (orientation != null) {
					Assert.assertEquals("Orientation " + orientation + " has invalid placement/front combination",
							facing, mode.getFront(orientation));
				}
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class TopOrientationForHorizontals {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.TWO_DIRECTIONS,
					BlockRotationMode.THREE_DIRECTIONS,
					BlockRotationMode.FOUR_DIRECTIONS,
					BlockRotationMode.SIX_DIRECTIONS,
					BlockRotationMode.THREE_FOUR_DIRECTIONS,
					BlockRotationMode.TWELVE_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void testTopEqualsUpForAllHorizontals() {
			for (Direction facing : HORIZONTALS) {
				final Orientation orientation = mode.getOrientationFacing(facing);
				Assert.assertNotNull(facing.toString(), orientation);
				Assert.assertEquals(facing.toString(), Direction.UP, mode.getTop(orientation));
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class NonRotatingFrontClick {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.TWO_DIRECTIONS,
					BlockRotationMode.THREE_DIRECTIONS,
					BlockRotationMode.THREE_FOUR_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void toolRotationOnFrontDoesntChangeFront() {
			for (Orientation orientation : mode.getValidDirections()) {
				Direction front = mode.getFront(orientation);
				final Orientation rotatedOrientation = mode.calculateToolRotation(orientation, front);
				Direction rotatedFront = mode.getFront(rotatedOrientation);
				Assert.assertEquals(front, rotatedFront);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class SwitchingFrontClick {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.FOUR_DIRECTIONS,
					BlockRotationMode.SIX_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void toolRotationOnFrontChangesFrontToOpposite() {
			for (Orientation orientation : mode.getValidDirections()) {
				Direction front = mode.getFront(orientation);
				final Orientation rotatedOrientation = mode.calculateToolRotation(orientation, front);
				Direction rotatedFront = mode.getFront(rotatedOrientation);
				Assert.assertEquals(front.getOpposite(), rotatedFront);
			}
		}
	}

	public static void checkFrontDirectionsAfterFourRotations(BlockRotationMode mode, Direction clickedSide, Orientation orientation, Direction... expectedDirections) {
		final Set<Direction> results = Sets.newHashSet();
		for (int i = 0; i < 4; i++) {
			orientation = mode.calculateToolRotation(orientation, clickedSide);
			Assert.assertTrue(mode.isOrientationValid(orientation));
			results.add(mode.getFront(orientation));
		}

		Assert.assertEquals(Sets.newHashSet(expectedDirections), results);
	}

	public static void checkTopDirectionsAfterFourRotations(BlockRotationMode mode, Direction clickedSide, Orientation orientation, Direction... expectedDirections) {
		checkTopDirectionsAfterFourRotations(mode, clickedSide, orientation, Sets.newHashSet(expectedDirections));
	}

	public static void checkTopDirectionsAfterFourRotations(BlockRotationMode mode, Direction clickedSide, Orientation orientation, Set<Direction> expectedDirections) {
		final Set<Direction> results = Sets.newHashSet();
		for (int i = 0; i < 4; i++) {
			orientation = mode.calculateToolRotation(orientation, clickedSide);
			Assert.assertTrue(mode.isOrientationValid(orientation));
			results.add(mode.getTop(orientation));
		}

		Assert.assertEquals(expectedDirections, results);
	}

	@RunWith(Parameterized.class)
	public static class ToolRotationForWeakConsistency {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.TWO_DIRECTIONS,
					BlockRotationMode.THREE_DIRECTIONS,
					BlockRotationMode.THREE_FOUR_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void toolRotationOnBackDoesntChangeFront() {
			for (Orientation orientation : mode.getValidDirections()) {
				final Direction front = mode.getFront(orientation);
				final Direction back = front.getOpposite();
				final Orientation rotatedOrientation = mode.calculateToolRotation(orientation, back);
				final Direction rotatedFront = mode.getFront(rotatedOrientation);
				Assert.assertEquals(front, rotatedFront);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class ToolRotationForStrongConsistency {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.FOUR_DIRECTIONS,
					BlockRotationMode.SIX_DIRECTIONS,
					BlockRotationMode.TWELVE_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void toolRotationOnBackChangesFrontToOpposite() {
			for (Orientation orientation : mode.getValidDirections()) {
				final Direction back = mode.getFront(orientation).getOpposite();
				final Orientation rotatedOrientation = mode.calculateToolRotation(orientation, back);
				final Direction rotatedFront = mode.getFront(rotatedOrientation);
				Assert.assertEquals(back, rotatedFront);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class UniqueOrientationForEverySide {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.TWO_DIRECTIONS,
					BlockRotationMode.THREE_DIRECTIONS,
					BlockRotationMode.FOUR_DIRECTIONS,
					BlockRotationMode.SIX_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void exactlyOneOrientationForEveryFacing() {
			final Map<Direction, Orientation> stateMap = Maps.newHashMap();
			for (Orientation orientation : mode.getValidDirections()) {
				final Direction front = mode.getFront(orientation);
				final Orientation prev = stateMap.put(front, orientation);
				Assert.assertNull(prev);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class UniqueOrientationForEverySideNotToggling {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.TWO_DIRECTIONS,
					BlockRotationMode.THREE_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void toolRotationOnFrontDoesntChangeOrientation() {
			for (Orientation orientation : mode.getValidDirections()) {
				Direction front = mode.getFront(orientation);
				final Orientation rotatedOrientation = mode.calculateToolRotation(orientation, front);
				Assert.assertEquals(orientation, rotatedOrientation);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class ExactHorizontalRotation {

		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(
					BlockRotationMode.SIX_DIRECTIONS,
					BlockRotationMode.TWELVE_DIRECTIONS);
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void toolRotationOnHorizontalsChangesFrontToClickedSide() {
			for (Orientation initialOrientation : mode.getValidDirections()) {
				for (Direction rotatedSide : HORIZONTALS) {
					Orientation orientation = initialOrientation;
					final Set<Orientation> results = Sets.newHashSet();
					for (int i = 0; i < 4; i++) {
						final Direction preRotationFront = mode.getFront(orientation);
						orientation = mode.calculateToolRotation(orientation, rotatedSide);
						final Direction postRotationFront = mode.getFront(orientation);
						Assert.assertTrue(mode.isOrientationValid(orientation));
						if (preRotationFront == rotatedSide)
							Assert.assertEquals(postRotationFront, rotatedSide.getOpposite());
						else
							Assert.assertEquals(postRotationFront, rotatedSide);
						results.add(orientation);
					}

					Assert.assertEquals("Multiple states: " + results, 2, results.size());
				}
			}
		}
	}

	public static class TwoDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.TWO_DIRECTIONS;

		@Test
		public void toolRotationOnHorizontalChangesFrontAxisToClickedSide() {
			for (Orientation orientation : MODE.getValidDirections()) {
				for (Direction rotatedSide : HORIZONTALS) {
					final Orientation rotatedOrientation = MODE.calculateToolRotation(orientation, rotatedSide);
					final Direction rotatedFront = MODE.getFront(rotatedOrientation);
					Assert.assertEquals(rotatedSide.getAxis(), rotatedFront.getAxis());
				}
			}
		}

		@Test
		public void toolTopRotationCoversHorizontals() {
			checkFrontDirectionsAfterFourRotations(MODE, Direction.UP, Orientation.XP_YP, Direction.NORTH, Direction.WEST);
		}

		@Test
		public void toolBottomRotationCoversHorizontals() {
			checkFrontDirectionsAfterFourRotations(MODE, Direction.DOWN, Orientation.XP_YP, Direction.NORTH, Direction.WEST);
		}
	}

	public static class ThreeDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.THREE_DIRECTIONS;

		@Test
		public void toolRotationOnAnySideChangesFrontAxisToClickedSide() {
			for (Orientation orientation : MODE.getValidDirections()) {
				for (Direction rotatedSide : Direction.values()) {
					final Orientation rotatedOrientation = MODE.calculateToolRotation(orientation, rotatedSide);
					final Direction rotatedFront = MODE.getFront(rotatedOrientation);
					Assert.assertEquals(rotatedSide.getAxis(), rotatedFront.getAxis());
				}
			}
		}
	}

	public static class FourDirections {
		private static final BlockRotationMode MODE = BlockRotationMode.FOUR_DIRECTIONS;

		@Test
		public void toolTopRotationCoversHorizontals() {
			checkFrontDirectionsAfterFourRotations(MODE, Direction.UP, Orientation.XP_YP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
		}

		@Test
		public void toolBottomRotationCoversHorizontals() {
			checkFrontDirectionsAfterFourRotations(MODE, Direction.DOWN, Orientation.XP_YP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
		}
	}

	public static class SixDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.SIX_DIRECTIONS;

		@Test
		public void toolRotationOnAnySideChangesFrontToClickedSideOrOpposite() {
			for (Orientation orientation : MODE.getValidDirections()) {
				for (Direction rotatedSide : Direction.values()) {
					checkFrontDirectionsAfterFourRotations(MODE, rotatedSide, orientation, rotatedSide, rotatedSide.getOpposite());
				}
			}
		}
	}

	public static class ThreeFourDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.THREE_FOUR_DIRECTIONS;

		@Test
		public void toolRotationOnTopCoversHorizontals() {
			checkTopDirectionsAfterFourRotations(MODE, Direction.UP, Orientation.XP_YP, HORIZONTALS);
			checkFrontDirectionsAfterFourRotations(MODE, Direction.UP, Orientation.XP_YP, Direction.UP);
		}

		@Test
		public void toolRotationOnBottomCoversHorizontals() {
			checkTopDirectionsAfterFourRotations(MODE, Direction.DOWN, Orientation.XP_YP, HORIZONTALS);
			checkFrontDirectionsAfterFourRotations(MODE, Direction.DOWN, Orientation.XP_YP, Direction.UP);
		}

		@Test
		public void toolRotationOnEastCoversFourSides() {
			checkTopDirectionsAfterFourRotations(MODE, Direction.EAST, Orientation.XP_YP, Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN);
			checkFrontDirectionsAfterFourRotations(MODE, Direction.EAST, Orientation.XP_YP, Direction.WEST);
		}

		@Test
		public void toolRotationOnWestCoversFourSides() {
			checkTopDirectionsAfterFourRotations(MODE, Direction.WEST, Orientation.XP_YP, Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN);
			checkFrontDirectionsAfterFourRotations(MODE, Direction.WEST, Orientation.XP_YP, Direction.WEST);
		}

		@Test
		public void toolRotationOnNorthCoversFourSides() {
			checkTopDirectionsAfterFourRotations(MODE, Direction.NORTH, Orientation.XP_YP, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN);
			checkFrontDirectionsAfterFourRotations(MODE, Direction.NORTH, Orientation.XP_YP, Direction.NORTH);
		}

		@Test
		public void toolRotationOnSouthCoversFourSides() {
			checkTopDirectionsAfterFourRotations(MODE, Direction.SOUTH, Orientation.XP_YP, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN);
			checkFrontDirectionsAfterFourRotations(MODE, Direction.SOUTH, Orientation.XP_YP, Direction.NORTH);
		}
	}

	public static class TwelveDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.TWELVE_DIRECTIONS;

		@Test
		public void toolRotationOnTopCoversHorizontals() {
			checkTopDirectionsAfterFourRotations(MODE, Direction.UP, Orientation.XP_YP, HORIZONTALS);
			checkFrontDirectionsAfterFourRotations(MODE, Direction.UP, Orientation.XP_YP, Direction.UP);
		}

		@Test
		public void toolRotationOnBottomCoversHorizontals() {
			checkTopDirectionsAfterFourRotations(MODE, Direction.DOWN, Orientation.XP_YP, HORIZONTALS);
			checkFrontDirectionsAfterFourRotations(MODE, Direction.DOWN, Orientation.XP_YP, Direction.DOWN);
		}

		@Test
		public void toolRotationOnHorizontalChangesFrontToOpposite() {
			for (Direction front : HORIZONTALS) {
				Orientation orientation = MODE.getOrientationFacing(front);
				final Orientation rotatedOrientation = MODE.calculateToolRotation(orientation, front);
				Direction rotatedFront = MODE.getFront(rotatedOrientation);
				Assert.assertEquals(front.getOpposite(), rotatedFront);
			}
		}
	}

}
