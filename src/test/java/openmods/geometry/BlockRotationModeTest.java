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
	public static class PlacementConsistency {
		@Parameters(name = "{0}")
		public static Iterable<BlockRotationMode> dirs() {
			return Arrays.asList(BlockRotationMode.values());
		}

		@Parameter
		public BlockRotationMode mode;

		@Test
		public void testSurfacePlacementConsistency() {
			for (EnumFacing facing : EnumFacing.values()) {
				Orientation orientation = mode.getPlacementOrientationFromSurface(facing);
				Assert.assertTrue("Orientation " + orientation + " is not valid",
						orientation == null || mode.validDirections.contains(orientation));
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
			final Orientation orientation = mode.getPlacementOrientationFromSurface(facing);
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
				Orientation orientation = mode.getPlacementOrientationFromSurface(facing);
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
				Orientation orientation = mode.getPlacementOrientationFromSurface(facing);
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
				final Orientation orientation = mode.getPlacementOrientationFromSurface(facing);
				Assert.assertNotNull(facing.toString(), orientation);
				Assert.assertEquals(facing.toString(), EnumFacing.UP, mode.getTop(orientation));
			}
		}
	}
}
