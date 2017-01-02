package openmods.math;

public class Complex {

	public static final Complex ZERO = new Complex(0, 0);
	public static final Complex ONE = new Complex(1, 0);
	public static final Complex I = new Complex(0, 1);

	public final double re;

	public final double im;

	private Complex(double real, double imaginary) {
		this.re = real;
		this.im = imaginary;
	}

	public static Complex real(double v) {
		return new Complex(v, 0);
	}

	public static Complex imaginary(double v) {
		return new Complex(0, v);
	}

	public static Complex polar(double r, double phase) {
		return new Complex(r * Math.cos(phase), r * Math.sin(phase));
	}

	public static Complex cartesian(double x, double y) {
		return new Complex(x, y);
	}

	public static Complex create(double re, double im) {
		return new Complex(re, im);
	}

	public Complex conj() {
		return new Complex(this.re, -this.im);
	}

	public Complex add(Complex other) {
		return new Complex(this.re + other.re, this.im + other.im);
	}

	public Complex subtract(Complex other) {
		return new Complex(this.re - other.re, this.im - other.im);
	}

	public Complex negate() {
		return new Complex(-this.re, -this.im);
	}

	public Complex multiply(double scalar) {
		return new Complex(scalar * this.re, scalar * this.im);
	}

	public Complex multiply(Complex other) {
		return new Complex(this.re * other.re - this.im * other.im,
				this.im * other.re + this.re * other.im);
	}

	public Complex divide(double scalar) {
		return new Complex(this.re / scalar, this.im / scalar);
	}

	public Complex divide(Complex other) {
		final double denominator = other.squareModule();
		return new Complex((this.re * other.re + this.im * other.im) / denominator,
				(this.im * other.re - this.re * other.im) / denominator);
	}

	public double squareModule() {
		return this.re * this.re + this.im * this.im;
	}

	public double abs() {
		return Math.sqrt(squareModule());
	}

	public double phase() {
		return Math.atan2(this.im, this.re);
	}

	public Complex exp() {
		final double r = Math.exp(this.re);
		return new Complex(r * Math.cos(this.im), r * Math.sin(this.im));
	}

	public Complex ln() {
		return new Complex(Math.log(abs()), phase());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(im);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(re);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof Complex) {
			final Complex other = (Complex)obj;
			return (Double.doubleToLongBits(im) == Double.doubleToLongBits(other.im)) &&
					(Double.doubleToLongBits(re) == Double.doubleToLongBits(other.re));
		}

		return false;
	}

	@Override
	public String toString() {
		return "(" + re + "+" + im + "I)";
	}
}
