package openmods.geometry;

import net.minecraft.util.math.AxisAlignedBB;

public class AabbBuilder {

	private float minX;

	private float minY;

	private float minZ;

	private float maxX;

	private float maxY;

	private float maxZ;

	public AabbBuilder(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	public static AabbBuilder create() {
		return new AabbBuilder(
				Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
				Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
	}

	public AabbBuilder addPoint(float x, float y, float z) {
		if (x < minX) minX = x;
		if (x > maxX) maxX = x;

		if (y < minY) minY = y;
		if (y > maxY) maxY = y;

		if (z < minZ) minZ = z;
		if (z > maxZ) maxZ = z;
		return this;
	}

	public AxisAlignedBB build() {
		return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
	}
}
