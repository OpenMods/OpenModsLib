package openmods.shapes;

import openmods.utils.render.GeometryUtils;
import openmods.utils.render.GeometryUtils.Axis;

public class ShapeCuboidGenerator extends DefaultShapeGenerator {

	public enum Elements {
		CORNERS(true, false, false),
		EDGES(true, true, false),
		WALLS(true, true, true);

		private final boolean corners;

		private final boolean edges;

		private final boolean walls;

		Elements(boolean corners, boolean edges, boolean walls) {
			this.corners = corners;
			this.edges = edges;
			this.walls = walls;
		}
	}

	private final boolean corners;

	private final boolean edges;

	private final boolean walls;

	public ShapeCuboidGenerator(boolean corners, boolean edges, boolean walls) {
		this.corners = corners;
		this.edges = edges;
		this.walls = walls;
	}

	public ShapeCuboidGenerator(Elements elements) {
		this(elements.corners, elements.edges, elements.walls);
	}

	public ShapeCuboidGenerator() {
		this(Elements.WALLS);
	}

	@Override
	public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IShapeable shapeable) {
		final int dx = maxX - minX - 2;
		final int dy = maxY - minY - 2;
		final int dz = maxZ - minZ - 2;

		if (corners) {
			shapeable.setBlock(maxX, maxY, maxZ);
			shapeable.setBlock(maxX, maxY, minZ);
			shapeable.setBlock(maxX, minY, maxZ);
			shapeable.setBlock(maxX, minY, minZ);
			shapeable.setBlock(minX, maxY, maxZ);
			shapeable.setBlock(minX, maxY, minZ);
			shapeable.setBlock(minX, minY, maxZ);
			shapeable.setBlock(minX, minY, minZ);
		}

		if (edges) {
			GeometryUtils.makeLine(minX, minY + 1, minZ, Axis.Y, dy, shapeable);
			GeometryUtils.makeLine(minX, minY + 1, maxZ, Axis.Y, dy, shapeable);
			GeometryUtils.makeLine(maxX, minY + 1, maxZ, Axis.Y, dy, shapeable);
			GeometryUtils.makeLine(maxX, minY + 1, minZ, Axis.Y, dy, shapeable);

			GeometryUtils.makeLine(minX + 1, minY, minZ, Axis.X, dx, shapeable);
			GeometryUtils.makeLine(minX + 1, minY, maxZ, Axis.X, dx, shapeable);
			GeometryUtils.makeLine(minX + 1, maxY, maxZ, Axis.X, dx, shapeable);
			GeometryUtils.makeLine(minX + 1, maxY, minZ, Axis.X, dx, shapeable);

			GeometryUtils.makeLine(minX, minY, minZ + 1, Axis.Z, dz, shapeable);
			GeometryUtils.makeLine(minX, maxY, minZ + 1, Axis.Z, dz, shapeable);
			GeometryUtils.makeLine(maxX, maxY, minZ + 1, Axis.Z, dz, shapeable);
			GeometryUtils.makeLine(maxX, minY, minZ + 1, Axis.Z, dz, shapeable);
		}

		if (walls) {
			GeometryUtils.makePlane(minX + 1, minY + 1, minZ, dx, dy, Axis.X, Axis.Y, shapeable);
			GeometryUtils.makePlane(minX + 1, minY + 1, maxZ, dx, dy, Axis.X, Axis.Y, shapeable);

			GeometryUtils.makePlane(minX + 1, minY, minZ + 1, dx, dz, Axis.X, Axis.Z, shapeable);
			GeometryUtils.makePlane(minX + 1, maxY, minZ + 1, dx, dz, Axis.X, Axis.Z, shapeable);

			GeometryUtils.makePlane(minX, minY + 1, minZ + 1, dy, dz, Axis.Y, Axis.Z, shapeable);
			GeometryUtils.makePlane(maxX, minY + 1, minZ + 1, dy, dz, Axis.Y, Axis.Z, shapeable);
		}
	}
}
