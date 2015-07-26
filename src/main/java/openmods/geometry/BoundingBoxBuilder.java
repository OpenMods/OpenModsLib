package openmods.geometry;

public class BoundingBoxBuilder {

	private float top;

	private float bottom;

	private float left;

	private float right;

	private BoundingBoxBuilder(float left, float right, float top, float bottom) {
		this.top = top;
		this.bottom = bottom;
		this.left = left;
		this.right = right;
	}

	public static BoundingBoxBuilder create() {
		return new BoundingBoxBuilder(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
	}

	public static BoundingBoxBuilder create(int x, int y) {
		return new BoundingBoxBuilder(x, x, y, y);
	}

	public BoundingBoxBuilder addPoint(float x, float y) {
		if (x < left) left = x;
		if (x > right) right = x;
		if (y < top) top = y;
		if (y > bottom) bottom = y;
		return this;
	}

	public Box2d build() {
		return Box2d.fromCoords(top, bottom, left, right);
	}
}
