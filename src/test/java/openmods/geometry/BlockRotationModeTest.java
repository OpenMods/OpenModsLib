package openmods.geometry;

import net.minecraft.util.EnumFacing;
import openmods.block.BlockRotationMode;
import org.junit.Assert;
import org.junit.Test;

public class BlockRotationModeTest {

	@Test
	public void testSurfacePlacementConsistency() {
		for (BlockRotationMode mode : BlockRotationMode.values()) {
			for (EnumFacing facing : EnumFacing.values()) {
				Orientation orientation = mode.getPlacementOrientationFromSurface(facing);
				Assert.assertTrue("Orientation " + orientation + " is not valid for " + mode,
						orientation == null || mode.validDirections.contains(orientation));
			}
		}
	}

	// front should be on the same axis as placement side
	private static void testSurfacePlacementFrontWeakConsistency(BlockRotationMode mode) {
		for (EnumFacing facing : EnumFacing.values()) {
			Orientation orientation = mode.getPlacementOrientationFromSurface(facing);
			if (orientation != null) {
				Assert.assertEquals("Orientation " + orientation + " has invalid placement/front combination",
						facing.getAxis(), mode.getFront(orientation).getAxis());
			}
		}
	}

	// front should be on the same direction as placement side
	private static void testSurfacePlacementFrontStrongConsistency(BlockRotationMode mode) {
		for (EnumFacing facing : EnumFacing.values()) {
			Orientation orientation = mode.getPlacementOrientationFromSurface(facing);
			if (orientation != null) {
				Assert.assertEquals("Orientation " + orientation + " has invalid placement/front combination",
						facing, mode.getFront(orientation));
			}
		}
	}

	@Test
	public void testSurfacePlacementFrontConsistencyTwoDirections() {
		testSurfacePlacementFrontWeakConsistency(BlockRotationMode.TWO_DIRECTIONS);
	}

	@Test
	public void testSurfacePlacementFrontConsistencyThreeDirections() {
		testSurfacePlacementFrontWeakConsistency(BlockRotationMode.THREE_DIRECTIONS);
	}

	@Test
	public void testSurfacePlacementFrontConsistencyFourDirections() {
		testSurfacePlacementFrontStrongConsistency(BlockRotationMode.FOUR_DIRECTIONS);
	}

	@Test
	public void testSurfacePlacementFrontConsistencySixDirections() {
		testSurfacePlacementFrontStrongConsistency(BlockRotationMode.SIX_DIRECTIONS);
	}

	@Test
	public void testSurfacePlacementFrontConsistencySixDirectionsLegacy() {
		testSurfacePlacementFrontStrongConsistency(BlockRotationMode.SIX_DIRECTIONS_LEGACY);
	}

	@Test
	public void testSurfacePlacementFrontConsistencyThreeFourDirections() {
		testSurfacePlacementFrontWeakConsistency(BlockRotationMode.THREE_FOUR_DIRECTIONS);
	}

	@Test
	public void testSurfacePlacementFrontConsistencyTwelveDirections() {
		testSurfacePlacementFrontStrongConsistency(BlockRotationMode.TWELVE_DIRECTIONS);
	}
}
