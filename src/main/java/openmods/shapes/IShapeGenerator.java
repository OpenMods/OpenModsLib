package openmods.shapes;

public interface IShapeGenerator {
	void generateShape(int xSize, int ySize, int zSize, IShapeable shapeable);

	void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IShapeable shapeable);
}
