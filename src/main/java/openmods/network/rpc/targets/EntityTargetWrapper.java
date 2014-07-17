package openmods.network.rpc.targets;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import openmods.network.rpc.ITargetWrapper;

public class EntityTargetWrapper implements ITargetWrapper {

	private Entity entity;

	public EntityTargetWrapper() {}

	public EntityTargetWrapper(Entity entity) {
		this.entity = entity;
	}

	@Override
	public Object getTarget() {
		return entity;
	}

	@Override
	public void writeToStream(DataOutput output) throws IOException {
		output.writeInt(entity.getEntityId());
	}

	@Override
	public void readFromStreamStream(EntityPlayer player, DataInput input) throws IOException {
		int entityId = input.readInt();

		World world = player.worldObj;
		entity = world.getEntityByID(entityId);
	}
}
