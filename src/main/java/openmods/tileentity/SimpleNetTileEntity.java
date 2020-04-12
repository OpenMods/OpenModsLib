package openmods.tileentity;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

public abstract class SimpleNetTileEntity extends OpenTileEntity {

	public SimpleNetTileEntity(TileEntityType<?> type) {
		super(type);
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return writeToPacket(this);
	}

	public static SUpdateTileEntityPacket writeToPacket(TileEntity te) {
		CompoundNBT data = new CompoundNBT();
		te.write(data);
		return new SUpdateTileEntityPacket(te.getPos(), 42, data);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		read(getBlockState(), pkt.getNbtCompound());
	}
}