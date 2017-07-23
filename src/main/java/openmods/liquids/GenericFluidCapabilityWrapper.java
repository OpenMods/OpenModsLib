package openmods.liquids;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

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
		public int fill(FluidStack resource, boolean doFill) {
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
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			return null;
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			return null;
		}
	}

	private final IFluidHandler tank;

	public GenericFluidCapabilityWrapper(IFluidHandler tank) {
		this.tank = tank;
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (resource == null || !canFill(resource.getFluid())) return 0;
		return tank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if (resource == null || !canDrain(resource.getFluid())) { return null; }
		return tank.drain(resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		if (!canDrain(null)) return null;
		return tank.drain(maxDrain, doDrain);
	}

	protected boolean canFill(Fluid fluid) {
		return true;
	}

	protected boolean canDrain(Fluid fluid) {
		return true;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return tank.getTankProperties();
	}

}
