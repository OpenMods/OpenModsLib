package openmods.renderer.rotations;

import net.minecraft.client.renderer.RenderBlocks;
import openmods.geometry.Orientation;
import openmods.reflection.ClonerFactory;
import openmods.reflection.ClonerFactory.ICloner;
import openmods.renderer.FixedRenderBlocks;
import openmods.renderer.TweakedRenderBlocks;

public abstract class ClonerSetup implements IRendererSetup {

	static final IRendererSetup fixedSetup = new ClonerSetup() {
		@Override
		protected RenderBlocks createRenderer() {
			return new FixedRenderBlocks();
		}
	};

	static final IRendererSetup tweakedSetup = new ClonerSetup() {
		@Override
		protected RenderBlocks createRenderer() {
			return new TweakedRenderBlocks();
		}
	};

	private final SideRotationConfigurator configurator = new SideRotationConfigurator();

	private static final ICloner<RenderBlocks> CLONER = ClonerFactory.instance.getCloner(RenderBlocks.class);

	ClonerSetup() {}

	protected abstract RenderBlocks createRenderer();

	@Override
	public RenderBlocks enter(Orientation orientation, int metadata, RenderBlocks renderer) {
		final RenderBlocks tweakedRenderer = createRenderer();
		CLONER.clone(renderer, tweakedRenderer);
		configurator.setupFaces(tweakedRenderer, orientation);
		return tweakedRenderer;
	}

	@Override
	public void exit(RenderBlocks renderer) {
		// NO-OP, since we were operating on copy
	}

}
