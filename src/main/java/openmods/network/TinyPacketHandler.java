package openmods.network;

import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet131MapData;
import openmods.Log;
import openmods.OpenMods;
import cpw.mods.fml.common.network.ITinyPacketHandler;

public class TinyPacketHandler implements ITinyPacketHandler {

	public static final short TYPE_SYNC = 0;

	@Override
	public void handle(NetHandler handler, Packet131MapData packet) {
		try {
			OpenMods.syncableManager.handlePacket(packet);
		} catch (Exception e) {
			Log.warn(e, "Error while handling sync map data from player '%s'", handler.getPlayer());
		}
	}

}
