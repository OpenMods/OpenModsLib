package openmods.liquids;

import javax.annotation.Nonnull;
import net.minecraft.fluid.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class GenericFluidCapabilityWrapper implements IFluidHandler {

	public static class Source extends GenericFluidCapabilityWrapper {
		public Source(IFluidHandler tank) {
			super(tank);
		}

		@Override
		public boolean canFill(Fluid fluid) {
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction doFill) {
			return 0;
		}
	}

	public static class Drain extends GenericFluidCapabilityWrapper {
		public Drain(IFluidHandler tank) {
			super(tank);
		}

		@Override
		public boolean canDrain(Fluid fluid) {
			return false;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction doDrain) {
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction doDrain) {
			return FluidStack.EMPTY;
		}
	}

	private final IFluidHandler tank;

	public GenericFluidCapabilityWrapper(IFluidHandler tank) {
		this.tank = tank;
	}

	@Override
	public int fill(FluidStack resource, FluidAction doFill) {
		if (resource == null || !canFill(resource.getFluid())) return 0;
		return tank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction doDrain) {
		if (resource == null || !canDrain(resource.getFluid())) { return FluidStack.EMPTY; }
		return tank.drain(resource.getAmount(), doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction doDrain) {
		if (!canDrain(null)) return FluidStack.EMPTY;
		return tank.drain(maxDrain, doDrain);
	}

	protected boolean canFill(Fluid fluid) {
		return true;
	}

	protected boolean canDrain(Fluid fluid) {
		return true;
	}

	@Override
	public int getTanks() {
		return this.tank.getTanks();
	}

	@Nonnull
	@Override
	public FluidStack getFluidInTank(int tank) {
		return this.tank.getFluidInTank(tank);
	}

	@Override
	public int getTankCapacity(int tank) {
		return this.tank.getTankCapacity(tank);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		if (!canDrain(stack.getFluid())) {
			return false;
		}
		return this.tank.isFluidValid(tank, stack);
	}

}
