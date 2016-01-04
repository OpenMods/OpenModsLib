package openmods.colors;

public class RGB {
	public int r;
	public int g;
	public int b;

	public RGB(float r, float g, float b) {
		this.r = ((int)(r * 255)) & 0xFF;
		this.g = ((int)(g * 255)) & 0xFF;
		this.b = ((int)(b * 255)) & 0xFF;
	}

	public RGB(int r, int g, int b) {
		this.r = r & 0xFF;
		this.g = g & 0xFF;
		this.b = b & 0xFF;
	}

	public RGB(int color) {
		this(((color & 0xFF0000) >> 16), ((color & 0x00FF00) >> 8), (color & 0x0000FF));
	}

	public RGB() {}

	public void setColor(int color) {
		r = (color & 0xFF0000) >> 16;
		g = (color & 0x00FF00) >> 8;
		b = color & 0x0000FF;
	}

	public int getColor() {
		return r << 16 | g << 8 | b;
	}

	public float getR() {
		return r / 255f;
	}

	public float getG() {
		return g / 255f;
	}

	public float getB() {
		return b / 255f;
	}

	public RGB interpolate(RGB other, double amount) {
		int iPolR = (int)(r * (1D - amount) + other.r * amount);
		int iPolG = (int)(g * (1D - amount) + other.g * amount);
		int iPolB = (int)(b * (1D - amount) + other.b * amount);
		return new RGB(iPolR, iPolG, iPolB);
	}

	public CYMK toCYMK() {
		float cyan = 1f - (r / 255f);
		float magenta = 1f - (g / 255f);
		float yellow = 1f - (b / 255f);
		float K = 1;
		if (cyan < K) {
			K = cyan;
		}
		if (magenta < K) {
			K = magenta;
		}
		if (yellow < K) {
			K = yellow;
		}
		if (K == 1) {
			cyan = 0;
			magenta = 0;
			yellow = 0;
		} else {
			cyan = (cyan - K) / (1f - K);
			magenta = (magenta - K) / (1f - K);
			yellow = (yellow - K) / (1f - K);
		}
		return new CYMK(cyan, yellow, magenta, K);
	}

	public int distanceSq(RGB other) {
		final int dR = this.r - other.r;
		final int dG = this.g - other.g;
		final int dB = this.b - other.b;
		return (dR * dR) + (dG * dG) + (dB * dB);
	}
}