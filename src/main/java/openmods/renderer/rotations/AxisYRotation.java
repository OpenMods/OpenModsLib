package openmods.renderer.rotations;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraftforge.common.util.ForgeDirection;
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
	public RenderBlocks enter(ForgeDirection north, RenderBlocks renderer) {
		switch (north) {
			case EAST:
				renderer.uvRotateTop = 1;
				renderer.uvRotateBottom = 2;
				break;
			case WEST:
				renderer.uvRotateTop = 2;
				renderer.uvRotateBottom = 1;
				break;
			case SOUTH:
				renderer.uvRotateTop = 3;
				renderer.uvRotateBottom = 3;
				break;
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
