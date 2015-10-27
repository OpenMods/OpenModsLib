package openmods.renderer.rotations;

import net.minecraft.client.renderer.RenderBlocks;
import openmods.geometry.Orientation;

public class SideRotationConfigurator {

	public static final int ROTATE_CW = 1;
	public static final int ROTATE_CCW = 2;
	public static final int INVERT = 3;

	public void setupFaces(RenderBlocks renderer, Orientation orientation) {
		switch (orientation) {
		// TOP = X+
			case ZP_XP:
				renderer.uvRotateTop = ROTATE_CW;
				renderer.uvRotateBottom = ROTATE_CCW;
				renderer.uvRotateNorth = INVERT;
				renderer.uvRotateSouth = INVERT;
				renderer.uvRotateWest = ROTATE_CW;
				renderer.uvRotateEast = ROTATE_CCW;
				break;
			case ZN_XP:
				renderer.uvRotateTop = ROTATE_CW;
				renderer.uvRotateBottom = ROTATE_CCW;
				renderer.uvRotateWest = ROTATE_CW;
				renderer.uvRotateEast = ROTATE_CCW;
				break;
			case YP_XP:
				renderer.uvRotateTop = ROTATE_CW;
				renderer.uvRotateBottom = ROTATE_CCW;
				renderer.uvRotateNorth = ROTATE_CW;
				renderer.uvRotateSouth = ROTATE_CCW;
				renderer.uvRotateWest = ROTATE_CW;
				renderer.uvRotateEast = ROTATE_CCW;
				break;
			case YN_XP:
				renderer.uvRotateTop = ROTATE_CW;
				renderer.uvRotateBottom = ROTATE_CCW;
				renderer.uvRotateNorth = ROTATE_CCW;
				renderer.uvRotateSouth = ROTATE_CW;
				renderer.uvRotateWest = ROTATE_CW;
				renderer.uvRotateEast = ROTATE_CCW;
				break;

			// TOP = X-
			case ZP_XN:
				renderer.uvRotateTop = ROTATE_CCW;
				renderer.uvRotateBottom = ROTATE_CW;
				renderer.uvRotateWest = ROTATE_CCW;
				renderer.uvRotateEast = ROTATE_CW;
				break;
			case ZN_XN:
				renderer.uvRotateTop = ROTATE_CCW;
				renderer.uvRotateBottom = ROTATE_CW;
				renderer.uvRotateNorth = INVERT;
				renderer.uvRotateSouth = INVERT;
				renderer.uvRotateWest = ROTATE_CCW;
				renderer.uvRotateEast = ROTATE_CW;
				break;
			case YN_XN:
				renderer.uvRotateTop = ROTATE_CCW;
				renderer.uvRotateBottom = ROTATE_CW;
				renderer.uvRotateNorth = ROTATE_CW;
				renderer.uvRotateSouth = ROTATE_CCW;
				renderer.uvRotateWest = ROTATE_CCW;
				renderer.uvRotateEast = ROTATE_CW;
				break;
			case YP_XN:
				renderer.uvRotateTop = ROTATE_CCW;
				renderer.uvRotateBottom = ROTATE_CW;
				renderer.uvRotateNorth = ROTATE_CCW;
				renderer.uvRotateSouth = ROTATE_CW;
				renderer.uvRotateWest = ROTATE_CCW;
				renderer.uvRotateEast = ROTATE_CW;
				break;

			// TOP = Y+
			case ZP_YP:
				renderer.uvRotateTop = ROTATE_CW;
				renderer.uvRotateBottom = ROTATE_CCW;
				break;
			case ZN_YP:
				renderer.uvRotateTop = ROTATE_CCW;
				renderer.uvRotateBottom = ROTATE_CW;
				break;
			case XN_YP:
				renderer.uvRotateTop = INVERT;
				renderer.uvRotateBottom = INVERT;
				break;
			case XP_YP:
				break;

			// TOP = Y-
			case ZP_YN:
				renderer.uvRotateTop = ROTATE_CCW;
				renderer.uvRotateBottom = ROTATE_CW;
				renderer.uvRotateNorth = INVERT;
				renderer.uvRotateSouth = INVERT;
				renderer.uvRotateWest = INVERT;
				renderer.uvRotateEast = INVERT;
				break;
			case ZN_YN:
				renderer.uvRotateTop = ROTATE_CW;
				renderer.uvRotateBottom = ROTATE_CCW;
				renderer.uvRotateNorth = INVERT;
				renderer.uvRotateSouth = INVERT;
				renderer.uvRotateWest = INVERT;
				renderer.uvRotateEast = INVERT;
				break;
			case XP_YN:
				renderer.uvRotateTop = INVERT;
				renderer.uvRotateBottom = INVERT;
				renderer.uvRotateNorth = INVERT;
				renderer.uvRotateSouth = INVERT;
				renderer.uvRotateWest = INVERT;
				renderer.uvRotateEast = INVERT;
				break;
			case XN_YN:
				renderer.uvRotateNorth = INVERT;
				renderer.uvRotateSouth = INVERT;
				renderer.uvRotateWest = INVERT;
				renderer.uvRotateEast = INVERT;
				break;

			// TOP = Z+
			case YN_ZP:
				renderer.uvRotateTop = INVERT;
				renderer.uvRotateBottom = INVERT;
				renderer.uvRotateNorth = ROTATE_CW;
				renderer.uvRotateSouth = ROTATE_CCW;
				renderer.uvRotateWest = ROTATE_CW;
				renderer.uvRotateEast = ROTATE_CCW;
				break;
			case YP_ZP:
				renderer.uvRotateTop = INVERT;
				renderer.uvRotateBottom = INVERT;
				renderer.uvRotateNorth = ROTATE_CW;
				renderer.uvRotateSouth = ROTATE_CCW;
				renderer.uvRotateWest = ROTATE_CCW;
				renderer.uvRotateEast = ROTATE_CW;
				break;
			case XN_ZP:
				renderer.uvRotateTop = INVERT;
				renderer.uvRotateBottom = INVERT;
				renderer.uvRotateNorth = ROTATE_CW;
				renderer.uvRotateSouth = ROTATE_CCW;
				renderer.uvRotateWest = INVERT;
				renderer.uvRotateEast = INVERT;
				break;
			case XP_ZP:
				renderer.uvRotateTop = INVERT;
				renderer.uvRotateBottom = INVERT;
				renderer.uvRotateNorth = ROTATE_CW;
				renderer.uvRotateSouth = ROTATE_CCW;
				break;

			// TOP = Z-
			case YP_ZN:
				renderer.uvRotateNorth = ROTATE_CCW;
				renderer.uvRotateSouth = ROTATE_CW;
				renderer.uvRotateWest = ROTATE_CW;
				renderer.uvRotateEast = ROTATE_CCW;
				break;
			case YN_ZN:
				renderer.uvRotateNorth = ROTATE_CCW;
				renderer.uvRotateSouth = ROTATE_CW;
				renderer.uvRotateWest = ROTATE_CCW;
				renderer.uvRotateEast = ROTATE_CW;
				break;
			case XN_ZN:
				renderer.uvRotateNorth = ROTATE_CCW;
				renderer.uvRotateSouth = ROTATE_CW;
				break;
			case XP_ZN:
				renderer.uvRotateNorth = ROTATE_CCW;
				renderer.uvRotateSouth = ROTATE_CW;
				renderer.uvRotateWest = INVERT;
				renderer.uvRotateEast = INVERT;
				break;
		}
	}

	public static void resetFaces(RenderBlocks renderer) {
		renderer.uvRotateBottom = 0;
		renderer.uvRotateEast = 0;
		renderer.uvRotateNorth = 0;
		renderer.uvRotateSouth = 0;
		renderer.uvRotateTop = 0;
		renderer.uvRotateWest = 0;
	}
}
