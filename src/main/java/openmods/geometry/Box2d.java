package openmods.geometry;

public class Box2d {

	public static final Box2d NULL = new Box2d(0, 0, 0, 0, 0, 0);

	public final int top;
	public final int bottom;

	public final int left;
	public final int right;

	public final int width;
	public final int height;

	private Box2d(int top, int bottom, int left, int right, int width, int height) {
		this.top = top;
		this.bottom = bottom;
		this.left = left;
		this.right = right;
		this.width = width;
		this.height = height;
	}

	public static Box2d fromCoords(int top, int bottom, int left, int right) {
		if (bottom < top) {
			final int tmp = bottom;
			bottom = top;
			top = tmp;
		}

		if (left > right) {
			final int tmp = left;
			left = right;
			right = tmp;
		}

		return new Box2d(top, bottom, left, right, right - left, bottom - top);
	}

	public static Box2d fromOriginAndSize(int x, int y, int width, int height) {
		final int left;
		final int right;
		if (width >= 0) {
			left = x;
			right = x + width;
		} else {
			left = x + width;
			right = x;
			width = -width;
		}

		final int top;
		final int bottom;
		if (height >= 0) {
			bottom = y + height;
			top = y;
		} else {
			bottom = y;
			top = y + height;
			height = -height;
		}

		return new Box2d(top, bottom, left, right, width, height);
	}

	public boolean isInside(int x, int y) {
		final float dx = x - left;
		final float dy = y - top;

		return 0 <= dx && 0 <= dy && dx < width && dy < height;
	}

	@Override
	public String toString() {
		return "[" + top + "," + left + "->" + right + "," + bottom + "]";
	}
}
