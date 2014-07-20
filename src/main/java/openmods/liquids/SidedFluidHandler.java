package openmods.liquids;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.IFluidTank;
import openmods.sync.SyncableFlags;
import openmods.utils.bitmap.IReadableBitMap;

public class SidedFluidHandler extends GenericFluidHandler {

	public static class Source extends GenericFluidHandler.Source {
		private final IReadableBitMap<ForgeDirection> flags;

		public Source(IReadableBitMap<ForgeDirection> flags, IFluidTank tank) {
			super(tank);
			this.flags = flags;
		}

		@Override
		public boolean canDrain(ForgeDirection from, Fluid fluid) {
			return flags.get(from) && super.canDrain(from, fluid);
		}
	}

	public static class Drain extends GenericFluidHandler.Drain {
		private final IReadableBitMap<ForgeDirection> flags;

		public Drain(IReadableBitMap<ForgeDirection> flags, IFluidTank tank) {
			super(tank);
			this.flags = flags;
		}

		@Override
		public boolean canFill(ForgeDirection from, Fluid fluid) {
			return flags.get(from) && super.canFill(from, fluid);
		}
	}

	private final SyncableFlags flags;

	public SidedFluidHandler(SyncableFlags flags, IFluidTank tank) {
		super(tank);
		this.flags = flags;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		return flags.get(from) && super.canFill(from, fluid);
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return flags.get(from) && super.canDrain(from, fluid);
	}
}
