package openmods.colors;

import com.google.common.base.Preconditions;

public class ColorUtils {
	public static int bitmaskToVanilla(int color) {
		int high = Integer.numberOfLeadingZeros(color);
		int low = Integer.numberOfTrailingZeros(color);
		Preconditions.checkArgument(high == 31 - low && low <= 16, "Invalid color value: %sb", Integer.toBinaryString(color));
		return low;
	}

	public static ColorMeta findNearestColor(RGB target, int tolernace) {
		ColorMeta result = null;
		int distSq = Integer.MAX_VALUE;

		for (ColorMeta meta : ColorMeta.VALUES) {
			final int currentDistSq = meta.rgbWrap.distance(target);
			if (currentDistSq < distSq) {
				result = meta;
				distSq = currentDistSq;
			}
		}

		return (distSq < 3 * tolernace * tolernace)? result : null;
	}

}
