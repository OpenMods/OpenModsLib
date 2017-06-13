package openmods.geometry;

import com.google.common.collect.Sets;
import java.util.Arrays;
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
	public static class SurfacePlacementFrontStringConsistency {

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
}
