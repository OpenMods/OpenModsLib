package openmods.events.network;

import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import openmods.Log;
import openmods.inventory.IFakeSlotListener;
import openmods.inventory.IInventoryProvider;

public class FakeSlotServer {
	public static final FakeSlotServer instance = new FakeSlotServer();

	@SubscribeEvent
	public void onFakeSlotChange(FakeSlotEventPacket event) {
		// This is being called on the server side everytime a client clicks on
		// a Fake Slot.

		// It grabs the IFakeSlotListener the GUI click was operating on
		TileEntity te = event.getWorld().getTileEntity(event.xCoord, event.yCoord, event.zCoord);
		if (!(te instanceof IFakeSlotListener)) { throw new UnsupportedOperationException("TileEntity does not implement IFakeSlotListener"); }

		// And reports the slot change accordingly.
		IFakeSlotListener listener = (IFakeSlotListener)te;
		listener.onFakeSlotChange(event.slot, event.stack);
	}
}
