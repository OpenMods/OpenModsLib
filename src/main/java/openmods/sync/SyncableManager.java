package openmods.sync;

import java.io.DataInput;
import java.io.IOException;
import java.util.Set;

import net.minecraft.network.packet.Packet131MapData;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.world.World;
import openmods.LibConfig;
import openmods.OpenMods;
import openmods.network.PacketLogger;

import com.google.common.io.ByteStreams;

public class SyncableManager {

	public void handlePacket(Packet250CustomPayload packet) throws IOException {
		DataInput input = ByteStreams.newDataInput(packet.data);

		World world = readWorld(input);

		ISyncHandler handler = SyncMap.findSyncMap(world, input);
		if (handler != null) {
			Set<ISyncableObject> changes = handler.getSyncMap().readFromStream(input);
			handler.onSynced(changes);

			if (LibConfig.logPackets) PacketLogger.log(packet, true, handler.toString(), handler.getClass().toString(), Integer.toString(changes.size()));
		}
	}

	public void handlePacket(Packet131MapData packet) throws IOException {
		DataInput input = ByteStreams.newDataInput(packet.itemData);

		World world = readWorld(input);

		ISyncHandler handler = SyncMap.findSyncMap(world, input);
		if (handler != null) {
			Set<ISyncableObject> changes = handler.getSyncMap().readFromStream(input);
			handler.onSynced(changes);

			if (LibConfig.logPackets) PacketLogger.log(packet, true, handler.toString(), handler.getClass().toString(), Integer.toString(changes.size()));
		}
	}

	private static World readWorld(DataInput input) throws IOException {
		boolean toServer = input.readBoolean();

		if (toServer) {
			int dimension = input.readInt();
			return OpenMods.proxy.getServerWorld(dimension);
		} else {
			return OpenMods.proxy.getClientWorld();
		}
	}
}
