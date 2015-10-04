package openmods.shapes;

import java.util.Set;

import openmods.utils.render.GeometryUtils;
import openmods.utils.render.GeometryUtils.Octant;

public class ShapeSphereGenerator implements IShapeGenerator {

	private final Set<Octant> octants;

	public ShapeSphereGenerator() {
		this(Octant.ALL);
	}

	public ShapeSphereGenerator(Set<Octant> octants) {
		this.octants = octants;
	}

	@Override
	public void generateShape(int radiusX, int radiusY, int radiusZ, IShapeable shapeable) {
		generateShape(-radiusX, -radiusY, -radiusZ, +radiusX, +radiusY, +radiusZ, shapeable);
	}

	@Override
	public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IShapeable shapeable) {
		GeometryUtils.makeEllipsoid(minX, minY, minZ, maxX, maxY, maxZ, shapeable, octants);
	}
}
