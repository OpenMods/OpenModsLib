package openmods.sync;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import openmods.GenericTank;
import openmods.OpenMods;

public class SyncableTank extends GenericTank implements ISyncableObject {

	private boolean dirty = false;
	private long ticksSinceChange = 0;

	public SyncableTank(int capacity, FluidStack... acceptableFluids) {
		super(capacity, acceptableFluids);
	}

	@Override
	public int getTicksSinceChange(World world) {
		return (int)(OpenMods.proxy.getTicks(world) - ticksSinceChange);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void markClean() {
		dirty = false;
	}

	@Override
	public void markDirty() {
		dirty = true;
	}

	@Override
	public void readFromStream(DataInput stream) throws IOException {
		int fluidId = stream.readInt();
		if (fluidId > -1) {
			int fluidAmount = stream.readInt();
			short len = readShort();
			NBTTagCompound tag;
			if (len < 0)
				tag = null;
			byte[] bytes = new byte[len];
			stream.readFully(bytes);
			tag = CompressedStreamTools.decompress(bytes);
			this.fluid = new FluidStack(fluidId, fluidAmount, tag);
		} else {
			this.fluid = null;
		}
	}

	@Override
	public void writeToStream(DataOutput stream, boolean fullData) throws IOException {
		if (fluid != null) {
			stream.writeInt(fluid.fluidID);
			stream.writeInt(fluid.amount);
			if (compound == null) {
				stream.writeShort(-1);
	 		}
			else {
				byte[] bytes = CompressedStreamTools.compress(fluid.tag);
				stream.writeShort(bytes.length);
				stream.write(bytes);
			}
		} else {
			stream.writeInt(-1);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		this.writeToNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		this.readFromNBT(tag);
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		int filled = super.fill(resource, doFill);
		if (filled > 0) {
			markDirty();
		}
		return filled;
	}

	@Override
	public FluidStack drain(FluidStack stack, boolean doDrain) {
		FluidStack drained = super.drain(stack, doDrain);
		if (drained != null) {
			markDirty();
		}
		return drained;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		FluidStack drained = super.drain(maxDrain, doDrain);
		if (drained != null) {
			markDirty();
		}
		return drained;
	}

	@Override
	public void resetChangeTimer(World world) {
		ticksSinceChange = OpenMods.proxy.getTicks(world);
	}

}
