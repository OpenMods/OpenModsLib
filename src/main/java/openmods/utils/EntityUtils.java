package openmods.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class EntityUtils {

	public static Vec3 getCurrentPosition(Entity entity) {
		return Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ);
	}

	public static Vec3 getPredictedPosition(Entity entity) {
		return getCurrentPosition(entity).addVector(
				entity.motionX,
				entity.motionY,
				entity.motionZ
				);
	}

	public static MovingObjectPosition raytraceEntity(Entity entity) {

		if (entity == null || entity.worldObj == null) { return null; }

		// TODO: Check 5th param is fine
		return entity.worldObj.func_147447_a(
				getCurrentPosition(entity),
				getPredictedPosition(entity),
				false,
				true,
				false);
	}
}
