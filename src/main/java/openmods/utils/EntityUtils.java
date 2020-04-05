package openmods.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class EntityUtils {

	public static RayTraceResult raytraceEntity(Entity entity) {

		if (entity == null || entity.world == null) { return null; }

		final Vec3d pos = entity.getPositionVec();
		return entity.world.rayTraceBlocks(
				new RayTraceContext(
						pos,
						pos.add(entity.getMotion()),
						RayTraceContext.BlockMode.COLLIDER,
						RayTraceContext.FluidMode.NONE,
						entity
				)
		);
	}

}
