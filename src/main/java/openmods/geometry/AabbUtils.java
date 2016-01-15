package openmods.geometry;

import net.minecraft.util.AxisAlignedBB;

public class AabbUtils {

	public static AxisAlignedBB createAabb(double x1, double y1, double z1, double x2, double y2, double z2) {
		final double minX;
		final double maxX;
		if (x1 > x2) {
			minX = x2;
			maxX = x1;
		} else {
			minX = x1;
			maxX = x2;
		}

		final double minY;
		final double maxY;
		if (y1 > y2) {
			minY = y2;
			maxY = y1;
		} else {
			minY = y1;
			maxY = y2;
		}

		final double minZ;
		final double maxZ;
		if (z1 > z2) {
			minZ = z2;
			maxZ = z1;
		} else {
			minZ = z1;
			maxZ = z2;
		}

		return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
	}

}
