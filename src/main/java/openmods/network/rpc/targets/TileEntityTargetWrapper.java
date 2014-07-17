package openmods.network.rpc.targets;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import openmods.network.rpc.ITargetWrapper;

public class TileEntityTargetWrapper implements ITargetWrapper {

	private TileEntity te;

	public TileEntityTargetWrapper() {}

	public TileEntityTargetWrapper(TileEntity te) {
		this.te = te;
	}

	@Override
	public Object getTarget() {
		return te;
	}

	@Override
	public void writeToStream(DataOutput output) throws IOException {
		output.writeInt(te.xCoord);
		output.writeInt(te.yCoord);
		output.writeInt(te.zCoord);
	}

	@Override
	public void readFromStreamStream(EntityPlayer player, DataInput input) throws IOException {
		int x = input.readInt();
		int y = input.readInt();
		int z = input.readInt();

		World world = player.worldObj;
		te = world.getTileEntity(x, y, z);
	}

}
