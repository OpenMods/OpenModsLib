package openmods.tileentity;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;

public abstract class SimpleNetTileEntity extends OpenTileEntity {

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return writeToPacket(this);
	}

	public static SUpdateTileEntityPacket writeToPacket(TileEntity te) {
		CompoundNBT data = new CompoundNBT();
		te.writeToNBT(data);
		return new SUpdateTileEntityPacket(te.getPos(), 42, data);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		readFromNBT(pkt.getNbtCompound());
	}
}