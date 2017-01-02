package openmods.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import openmods.reflection.FieldAccess;

public class EntityUtils {

	public static Vec3d getPredictedPosition(Entity entity) {
		return entity.getPositionVector().addVector(
				entity.motionX,
				entity.motionY,
				entity.motionZ);
	}

	public static RayTraceResult raytraceEntity(Entity entity) {

		if (entity == null || entity.worldObj == null) { return null; }

		return entity.worldObj.rayTraceBlocks(
				entity.getPositionVector(),
				getPredictedPosition(entity),
				false,
				true,
				false);
	}

	private static final FieldAccess<Boolean> ENTITY_INVULNERABLE = FieldAccess.create(Entity.class, "invulnerable", "field_83001_bt");

	public static void setEntityInvulnerable(Entity entity, boolean state) {
		ENTITY_INVULNERABLE.set(entity, state);
	}
}
