package openmods.renderer.rotations;

import net.minecraft.client.renderer.RenderBlocks;
import openmods.geometry.Orientation;

public class VanillaSetup implements IRendererSetup {

	static final IRendererSetup instance = new VanillaSetup();

	private final SideRotationConfigurator configurator = new SideRotationConfigurator();

	private VanillaSetup() {}

	@Override
	public RenderBlocks enter(Orientation orientation, int metadata, RenderBlocks renderer) {
		configurator.setupFaces(renderer, orientation);
		// no tweaked renderer needed, since we are only using correctly implemented orientations (*_YP)
		return renderer;
	}

	@Override
	public void exit(RenderBlocks renderer) {
		SideRotationConfigurator.resetFaces(renderer);
	}

}
