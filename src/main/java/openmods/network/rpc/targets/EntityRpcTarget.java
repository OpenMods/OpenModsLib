package openmods.network.rpc.targets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
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
		output.writeInt(entity.worldObj.provider.getDimensionId());
		output.writeInt(entity.getEntityId());
	}

	@Override
	public void readFromStreamStream(EntityPlayer player, PacketBuffer input) {
		int worldId = input.readInt();
		int entityId = input.readInt();

		World world = WorldUtils.getWorld(worldId);
		entity = world.getEntityByID(entityId);
	}

	@Override
	public void afterCall() {}
}
