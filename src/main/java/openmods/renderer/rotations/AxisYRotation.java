package openmods.renderer.rotations;

import net.minecraft.client.renderer.RenderBlocks;
import openmods.geometry.Orientation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This object configures block renderer to render block sides as if block was rotated around Y axis.
 * Note: Only top and bottom sides require actual rotation.
 */
public class AxisYRotation implements IRendererSetup {

	public static final IRendererSetup instance = new AxisYRotation();

	private AxisYRotation() {}

	@Override
	@SideOnly(Side.CLIENT)
	public RenderBlocks enter(Orientation orientation, int metadata, RenderBlocks renderer) {
		// TODO: verify
		switch (orientation) {
			case ZP_YP:
				renderer.uvRotateTop = 1;
				renderer.uvRotateBottom = 2;
				break;
			case ZN_YP:
				renderer.uvRotateTop = 2;
				renderer.uvRotateBottom = 1;
				break;
			case XN_YP:
				renderer.uvRotateTop = 3;
				renderer.uvRotateBottom = 3;
				break;
			case XP_YP:
			default:
				break;
		}

		// no tweaked renderer needed
		return renderer;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void exit(RenderBlocks renderer) {
		renderer.uvRotateTop = 0;
		renderer.uvRotateBottom = 0;
	}

}
