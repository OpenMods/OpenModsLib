package openmods.renderer.rotations;

import net.minecraft.client.renderer.RenderBlocks;
import openmods.geometry.Orientation;
import openmods.reflection.ClonerFactory;
import openmods.reflection.ClonerFactory.ICloner;
import openmods.renderer.TweakedRenderBlocks;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This object configures renderer to render block sides as if block was oriented in such way, that top side points into given direction.
 * Warning: this object assumes that it operates on "tweaked" version of render blocks, where bottom side is vertical flip of top side.
 * Otherwise it looks like it's impossible to implement this transformation with just rotations (texture flips are needed).
 */
public class TopRotation implements IRendererSetup {

	public static final IRendererSetup instance = new TopRotation();

	private final SideRotationConfigurator configurator = new SideRotationConfigurator();

	private static final ICloner<RenderBlocks> CLONER = ClonerFactory.instance.getCloner(RenderBlocks.class);

	private TopRotation() {}

	@Override
	@SideOnly(Side.CLIENT)
	public RenderBlocks enter(Orientation orientation, int metadata, RenderBlocks renderer) {
		final RenderBlocks tweakedRenderer = new TweakedRenderBlocks();
		CLONER.clone(renderer, tweakedRenderer);
		configurator.setupFaces(tweakedRenderer, orientation);
		return tweakedRenderer;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void exit(RenderBlocks renderer) {
		// NO-OP, since we were operating on copy
	}

}
