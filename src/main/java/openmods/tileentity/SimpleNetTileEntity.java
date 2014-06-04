package openmods.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import openmods.tileentity.OpenTileEntity;

public abstract class SimpleNetTileEntity extends OpenTileEntity {

	@Override
	public Packet getDescriptionPacket() {
		return writeToPacket(this);
	}

	public static Packet writeToPacket(TileEntity te) {
		NBTTagCompound data = new NBTTagCompound();
		te.writeToNBT(data);
		return new S35PacketUpdateTileEntity(te.xCoord, te.yCoord, te.zCoord, 42, data);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		readFromNBT(pkt.func_148857_g());
	}
}