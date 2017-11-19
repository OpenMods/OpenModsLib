package openmods.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class EntityUtils {

	public static Vec3d getPredictedPosition(Entity entity) {
		return entity.getPositionVector().addVector(
				entity.motionX,
				entity.motionY,
				entity.motionZ);
	}

	public static RayTraceResult raytraceEntity(Entity entity) {

		if (entity == null || entity.world == null) { return null; }

		return entity.world.rayTraceBlocks(
				entity.getPositionVector(),
				getPredictedPosition(entity),
				false,
				true,
				false);
	}

}
