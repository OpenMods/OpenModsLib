package openmods.geometry;

import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.util.Direction;
import net.minecraft.util.math.Vec3d;

public class BlockTextureTransform {

	public static class TexCoords {
		public final double u;
		public final double v;
		public final double h;

		public TexCoords(double u, double v, double h) {
			this.u = u;
			this.v = v;
			this.h = h;
		}
	}

	private static final Matrix2d MIRROR_U = Matrix2d.createMirrorX();
	private static final Matrix2d MIRROR_V = Matrix2d.createMirrorY();
	private static final Matrix2d MIRROR_UV = Matrix2d.createMirrorXY();
	private static final Matrix2d ROTATE_CW = Matrix2d.createRotateCW();
	private static final Matrix2d ROTATE_CCW = Matrix2d.createRotateCCW();
	private static final Matrix2d SWAP = Matrix2d.createSwap();

	public static class Builder {
		private final Map<Direction, Matrix2d> transforms = Maps.newEnumMap(Direction.class);

		private Builder() {
			for (Direction dir : Direction.values())
				transforms.put(dir, Matrix2d.createIdentity());
		}

		private Builder(Map<Direction, Matrix2d> transforms) {
			for (Map.Entry<Direction, Matrix2d> e : transforms.entrySet())
				this.transforms.put(e.getKey(), e.getValue().copy());
		}

		public BlockTextureTransform build() {
			final EnumMap<Direction, Matrix2d> inverseTransforms = Maps.newEnumMap(transforms);
			for (Map.Entry<Direction, Matrix2d> e : inverseTransforms.entrySet())
				e.setValue(e.getValue().invertCopy());

			return new BlockTextureTransform(Maps.newEnumMap(transforms), inverseTransforms);
		}

		public Builder mirrorU(Direction side) {
			transforms.get(side).mulRight(MIRROR_U);
			return this;
		}

		public Builder mirrorV(Direction side) {
			transforms.get(side).mulRight(MIRROR_V);
			return this;
		}

		public Builder mirrorUV(Direction side) {
			transforms.get(side).mulRight(MIRROR_UV);
			return this;
		}

		public Builder rotateCW(Direction side) {
			transforms.get(side).mulRight(ROTATE_CW);
			return this;
		}

		public Builder rotateCCW(Direction side) {
			transforms.get(side).mulRight(ROTATE_CCW);
			return this;
		}

		public Builder swapUV(Direction side) {
			transforms.get(side).mulRight(SWAP);
			return this;
		}
	}

	private final Map<Direction, Matrix2d> transforms;

	private final Map<Direction, Matrix2d> inverseTransforms;

	private BlockTextureTransform(Map<Direction, Matrix2d> transforms, Map<Direction, Matrix2d> inverseTransforms) {
		this.transforms = transforms;
		this.inverseTransforms = inverseTransforms;
	}

	public TexCoords worldVecToTextureCoords(Direction side, double x, double y, double z) {
		final double wallX;
		final double wallY;
		final double h;
		// positive h always points "outside" block
		switch (side) {
			case UP:
				wallX = x;
				wallY = z;
				h = y - 1;
				break;
			case DOWN:
				wallX = x;
				wallY = z;
				h = -y;
				break;
			case EAST:
				wallX = z;
				wallY = 1 - y;
				h = x - 1;
				break;
			case WEST:
				wallX = z;
				wallY = 1 - y;
				h = -x;
				break;
			case NORTH:
				wallX = x;
				wallY = 1 - y;
				h = -z;
				break;
			case SOUTH:
				wallX = x;
				wallY = 1 - y;
				h = z - 1;
				break;
			default:
				throw new IllegalArgumentException(side.toString());
		}

		final Matrix2d transformation = transforms.get(side);
		final double u = transformation.transformX(wallX - 0.5, wallY - 0.5) + 0.5;
		final double v = transformation.transformY(wallX - 0.5, wallY - 0.5) + 0.5;
		return new TexCoords(u, v, h);
	}

	public Vec3d textureCoordsToWorldVec(Direction side, double u, double v, double h) {
		final Matrix2d transformation = inverseTransforms.get(side);
		if (transformation == null) throw new IllegalArgumentException(side.toString());

		final double wallX = transformation.transformX(u - 0.5, v - 0.5) + 0.5;
		final double wallY = transformation.transformY(u - 0.5, v - 0.5) + 0.5;

		final double globalX;
		final double globalY;
		final double globalZ;

		switch (side) {
			case UP:
				globalX = wallX;
				globalY = h + 1;
				globalZ = wallY;
				break;
			case DOWN:
				globalX = wallX;
				globalY = -h;
				globalZ = wallY;
				break;
			case EAST:
				globalX = h + 1;
				globalY = 1 - wallY;
				globalZ = wallX;
				break;
			case WEST:
				globalX = -h;
				globalY = 1 - wallY;
				globalZ = wallX;
				break;
			case NORTH:
				globalX = wallX;
				globalY = 1 - wallY;
				globalZ = -h;
				break;
			case SOUTH:
				globalX = wallX;
				globalY = 1 - wallY;
				globalZ = h + 1;
				break;
			default:
				throw new IllegalArgumentException(side.toString());
		}

		return new Vec3d(globalX, globalY, globalZ);
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder builderFromThis() {
		return new Builder(transforms);
	}
}
