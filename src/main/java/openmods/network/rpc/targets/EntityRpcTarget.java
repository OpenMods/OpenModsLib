package openmods.network.rpc.targets;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
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
		output.writeResourceLocation(entity.world.getDimensionKey().getLocation());
		output.writeInt(entity.getEntityId());
	}

	@Override
	public void readFromStreamStream(LogicalSide side, PacketBuffer input) {
		RegistryKey<World> worldId = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, input.readResourceLocation());
		int entityId = input.readInt();

		World world = WorldUtils.getWorld(side, worldId);
		entity = world.getEntityByID(entityId);
	}

	@Override
	public void afterCall() {}
}
