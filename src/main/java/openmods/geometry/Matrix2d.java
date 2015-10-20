package openmods.geometry;

public class Matrix2d {

	public double m00;
	public double m01;
	public double m10;
	public double m11;

	public Matrix2d() {}

	public Matrix2d(Matrix2d src) {
		copy(src);
	}

	public Matrix2d(double m00, double m10, double m01, double m11) {
		this.m00 = m00;
		this.m01 = m01;
		this.m10 = m10;
		this.m11 = m11;
	}

	public Matrix2d copy() {
		return new Matrix2d(this);
	}

	public Matrix2d copy(Matrix2d src) {
		return copy(src, this);
	}

	public static Matrix2d copy(Matrix2d src, Matrix2d dest) {
		if (dest == null) dest = new Matrix2d();

		dest.m00 = src.m00;
		dest.m01 = src.m01;
		dest.m10 = src.m10;
		dest.m11 = src.m11;

		return dest;
	}

	public static Matrix2d createIdentity() {
		return new Matrix2d(+1, 0, 0, +1);
	}

	public static Matrix2d createSwap() {
		return new Matrix2d(0, +1, +1, 0);
	}

	public static Matrix2d createRotateCW() {
		return new Matrix2d(0, +1, -1, 0);
	}

	public static Matrix2d createRotateCCW() {
		return new Matrix2d(0, -1, +1, 0);
	}

	public static Matrix2d createMirrorX() {
		return new Matrix2d(-1, 0, 0, +1);
	}

	public static Matrix2d createMirrorY() {
		return new Matrix2d(+1, 0, 0, -1);
	}

	public static Matrix2d createMirrorXY() {
		return new Matrix2d(-1, 0, 0, -1);
	}

	public static Matrix2d add(Matrix2d left, Matrix2d right) {
		return add(left, right, new Matrix2d());
	}

	public Matrix2d add(Matrix2d v) {
		return add(v, this, this);
	}

	public static Matrix2d add(Matrix2d left, Matrix2d right, Matrix2d dest) {
		dest.m00 = left.m00 + right.m00;
		dest.m01 = left.m01 + right.m01;
		dest.m10 = left.m10 + right.m10;
		dest.m11 = left.m11 + right.m11;

		return dest;
	}

	public static Matrix2d sub(Matrix2d left, Matrix2d right) {
		return sub(left, right, new Matrix2d());
	}

	public Matrix2d sub(Matrix2d v) {
		return sub(v, this, this);
	}

	public static Matrix2d sub(Matrix2d left, Matrix2d right, Matrix2d dest) {

		dest.m00 = left.m00 - right.m00;
		dest.m01 = left.m01 - right.m01;
		dest.m10 = left.m10 - right.m10;
		dest.m11 = left.m11 - right.m11;

		return dest;
	}

	public static Matrix2d mul(Matrix2d left, Matrix2d right) {
		return mul(left, right, new Matrix2d());
	}

	public Matrix2d mulLeft(Matrix2d v) {
		return mul(v, this, this);
	}

	public Matrix2d mulRight(Matrix2d v) {
		return mul(this, v, this);
	}

	public static Matrix2d mul(Matrix2d left, Matrix2d right, Matrix2d dest) {
		final double m00 = left.m00 * right.m00 + left.m10 * right.m01;
		final double m01 = left.m01 * right.m00 + left.m11 * right.m01;
		final double m10 = left.m00 * right.m10 + left.m10 * right.m11;
		final double m11 = left.m01 * right.m10 + left.m11 * right.m11;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m10 = m10;
		dest.m11 = m11;

		return dest;
	}

	public double transformX(double x, double y) {
		return m00 * x + m10 * y;
	}

	public double transformY(double x, double y) {
		return m01 * x + m11 * y;
	}

	public Matrix2d transposeCopy() {
		return transpose(new Matrix2d());
	}

	public Matrix2d transposeInplace() {
		return transpose(this);
	}

	public Matrix2d transpose(Matrix2d dest) {
		return transpose(this, dest);
	}

	public static Matrix2d transpose(Matrix2d src, Matrix2d dest) {
		final double m01 = src.m10;
		final double m10 = src.m01;

		dest.m01 = m01;
		dest.m10 = m10;

		return dest;
	}

	public Matrix2d invertCopy() {
		return invert(this, new Matrix2d());
	}

	public Matrix2d invertInplace() {
		return invert(this, this);
	}

	public static Matrix2d invert(Matrix2d src) {
		return invert(src, new Matrix2d());
	}

	public static Matrix2d invert(Matrix2d src, Matrix2d dest) {
		double determinant = src.determinant();
		if (determinant == 0) throw new ArithmeticException("Can't invert matrix " + src);

		final double determinant_inv = 1f / determinant;

		final double t00 = src.m11 * determinant_inv;
		final double t01 = -src.m01 * determinant_inv;
		final double t11 = src.m00 * determinant_inv;
		final double t10 = -src.m10 * determinant_inv;

		dest.m00 = t00;
		dest.m01 = t01;
		dest.m10 = t10;
		dest.m11 = t11;
		return dest;
	}

	@Override
	public String toString() {
		return "[" + m00 + ' ' + m10 + ';' + m01 + ' ' + m11 + ']';
	}

	public Matrix2d negate() {
		return negate(this);
	}

	public Matrix2d negate(Matrix2d dest) {
		return negate(this, dest);
	}

	public static Matrix2d negate(Matrix2d src, Matrix2d dest) {
		dest.m00 = -src.m00;
		dest.m01 = -src.m01;
		dest.m10 = -src.m10;
		dest.m11 = -src.m11;

		return dest;
	}

	public Matrix2d setIdentity() {
		return setIdentity(this);
	}

	public static Matrix2d setIdentity(Matrix2d src) {
		src.m00 = 1.0;
		src.m01 = 0.0;
		src.m10 = 0.0;
		src.m11 = 1.0;
		return src;
	}

	public Matrix2d setZero() {
		return setZero(this);
	}

	public static Matrix2d setZero(Matrix2d src) {
		src.m00 = 0.0;
		src.m01 = 0.0;
		src.m10 = 0.0;
		src.m11 = 0.0;
		return src;
	}

	public double determinant() {
		return m00 * m11 - m01 * m10;
	}
}
