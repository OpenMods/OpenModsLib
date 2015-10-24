package openmods.geometry;

import net.minecraft.util.Vec3;

import com.google.common.base.Objects;

public class Matrix3d {

	public double m00;
	public double m01;
	public double m02;
	public double m10;
	public double m11;
	public double m12;
	public double m20;
	public double m21;
	public double m22;

	public Matrix3d() {}

	public Matrix3d(Matrix3d m) {
		copy(m);
	}

	public Matrix3d copy(Matrix3d src) {
		return copy(src, this);
	}

	public Matrix3d(double m00, double m10, double m20, double m01, double m11, double m21, double m02, double m12, double m22) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
	}

	public Matrix3d copy() {
		return new Matrix3d(this);
	}

	public static Matrix3d copy(Matrix3d src, Matrix3d dest) {
		dest.m00 = src.m00;
		dest.m10 = src.m10;
		dest.m20 = src.m20;
		dest.m01 = src.m01;
		dest.m11 = src.m11;
		dest.m21 = src.m21;
		dest.m02 = src.m02;
		dest.m12 = src.m12;
		dest.m22 = src.m22;

		return dest;
	}

	public Matrix3d add(Matrix3d m) {
		return add(this, m, this);
	}

	public static Matrix3d add(Matrix3d left, Matrix3d right) {
		return add(left, right, new Matrix3d());
	}

	public static Matrix3d add(Matrix3d left, Matrix3d right, Matrix3d dest) {
		dest.m00 = left.m00 + right.m00;
		dest.m01 = left.m01 + right.m01;
		dest.m02 = left.m02 + right.m02;
		dest.m10 = left.m10 + right.m10;
		dest.m11 = left.m11 + right.m11;
		dest.m12 = left.m12 + right.m12;
		dest.m20 = left.m20 + right.m20;
		dest.m21 = left.m21 + right.m21;
		dest.m22 = left.m22 + right.m22;

		return dest;
	}

	public Matrix3d sub(Matrix3d m) {
		return sub(this, m, this);
	}

	public static Matrix3d sub(Matrix3d left, Matrix3d right) {
		return sub(left, right, new Matrix3d());
	}

	public static Matrix3d sub(Matrix3d left, Matrix3d right, Matrix3d dest) {
		if (dest == null) dest = new Matrix3d();

		dest.m00 = left.m00 - right.m00;
		dest.m01 = left.m01 - right.m01;
		dest.m02 = left.m02 - right.m02;
		dest.m10 = left.m10 - right.m10;
		dest.m11 = left.m11 - right.m11;
		dest.m12 = left.m12 - right.m12;
		dest.m20 = left.m20 - right.m20;
		dest.m21 = left.m21 - right.m21;
		dest.m22 = left.m22 - right.m22;

		return dest;
	}

	public Matrix3d mulLeft(Matrix3d v) {
		return mul(v, this, this);
	}

	public Matrix3d mulRight(Matrix3d v) {
		return mul(this, v, this);
	}

	public static Matrix3d mul(Matrix3d left, Matrix3d right) {
		return mul(left, right, new Matrix3d());
	}

	public static Matrix3d mul(Matrix3d left, Matrix3d right, Matrix3d dest) {
		final double m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
		final double m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
		final double m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
		final double m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
		final double m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
		final double m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
		final double m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
		final double m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
		final double m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;

		return dest;
	}

	public double transformX(double x, double y, double z) {
		return m00 * x + m10 * y + m20 * z;
	}

	public double transformY(double x, double y, double z) {
		return m01 * x + m11 * y + m21 * z;
	}

	public double transformZ(double x, double y, double z) {
		return m02 * x + m12 * y + m22 * z;
	}

	public Vec3 transform(Vec3 vec) {
		final double tx = transformX(vec.xCoord, vec.yCoord, vec.zCoord);
		final double ty = transformY(vec.xCoord, vec.yCoord, vec.zCoord);
		final double tz = transformZ(vec.xCoord, vec.yCoord, vec.zCoord);
		return Vec3.createVectorHelper(tx, ty, tz);
	}

	public Matrix3d transpose() {
		return transpose(this, this);
	}

	public Matrix3d transpose(Matrix3d dest) {
		return transpose(this, dest);
	}

	public static Matrix3d transpose(Matrix3d src, Matrix3d dest) {
		final double m00 = src.m00;
		final double m01 = src.m10;
		final double m02 = src.m20;
		final double m10 = src.m01;
		final double m11 = src.m11;
		final double m12 = src.m21;
		final double m20 = src.m02;
		final double m21 = src.m12;
		final double m22 = src.m22;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		return dest;
	}

	public double determinant() {
		return m00 * (m11 * m22 - m12 * m21)
				+ m01 * (m12 * m20 - m10 * m22)
				+ m02 * (m10 * m21 - m11 * m20);
	}

	@Override
	public String toString() {
		return "[" + m00 + ' ' + m10 + ' ' + m20 + ';' + m01 + ' ' + m11 + ' ' + m21 + ';' + m02 + ' ' + m12 + ' ' + m22 + ']';
	}

	public Matrix3d invertCopy() {
		return invert(this, new Matrix3d());
	}

	public Matrix3d invertInplace() {
		return invert(this, this);
	}

	public static Matrix3d invert(Matrix3d src, Matrix3d dest) {
		final double determinant = src.determinant();
		if (determinant == 0) throw new ArithmeticException("Can't invert matrix " + src);
		final double determinant_inv = 1f / determinant;

		final double t00 = src.m11 * src.m22 - src.m12 * src.m21;
		final double t01 = -src.m10 * src.m22 + src.m12 * src.m20;
		final double t02 = src.m10 * src.m21 - src.m11 * src.m20;
		final double t10 = -src.m01 * src.m22 + src.m02 * src.m21;
		final double t11 = src.m00 * src.m22 - src.m02 * src.m20;
		final double t12 = -src.m00 * src.m21 + src.m01 * src.m20;
		final double t20 = src.m01 * src.m12 - src.m02 * src.m11;
		final double t21 = -src.m00 * src.m12 + src.m02 * src.m10;
		final double t22 = src.m00 * src.m11 - src.m01 * src.m10;

		dest.m00 = t00 * determinant_inv;
		dest.m11 = t11 * determinant_inv;
		dest.m22 = t22 * determinant_inv;
		dest.m01 = t10 * determinant_inv;
		dest.m10 = t01 * determinant_inv;
		dest.m20 = t02 * determinant_inv;
		dest.m02 = t20 * determinant_inv;
		dest.m12 = t21 * determinant_inv;
		dest.m21 = t12 * determinant_inv;
		return dest;
	}

	public Matrix3d negateCopy() {
		return negate(new Matrix3d());
	}

	public Matrix3d negateInplace() {
		return negate(this);
	}

	public Matrix3d negate(Matrix3d dest) {
		return negate(this, dest);
	}

	public static Matrix3d negate(Matrix3d src, Matrix3d dest) {
		dest.m00 = -src.m00;
		dest.m01 = -src.m02;
		dest.m02 = -src.m01;
		dest.m10 = -src.m10;
		dest.m11 = -src.m12;
		dest.m12 = -src.m11;
		dest.m20 = -src.m20;
		dest.m21 = -src.m22;
		dest.m22 = -src.m21;
		return dest;
	}

	public Matrix3d setIdentity() {
		return setIdentity(this);
	}

	public static Matrix3d setIdentity(Matrix3d m) {
		m.m00 = 1.0;
		m.m01 = 0.0;
		m.m02 = 0.0;
		m.m10 = 0.0;
		m.m11 = 1.0;
		m.m12 = 0.0;
		m.m20 = 0.0;
		m.m21 = 0.0;
		m.m22 = 1.0;
		return m;
	}

	public Matrix3d setZero() {
		return setZero(this);
	}

	public static Matrix3d setZero(Matrix3d m) {
		m.m00 = 0.0;
		m.m01 = 0.0;
		m.m02 = 0.0;
		m.m10 = 0.0;
		m.m11 = 0.0;
		m.m12 = 0.0;
		m.m20 = 0.0;
		m.m21 = 0.0;
		m.m22 = 0.0;
		return m;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(m00, m01, m02, m10, m11, m12, m20, m21, m22);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Matrix3d) {
			final Matrix3d o = (Matrix3d)obj;
			return m00 == o.m00 &&
					m01 == o.m01 &&
					m02 == o.m02 &&
					m10 == o.m10 &&
					m11 == o.m11 &&
					m12 == o.m12 &&
					m20 == o.m20 &&
					m21 == o.m21 &&
					m22 == o.m22;
		}
		return false;
	}

}
