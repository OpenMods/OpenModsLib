package openmods.liquids;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.IFluidTank;

@Deprecated
public class GenericFluidHandler implements IFluidHandler {

	public static class Source extends GenericFluidHandler {
		public Source(IFluidTank tank) {
			super(tank);
		}

		@Override
		public boolean canFill(EnumFacing from, Fluid fluid) {
			return false;
		}

		@Override
		public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
			return 0;
		}
	}

	public static class Drain extends GenericFluidHandler {
		public Drain(IFluidTank tank) {
			super(tank);
		}

		@Override
		public boolean canDrain(EnumFacing from, Fluid fluid) {
			return false;
		}

		@Override
		public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
			return null;
		}

		@Override
		public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
			return null;
		}
	}

	private final IFluidTank tank;

	public GenericFluidHandler(IFluidTank tank) {
		this.tank = tank;
	}

	@Override
	public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
		if (resource == null || !canFill(from, resource.getFluid())) return 0;
		return tank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
		if (resource == null || !resource.isFluidEqual(tank.getFluid()) || !canDrain(from, resource.getFluid())) { return null; }
		return tank.drain(resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
		if (!canDrain(from, null)) return null;
		return tank.drain(maxDrain, doDrain);
	}

	@Override
	public boolean canFill(EnumFacing from, Fluid fluid) {
		return true;
	}

	@Override
	public boolean canDrain(EnumFacing from, Fluid fluid) {
		return true;
	}

	@Override
	public FluidTankInfo[] getTankInfo(EnumFacing from) {
		return new FluidTankInfo[] { tank.getInfo() };
	}

}
