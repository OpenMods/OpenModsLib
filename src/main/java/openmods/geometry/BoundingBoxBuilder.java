package openmods.geometry;

public class BoundingBoxBuilder {

	private int top;

	private int bottom;

	private int left;

	private int right;

	private BoundingBoxBuilder(int left, int right, int top, int bottom) {
		this.top = top;
		this.bottom = bottom;
		this.left = left;
		this.right = right;
	}

	public static BoundingBoxBuilder create() {
		return new BoundingBoxBuilder(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
	}

	public static BoundingBoxBuilder create(int x, int y) {
		return new BoundingBoxBuilder(x, x, y, y);
	}

	public BoundingBoxBuilder addPoint(int x, int y) {
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
