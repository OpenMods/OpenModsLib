package openmods.geometry;

import net.minecraftforge.common.util.ForgeDirection;

public class BlockTextureTransform {

	public static class TexCoords {
		public final double u;
		public final double v;
		public final double z;

		public TexCoords(double u, double v, double z) {
			this.u = u;
			this.v = v;
			this.z = z;
		}
	}

	public static class WorldCoords {
		public final double x;
		public final double y;
		public final double z;

		public WorldCoords(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private final ForgeDirection side;

	public BlockTextureTransform(ForgeDirection side) {
		this.side = side;
	}

	public TexCoords worldVecToTextureCoords(double x, double y, double z) {
		switch (side) {
			case UP:
				return new TexCoords(x, z, 1 - y);
			case DOWN:
				return new TexCoords(x, z, y);
			case NORTH:
				return new TexCoords(1 - x, 1 - y, z);
			case SOUTH:
				return new TexCoords(x, 1 - y, 1 - z);
			case WEST:
				return new TexCoords(z, 1 - y, x);
			case EAST:
				return new TexCoords(1 - z, 1 - y, 1 - x);
			default:
				throw new IllegalArgumentException(side.toString());
		}
	}

	public WorldCoords textureCoordsToWorldVec(double u, double v, double z) {
		switch (side) {
			case UP:
				return new WorldCoords(u, 1 - z, v);
			case DOWN:
				return new WorldCoords(u, z, v);
			case NORTH:
				return new WorldCoords(1 - u, 1 - v, z);
			case SOUTH:
				return new WorldCoords(u, 1 - v, 1 - z);
			case WEST:
				return new WorldCoords(z, 1 - v, u);
			case EAST:
				return new WorldCoords(1 - z, 1 - v, 1 - u);
			default:
				throw new IllegalArgumentException(side.toString());
		}
	}
}
