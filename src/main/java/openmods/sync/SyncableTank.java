package openmods.sync;

import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import openmods.api.IValueProvider;
import openmods.liquids.GenericTank;

public class SyncableTank extends GenericTank implements ISyncableObject, IValueProvider<FluidStack> {

	private boolean dirty = false;

	public SyncableTank() {
		super(0);
	}

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
		this.fluid = FluidStack.readFromPacket(stream);
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		this.fluid.writeToPacket(stream);
	}

	@Override
	public void writeToNBT(CompoundNBT tag, String name) {
		final CompoundNBT tankTag = new CompoundNBT();
		this.writeToNBT(tankTag);
		tag.put(name, tankTag);
	}

	@Override
	public void readFromNBT(CompoundNBT tag, String name) {
		if (tag.contains(name, Constants.NBT.TAG_COMPOUND)) {
			final CompoundNBT tankTag = tag.getCompound(name);
			this.readFromNBT(tankTag);
		} else {
			// For legacy worlds - tag was saved in wrong place due to bug
			this.readFromNBT(tag);
		}
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		int filled = super.fill(resource, action);
		if (action.execute() && filled > 0) markDirty();
		return filled;
	}

	@Override
	public FluidStack drain(FluidStack stack, FluidAction action) {
		FluidStack drained = super.drain(stack, action);
		if (action.execute()&& !drained.isEmpty()) markDirty();
		return drained;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		FluidStack drained = super.drain(maxDrain, action);
		if (action.execute() && !drained.isEmpty()) markDirty();
		return drained;
	}

	@Override
	public FluidStack getValue() {
		return super.getFluid().copy();
	}

	@Override
	public void setFluid(@Nullable FluidStack fluid) {
		super.setFluid(fluid);
		markDirty();
	}
}
