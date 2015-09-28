package openmods.geometry;

import net.minecraft.util.AxisAlignedBB;

public class AabbUtils {

	public static AxisAlignedBB createAabb(double x1, double y1, double z1, double x2, double y2, double z2) {
		if (x1 > x2) {
			double tmp = x2;
			x2 = x1;
			x1 = tmp;
		}

		if (y1 > y2) {
			double tmp = y2;
			y2 = y1;
			y1 = tmp;
		}

		if (z1 > z2) {
			double tmp = z2;
			z2 = z1;
			z1 = tmp;
		}

		return AxisAlignedBB.getBoundingBox(x1, y1, z1, x2, y2, z2);
	}

}
