package openmods.colors;

public class CYMK {
	private float cyan, yellow, magenta, key;

	public CYMK(float c, float y, float m, float k) {
		this.cyan = c;
		this.yellow = y;
		this.magenta = m;
		this.key = k;
	}

	public float getCyan() {
		return cyan;
	}

	public void setCyan(float cyan) {
		this.cyan = cyan;
	}

	public float getYellow() {
		return yellow;
	}

	public void setYellow(float yellow) {
		this.yellow = yellow;
	}

	public float getMagenta() {
		return magenta;
	}

	public void setMagenta(float magenta) {
		this.magenta = magenta;
	}

	public float getKey() {
		return key;
	}

	public void setKey(float key) {
		this.key = key;
	}

}