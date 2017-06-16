package openmods.geometry;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.EnumFacing;
import openmods.block.BlockRotationMode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

public class BlockRotationModeTest {

	private static final Set<EnumFacing> HORIZONTALS = Sets.newHashSet(EnumFacing.HORIZONTALS);

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
			for (EnumFacing facing : EnumFacing.values()) {
				Orientation orientation = mode.getOrientationFacing(facing);
				if (orientation != null)
					Assert.assertTrue("Orientation " + orientation + " is not valid",
							mode.getValidDirections().contains(orientation));
			}
		}

		@Test
		public void testLocalDirections() {
			for (EnumFacing facing : EnumFacing.values()) {
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
			for (EnumFacing facing : EnumFacing.HORIZONTALS) {
				Orientation orientation = mode.getOrientationFacing(facing);
				Assert.assertNotNull(facing.toString(), orientation);
			}
		}

		@Test
		public void testUp() {
			Orientation orientation = mode.getOrientationFacing(EnumFacing.UP);
			Assert.assertNull(orientation);
		}

		@Test
		public void testDown() {
			Orientation orientation = mode.getOrientationFacing(EnumFacing.DOWN);
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
			for (EnumFacing facing : EnumFacing.values()) {
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

		private static void testTopIsHorizontalForVerticalFacing(BlockRotationMode mode, EnumFacing facing) {
			final Orientation orientation = mode.getOrientationFacing(facing);
			Assert.assertNotNull(facing.toString(), orientation);
			Assert.assertTrue(HORIZONTALS.contains(mode.getTop(orientation)));
		}

		@Test
		public void testTopIsHorizontalForUpFacing() {
			testTopIsHorizontalForVerticalFacing(mode, EnumFacing.UP);
		}

		@Test
		public void testTopIsHorizontalForDownFacing() {
			testTopIsHorizontalForVerticalFacing(mode, EnumFacing.DOWN);
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
			for (EnumFacing facing : EnumFacing.values()) {
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
			for (EnumFacing facing : EnumFacing.values()) {
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
			for (EnumFacing facing : EnumFacing.HORIZONTALS) {
				final Orientation orientation = mode.getOrientationFacing(facing);
				Assert.assertNotNull(facing.toString(), orientation);
				Assert.assertEquals(facing.toString(), EnumFacing.UP, mode.getTop(orientation));
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
				EnumFacing front = mode.getFront(orientation);
				final Orientation rotatedOrientation = mode.calculateToolRotation(orientation, front);
				EnumFacing rotatedFront = mode.getFront(rotatedOrientation);
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
				EnumFacing front = mode.getFront(orientation);
				final Orientation rotatedOrientation = mode.calculateToolRotation(orientation, front);
				EnumFacing rotatedFront = mode.getFront(rotatedOrientation);
				Assert.assertEquals(front.getOpposite(), rotatedFront);
			}
		}
	}

	public static void checkFrontDirectionsAfterFourRotations(BlockRotationMode mode, EnumFacing clickedSide, Orientation orientation, EnumFacing... expectedDirections) {
		final Set<EnumFacing> results = Sets.newHashSet();
		for (int i = 0; i < 4; i++) {
			orientation = mode.calculateToolRotation(orientation, clickedSide);
			Assert.assertTrue(mode.isOrientationValid(orientation));
			results.add(mode.getFront(orientation));
		}

		Assert.assertEquals(Sets.newHashSet(expectedDirections), results);
	}

	public static void checkTopDirectionsAfterFourRotations(BlockRotationMode mode, EnumFacing clickedSide, Orientation orientation, EnumFacing... expectedDirections) {
		final Set<EnumFacing> results = Sets.newHashSet();
		for (int i = 0; i < 4; i++) {
			orientation = mode.calculateToolRotation(orientation, clickedSide);
			Assert.assertTrue(mode.isOrientationValid(orientation));
			results.add(mode.getTop(orientation));
		}

		Assert.assertEquals(Sets.newHashSet(expectedDirections), results);
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
				final EnumFacing front = mode.getFront(orientation);
				final EnumFacing back = front.getOpposite();
				final Orientation rotatedOrientation = mode.calculateToolRotation(orientation, back);
				final EnumFacing rotatedFront = mode.getFront(rotatedOrientation);
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
				final EnumFacing back = mode.getFront(orientation).getOpposite();
				final Orientation rotatedOrientation = mode.calculateToolRotation(orientation, back);
				final EnumFacing rotatedFront = mode.getFront(rotatedOrientation);
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
			final Map<EnumFacing, Orientation> stateMap = Maps.newHashMap();
			for (Orientation orientation : mode.getValidDirections()) {
				final EnumFacing front = mode.getFront(orientation);
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
				EnumFacing front = mode.getFront(orientation);
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
				for (EnumFacing rotatedSide : EnumFacing.HORIZONTALS) {
					Orientation orientation = initialOrientation;
					final Set<Orientation> results = Sets.newHashSet();
					for (int i = 0; i < 4; i++) {
						final EnumFacing preRotationFront = mode.getFront(orientation);
						orientation = mode.calculateToolRotation(orientation, rotatedSide);
						final EnumFacing postRotationFront = mode.getFront(orientation);
						Assert.assertTrue(mode.isOrientationValid(orientation));
						if (preRotationFront == rotatedSide)
							Assert.assertEquals(postRotationFront, rotatedSide.getOpposite());
						else
							Assert.assertEquals(postRotationFront, rotatedSide);
						results.add(orientation);
					}

					Assert.assertTrue("Multiple states: " + results, results.size() == 2);
				}
			}
		}
	}

	public static class TwoDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.TWO_DIRECTIONS;

		@Test
		public void toolRotationOnHorizontalChangesFrontAxisToClickedSide() {
			for (Orientation orientation : MODE.getValidDirections()) {
				for (EnumFacing rotatedSide : EnumFacing.HORIZONTALS) {
					final Orientation rotatedOrientation = MODE.calculateToolRotation(orientation, rotatedSide);
					final EnumFacing rotatedFront = MODE.getFront(rotatedOrientation);
					Assert.assertEquals(rotatedSide.getAxis(), rotatedFront.getAxis());
				}
			}
		}

		@Test
		public void toolTopRotationCoversHorizontals() {
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.UP, Orientation.XP_YP, EnumFacing.NORTH, EnumFacing.WEST);
		}

		@Test
		public void toolBottomRotationCoversHorizontals() {
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.DOWN, Orientation.XP_YP, EnumFacing.NORTH, EnumFacing.WEST);
		}
	}

	public static class ThreeDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.THREE_DIRECTIONS;

		@Test
		public void toolRotationOnAnySideChangesFrontAxisToClickedSide() {
			for (Orientation orientation : MODE.getValidDirections()) {
				for (EnumFacing rotatedSide : EnumFacing.VALUES) {
					final Orientation rotatedOrientation = MODE.calculateToolRotation(orientation, rotatedSide);
					final EnumFacing rotatedFront = MODE.getFront(rotatedOrientation);
					Assert.assertEquals(rotatedSide.getAxis(), rotatedFront.getAxis());
				}
			}
		}
	}

	public static class FourDirections {
		private static final BlockRotationMode MODE = BlockRotationMode.FOUR_DIRECTIONS;

		@Test
		public void toolTopRotationCoversHorizontals() {
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.UP, Orientation.XP_YP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST);
		}

		@Test
		public void toolBottomRotationCoversHorizontals() {
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.DOWN, Orientation.XP_YP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST);
		}
	}

	public static class SixDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.SIX_DIRECTIONS;

		@Test
		public void toolRotationOnAnySideChangesFrontToClickedSideOrOpposite() {
			for (Orientation orientation : MODE.getValidDirections()) {
				for (EnumFacing rotatedSide : EnumFacing.VALUES) {
					checkFrontDirectionsAfterFourRotations(MODE, rotatedSide, orientation, rotatedSide, rotatedSide.getOpposite());
				}
			}
		}
	}

	public static class ThreeFourDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.THREE_FOUR_DIRECTIONS;

		@Test
		public void toolRotationOnTopCoversHorizontals() {
			checkTopDirectionsAfterFourRotations(MODE, EnumFacing.UP, Orientation.XP_YP, EnumFacing.HORIZONTALS);
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.UP, Orientation.XP_YP, EnumFacing.UP);
		}

		@Test
		public void toolRotationOnBottomCoversHorizontals() {
			checkTopDirectionsAfterFourRotations(MODE, EnumFacing.DOWN, Orientation.XP_YP, EnumFacing.HORIZONTALS);
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.DOWN, Orientation.XP_YP, EnumFacing.UP);
		}

		@Test
		public void toolRotationOnEastCoversFourSides() {
			checkTopDirectionsAfterFourRotations(MODE, EnumFacing.EAST, Orientation.XP_YP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP, EnumFacing.DOWN);
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.EAST, Orientation.XP_YP, EnumFacing.WEST);
		}

		@Test
		public void toolRotationOnWestCoversFourSides() {
			checkTopDirectionsAfterFourRotations(MODE, EnumFacing.WEST, Orientation.XP_YP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP, EnumFacing.DOWN);
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.WEST, Orientation.XP_YP, EnumFacing.WEST);
		}

		@Test
		public void toolRotationOnNorthCoversFourSides() {
			checkTopDirectionsAfterFourRotations(MODE, EnumFacing.NORTH, Orientation.XP_YP, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN);
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.NORTH, Orientation.XP_YP, EnumFacing.NORTH);
		}

		@Test
		public void toolRotationOnSouthCoversFourSides() {
			checkTopDirectionsAfterFourRotations(MODE, EnumFacing.SOUTH, Orientation.XP_YP, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN);
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.SOUTH, Orientation.XP_YP, EnumFacing.NORTH);
		}
	}

	public static class TwelveDirections {

		private static final BlockRotationMode MODE = BlockRotationMode.TWELVE_DIRECTIONS;

		@Test
		public void toolRotationOnTopCoversHorizontals() {
			checkTopDirectionsAfterFourRotations(MODE, EnumFacing.UP, Orientation.XP_YP, EnumFacing.HORIZONTALS);
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.UP, Orientation.XP_YP, EnumFacing.UP);
		}

		@Test
		public void toolRotationOnBottomCoversHorizontals() {
			checkTopDirectionsAfterFourRotations(MODE, EnumFacing.DOWN, Orientation.XP_YP, EnumFacing.HORIZONTALS);
			checkFrontDirectionsAfterFourRotations(MODE, EnumFacing.DOWN, Orientation.XP_YP, EnumFacing.DOWN);
		}

		@Test
		public void toolRotationOnHorizontalChangesFrontToOpposite() {
			for (EnumFacing front : EnumFacing.HORIZONTALS) {
				Orientation orientation = MODE.getOrientationFacing(front);
				final Orientation rotatedOrientation = MODE.calculateToolRotation(orientation, front);
				EnumFacing rotatedFront = MODE.getFront(rotatedOrientation);
				Assert.assertEquals(front.getOpposite(), rotatedFront);
			}
		}
	}

}
