package openmods.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public abstract class SimpleNetTileEntity extends OpenTileEntity {

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return writeToPacket(this);
	}

	public static SPacketUpdateTileEntity writeToPacket(TileEntity te) {
		NBTTagCompound data = new NBTTagCompound();
		te.writeToNBT(data);
		return new SPacketUpdateTileEntity(te.getPos(), 42, data);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}
}