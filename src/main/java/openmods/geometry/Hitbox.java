package openmods.geometry;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class Hitbox {

	public String name;

	public Vec3d from;

	public Vec3d to;

	private transient AxisAlignedBB aabb;

	public AxisAlignedBB aabb() {
		if (aabb == null)
			aabb = new AxisAlignedBB(from, to);

		return aabb;
	}

}
