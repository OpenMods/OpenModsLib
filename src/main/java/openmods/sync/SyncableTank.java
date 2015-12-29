package openmods.sync;

import java.io.*;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import openmods.api.IValueProvider;
import openmods.liquids.GenericTank;
import openmods.utils.ByteUtils;

import com.google.common.io.ByteStreams;

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
	public void readFromStream(DataInputStream stream) throws IOException {
		if (stream.readBoolean()) {
			String fluidName = stream.readUTF();
			Fluid fluid = FluidRegistry.getFluid(fluidName);

			int fluidAmount = stream.readInt();

			this.fluid = new FluidStack(fluid, fluidAmount);

			final int tagSize = ByteUtils.readVLI(stream);
			if (tagSize > 0) {
				this.fluid.tag = CompressedStreamTools.readCompressed(ByteStreams.limit(stream, tagSize));
			}

		} else {
			this.fluid = null;
		}
	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
		if (fluid != null) {
			stream.writeBoolean(true);
			stream.writeUTF(FluidRegistry.getFluidName(fluid.getFluid()));
			stream.writeInt(fluid.amount);
			if (fluid.tag != null) {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				CompressedStreamTools.writeCompressed(fluid.tag, buffer);

				byte[] bytes = buffer.toByteArray();
				ByteUtils.writeVLI(stream, bytes.length);
				stream.write(bytes);
			} else {
				stream.writeByte(0);
			}
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
