package openmods.shapes;

public class ShapePlanesGenerator extends DefaultShapeGenerator {

	@Override
	public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IShapeable shapeable) {
		for (int y = minY; y <= maxY; y++) {
			for (int z = minZ; z <= maxZ; z++)
				shapeable.setBlock(0, y, z);

			for (int x = minX; x <= maxX; x++)
				shapeable.setBlock(x, y, 0);
		}

		for (int x = minX; x <= maxX; x++)
			for (int z = minZ; z <= maxZ; z++)
				shapeable.setBlock(x, 0, z);
	}

}
