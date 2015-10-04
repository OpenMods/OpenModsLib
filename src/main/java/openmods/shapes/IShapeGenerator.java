package openmods.shapes;

public interface IShapeGenerator {
	public void generateShape(int xSize, int ySize, int zSize, IShapeable shapeable);

	public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IShapeable shapeable);
}
