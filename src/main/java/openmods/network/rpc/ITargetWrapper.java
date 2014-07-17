package openmods.network.rpc;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;

public interface ITargetWrapper {

	public Object getTarget();

	public void writeToStream(DataOutput output) throws IOException;

	public void readFromStreamStream(EntityPlayer player, DataInput input) throws IOException;
}
