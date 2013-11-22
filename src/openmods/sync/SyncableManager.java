package openmods.sync;

import java.io.DataInput;
import java.io.IOException;
import java.util.Set;

import openmods.OpenMods;
import openmods.common.api.IOpenMod;
import openmods.interfaces.IProxy;

import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.world.World;

import com.google.common.io.ByteStreams;

public class SyncableManager {
	
	public void handlePacket(Packet250CustomPayload packet) throws IOException {
		DataInput input = ByteStreams.newDataInput(packet.data);

		boolean toServer = input.readBoolean();

		World world;
		if (toServer) {
			int dimension = input.readInt();
			world = OpenMods.proxy.getServerWorld(dimension);
		} else {
			world = OpenMods.proxy.getClientWorld();
		}

		ISyncHandler handler = SyncMap.findSyncMap(world, input);
		if (handler != null) {
			Set<ISyncableObject> changes = handler.getSyncMap().readFromStream(input);
			handler.onSynced(changes);
		}
	}
}
