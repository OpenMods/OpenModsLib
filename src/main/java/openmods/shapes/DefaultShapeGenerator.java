package openmods.shapes;

public abstract class DefaultShapeGenerator implements IShapeGenerator {

	@Override
	public void generateShape(int xSize, int ySize, int zSize, IShapeable shapeable) {
		generateShape(-xSize, -ySize, -zSize, +xSize, +ySize, +zSize, shapeable);

	}
}
