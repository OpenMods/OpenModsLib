package openmods.renderer;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.common.ForgeDirection;

public class RotatedTessellator extends Tessellator {

	protected final Tessellator wrapped;

	public RotatedTessellator(Tessellator wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void setTextureUV(double par1, double par3) {
		wrapped.setTextureUV(par1, par3);
	}

	@Override
	public void setColorRGBA(int par1, int par2, int par3, int par4) {
		wrapped.setColorRGBA(par1, par2, par3, par4);
	}

	public static class R90 extends RotatedTessellator {
		public R90(Tessellator wrapped) {
			super(wrapped);
		}

		@Override
		public void addVertex(double x, double y, double z) {
			wrapped.addVertex(1 - z, y, x);
		}
	}

	public static class R180 extends RotatedTessellator {
		public R180(Tessellator wrapped) {
			super(wrapped);
		}

		@Override
		public void addVertex(double x, double y, double z) {
			wrapped.addVertex(1 - x, y, 1 - z);
		}
	}

	public static class R270 extends RotatedTessellator {
		public R270(Tessellator wrapped) {
			super(wrapped);
		}

		@Override
		public void addVertex(double x, double y, double z) {
			wrapped.addVertex(z, y, 1 - x);
		}
	}

	public static Tessellator wrap(Tessellator tes, ForgeDirection dir) {
		switch (dir) {
			case NORTH:
				return new R180(tes);
			case SOUTH:
				return tes;
			case EAST:
				return new R270(tes);
			case WEST:
				return new R90(tes);
			default:
				return tes;
		}
	}
}
