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

}
