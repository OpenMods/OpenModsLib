package openmods.renderer.rotations;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraftforge.common.util.ForgeDirection;
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

	private static final ICloner<RenderBlocks> CLONER = ClonerFactory.instance.getCloner(RenderBlocks.class);

	private TopRotation() {}

	@Override
	@SideOnly(Side.CLIENT)
	public RenderBlocks enter(ForgeDirection top, RenderBlocks renderer) {
		final RenderBlocks tweakedRenderer = new TweakedRenderBlocks();
		CLONER.clone(renderer, tweakedRenderer);

		switch (top) {
			case DOWN:
				tweakedRenderer.uvRotateNorth = 3;
				tweakedRenderer.uvRotateSouth = 3;

				tweakedRenderer.uvRotateEast = 3;
				tweakedRenderer.uvRotateWest = 3;
				break;
			case EAST:
				tweakedRenderer.uvRotateTop = 1;
				tweakedRenderer.uvRotateBottom = 2;

				tweakedRenderer.uvRotateEast = 2;
				tweakedRenderer.uvRotateWest = 1;

				tweakedRenderer.uvRotateSouth = 1;
				tweakedRenderer.uvRotateNorth = 2;
				break;
			case NORTH:
				tweakedRenderer.uvRotateNorth = 2;
				tweakedRenderer.uvRotateSouth = 1;

				tweakedRenderer.uvRotateEast = 3;
				tweakedRenderer.uvRotateWest = 3;
				break;
			case SOUTH:
				tweakedRenderer.uvRotateTop = 3;
				tweakedRenderer.uvRotateBottom = 3;

				tweakedRenderer.uvRotateNorth = 1;
				tweakedRenderer.uvRotateSouth = 2;
				break;
			case UP:
				break;
			case WEST:
				tweakedRenderer.uvRotateTop = 2;
				tweakedRenderer.uvRotateBottom = 1;

				tweakedRenderer.uvRotateEast = 1;
				tweakedRenderer.uvRotateWest = 2;

				tweakedRenderer.uvRotateNorth = 2;
				tweakedRenderer.uvRotateSouth = 1;
				break;
			default:
				break;
		}

		return tweakedRenderer;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void exit(RenderBlocks renderer) {
		// NO-OP, since we were operating on copy
	}

}
