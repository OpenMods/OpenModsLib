package openmods.geometry;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

public class Hitbox {

	public String name;

	public Vector3d from;

	public Vector3d to;

	private transient AxisAlignedBB aabb;

	public AxisAlignedBB aabb() {
		if (aabb == null)
			aabb = new AxisAlignedBB(from, to);

		return aabb;
	}

}
