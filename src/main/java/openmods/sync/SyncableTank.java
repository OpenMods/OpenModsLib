package openmods.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import openmods.api.IValueProvider;
import openmods.liquids.GenericTank;
import openmods.utils.ByteUtils;

public class SyncableTank extends GenericTank implements ISyncableObject, IValueProvider<FluidStack> {

	private boolean dirty = false;

	public SyncableTank(int capacity, FluidStack... acceptableFluids) {
		super(capacity, acceptableFluids);
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
	public void readFromStream(DataInputStream stream) throws IOException {
		if (stream.readBoolean()) {
			int fluidId = ByteUtils.readVLI(stream);
			int fluidAmount = stream.readInt();
			NBTTagCompound tag = null;
			if (stream.readBoolean()) tag = CompressedStreamTools.readCompressed(stream);
			this.fluid = new FluidStack(fluidId, fluidAmount, tag);
		} else {
			this.fluid = null;
		}
	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
		if (fluid != null) {
			stream.writeBoolean(true);
			ByteUtils.writeVLI(stream, fluid.fluidID);
			stream.writeInt(fluid.amount);
			if (fluid.tag != null) {
				stream.writeBoolean(true);
				CompressedStreamTools.writeCompressed(fluid.tag, stream);
			} else {
				stream.writeBoolean(false);
			}
		} else {
			stream.writeBoolean(false);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		NBTTagCompound compound = new NBTTagCompound();
                this.writeToNBT(compound);
                tag.setTag(name, compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
                this.readFromNBT(tag.getCompoundTag(name));
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		int filled = super.fill(resource, doFill);
		if (doFill && filled > 0) markDirty();
		return filled;
	}

	@Override
	public FluidStack drain(FluidStack stack, boolean doDrain) {
		FluidStack drained = super.drain(stack, doDrain);
		if (doDrain && drained != null) markDirty();
		return drained;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		FluidStack drained = super.drain(maxDrain, doDrain);
		if (doDrain && drained != null) markDirty();
		return drained;
	}

	@Override
	public FluidStack getValue() {
		FluidStack stack = super.getFluid();
		return stack != null? stack.copy() : null;
	}

}
