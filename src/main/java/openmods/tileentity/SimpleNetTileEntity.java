package openmods.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public abstract class SimpleNetTileEntity extends OpenTileEntity {

	@Override
	public Packet<?> getDescriptionPacket() {
		return writeToPacket(this);
	}

	public static Packet<?> writeToPacket(TileEntity te) {
		NBTTagCompound data = new NBTTagCompound();
		te.writeToNBT(data);
		return new S35PacketUpdateTileEntity(te.getPos(), 42, data);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}
}