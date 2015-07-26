package openmods.geometry;

public class Box2d {

	public static final Box2d NULL = new Box2d(0, 0, 0, 0, 0, 0);

	public final float top;
	public final float bottom;

	public final float left;
	public final float right;

	public final float width;
	public final float height;

	private Box2d(float top, float bottom, float left, float right, float width, float height) {
		this.top = top;
		this.bottom = bottom;
		this.left = left;
		this.right = right;
		this.width = width;
		this.height = height;
	}

	public static Box2d fromCoords(float top, float bottom, float left, float right) {
		if (bottom < top) {
			final float tmp = bottom;
			bottom = top;
			top = tmp;
		}

		if (left > right) {
			final float tmp = left;
			left = right;
			right = tmp;
		}

		return new Box2d(top, bottom, left, right, right - left, bottom - top);
	}

	public static Box2d fromOriginAndSize(float x, float y, float width, float height) {
		final float left;
		final float right;
		if (width >= 0) {
			left = x;
			right = x + width;
		} else {
			left = x + width;
			right = x;
			width = -width;
		}

		final float top;
		final float bottom;
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

	public boolean isInside(float x, float y) {
		final float dx = x - left;
		final float dy = y - top;

		return 0 <= dx && 0 <= dy && dx < width && dy < height;
	}

	@Override
	public String toString() {
		return "[" + top + "," + left + "->" + bottom + "," + right + "]";
	}
}
