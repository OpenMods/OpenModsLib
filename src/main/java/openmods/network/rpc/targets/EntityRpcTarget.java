package openmods.network.rpc.targets;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.LogicalSide;
import openmods.network.rpc.IRpcTarget;
import openmods.utils.WorldUtils;

public class EntityRpcTarget implements IRpcTarget {

	private Entity entity;

	public EntityRpcTarget() {}

	public EntityRpcTarget(Entity entity) {
		this.entity = entity;
	}

	@Override
	public Object getTarget() {
		return entity;
	}

	@Override
	public void writeToStream(PacketBuffer output) {
		output.writeInt(entity.world.getDimension().getType().getId());
		output.writeInt(entity.getEntityId());
	}

	@Override
	public void readFromStreamStream(LogicalSide side, PacketBuffer input) {
		DimensionType worldId = DimensionType.getById(input.readInt());
		int entityId = input.readInt();

		World world = WorldUtils.getWorld(side, worldId);
		entity = world.getEntityByID(entityId);
	}

	@Override
	public void afterCall() {}
}
