package openmods.network.events;

import openmods.Log;
import openmods.tileentity.OpenTileEntity;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class TileEntityEventHandler {

	@SubscribeEvent
	public void onTileEntityEvent(TileEntityMessageEventPacket event) {
		OpenTileEntity tile = event.getTileEntity();
		if (tile != null) {
			tile.onEvent(event);
		} else {
			Log.warn("Received packet for invalid te @ (%d,%d,%d)", event.xCoord, event.yCoord, event.zCoord);
		}
	}
}
