package openmods.shapes;

import java.util.List;

import openmods.utils.CollectionUtils;
import openmods.utils.render.GeometryUtils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class ShapeEquilateral2dGenerator implements IShapeGenerator {

	private static class Trig {
		final double sin;
		final double cos;

		public Trig(double sin, double cos) {
			this.sin = sin;
			this.cos = cos;
		}
	}

	private static class Point {
		final int x;
		final int y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "[" + x + "," + y + "]";
		}

	}

	private enum Symmetry {
		TwoFold(Math.PI) {
			@Override
			public Point[] generate(Point[] initial) {
				final Point[] result = new Point[initial.length * 2];
				int i = 0;
				for (Point element : initial)
					result[i++] = element;

				for (int j = initial.length - 1; j >= 0; j--) {
					final Point point = initial[j];
					result[i++] = new Point(point.x, -point.y);
				}

				return result;
			}
		},
		FourFold(Math.PI / 2) {
			@Override
			public Point[] generate(Point[] initial) {
				final Point[] result = new Point[initial.length * 4];
				int i = 0;
				for (Point point : initial)
					result[i++] = point;

				for (int j = initial.length - 1; j >= 0; j--) {
					final Point point = initial[j];
					result[i++] = new Point(-point.x, point.y);
				}

				for (Point point : initial)
					result[i++] = new Point(-point.x, -point.y);

				for (int j = initial.length - 1; j >= 0; j--) {
					final Point point = initial[j];
					result[i++] = new Point(point.x, -point.y);
				}

				return result;
			}
		};

		public abstract Point[] generate(Point[] initial);

		public final double angleLimit;

		private Symmetry(double angleLimit) {
			this.angleLimit = angleLimit;
		}

	}

	private final Symmetry symmetry;

	private final Trig[] angles;

	public ShapeEquilateral2dGenerator(int sides) {

		this.symmetry = sides % 2 == 0? Symmetry.FourFold : Symmetry.TwoFold;

		final List<Trig> angles = Lists.newArrayList();
		for (int i = 0; i < sides; i++) {
			final double d = 2 * Math.PI * i / sides;
			if (d > this.symmetry.angleLimit) break;
			angles.add(new Trig(Math.sin(d), Math.cos(d)));
		}

		this.angles = angles.toArray(new Trig[angles.size()]);

	}

	@Override
	public void generateShape(int xSize, int ySize, int zSize, IShapeable shapeable) {
		generateShape(-xSize, -ySize, -zSize, +xSize, +ySize, +zSize, shapeable);
	}

	@Override
	public void generateShape(int minX, final int minY, int minZ, int maxX, final int maxY, int maxZ, final IShapeable shapeable) {
		final IShapeable columnShapeable = new IShapeable() {
			@Override
			public void setBlock(int x, int ingored, int z) {
				for (int y = minY; y <= maxY; y++)
					shapeable.setBlock(x, y, z);
			}
		};

		final double middleX = (maxX + minX) / 2.0;
		final double radiusX = (maxX - minX) / 2.0;

		final double middleZ = (maxZ + minZ) / 2.0;
		final double radiusZ = (maxZ - minZ) / 2.0;

		final Point[] points = CollectionUtils.transform(angles, new Function<Trig, Point>() {
			@Override
			public Point apply(Trig input) {
				int x = (int)Math.round(middleX + radiusX * input.cos);
				int z = (int)Math.round(middleZ + radiusZ * input.sin);
				return new Point(x, z);
			}
		});

		final Point[] duplicatedPoints = symmetry.generate(points);

		final Point initialPoint = duplicatedPoints[0];
		Point prevPoint = initialPoint;

		for (int i = 1; i < duplicatedPoints.length; i++) {
			final Point point = duplicatedPoints[i];
			GeometryUtils.line2D(0, prevPoint.x, prevPoint.y, point.x, point.y, columnShapeable);
			prevPoint = point;
		}

		GeometryUtils.line2D(0, prevPoint.x, prevPoint.y, initialPoint.x, initialPoint.y, columnShapeable);
	}
}
