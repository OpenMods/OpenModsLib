package openmods.network.rpc.targets;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import openmods.network.rpc.IRpcTarget;

import com.google.common.base.Preconditions;

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
	public void writeToStream(DataOutput output) throws IOException {
		output.writeInt(entity.worldObj.provider.dimensionId);
		output.writeInt(entity.getEntityId());
	}

	@Override
	public void readFromStreamStream(EntityPlayer player, DataInput input) throws IOException {
		int worldId = input.readInt();
		int entityId = input.readInt();

		World world = DimensionManager.getWorld(worldId);
		Preconditions.checkNotNull(world, "Can't find dimension id %s", worldId);
		entity = world.getEntityByID(entityId);
	}

	@Override
	public void afterCall() {}
}
