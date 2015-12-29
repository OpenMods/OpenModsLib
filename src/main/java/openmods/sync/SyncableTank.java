package openmods.sync;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import openmods.api.IValueProvider;
import openmods.liquids.GenericTank;

public class SyncableTank extends GenericTank implements ISyncableObject, IValueProvider<FluidStack> {

	private boolean dirty = false;

	public SyncableTank(int capacity) {
		super(capacity);
	}

	public SyncableTank(int capacity, Fluid... acceptableFluids) {
		super(capacity, acceptableFluids);
	}

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
	public void readFromStream(PacketBuffer stream) throws IOException {
		if (stream.readBoolean()) {
			String fluidName = stream.readStringFromBuffer(32767);
			Fluid fluid = FluidRegistry.getFluid(fluidName);

			int fluidAmount = stream.readInt();

			this.fluid = new FluidStack(fluid, fluidAmount);
			this.fluid.tag = stream.readNBTTagCompoundFromBuffer();
		} else {
			this.fluid = null;
		}
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		if (fluid != null) {
			stream.writeBoolean(true);
			final String id = FluidRegistry.getFluidName(fluid.getFluid());
			stream.writeString(id);
			stream.writeInt(fluid.amount);
			stream.writeNBTTagCompoundToBuffer(fluid.tag);
		} else {
			stream.writeBoolean(false);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		final NBTTagCompound tankTag = new NBTTagCompound();
		this.writeToNBT(tankTag);

		tag.setTag(name, tankTag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		if (tag.hasKey(name, Constants.NBT.TAG_COMPOUND)) {
			final NBTTagCompound tankTag = tag.getCompoundTag(name);
			this.readFromNBT(tankTag);
		} else {
			// For legacy worlds - tag was saved in wrong place due to bug
			this.readFromNBT(tag);
		}
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
