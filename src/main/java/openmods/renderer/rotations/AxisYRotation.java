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

	private final SideRotationConfigurator configurator = new SideRotationConfigurator();

	private AxisYRotation() {}

	@Override
	@SideOnly(Side.CLIENT)
	public RenderBlocks enter(Orientation orientation, int metadata, RenderBlocks renderer) {
		configurator.setupFaces(renderer, orientation);

		// no tweaked renderer needed, since we are only using correctly implemented orientations (*_YP)
		return renderer;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void exit(RenderBlocks renderer) {
		SideRotationConfigurator.resetFaces(renderer);
	}

}
