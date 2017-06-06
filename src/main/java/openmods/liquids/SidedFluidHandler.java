package openmods.liquids;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.IFluidTank;
import openmods.sync.SyncableFlags;
import openmods.utils.bitmap.IReadableBitMap;

@Deprecated
public class SidedFluidHandler extends GenericFluidHandler {

	public static class Source extends GenericFluidHandler.Source {
		private final IReadableBitMap<EnumFacing> flags;

		public Source(IReadableBitMap<EnumFacing> flags, IFluidTank tank) {
			super(tank);
			this.flags = flags;
		}

		@Override
		public boolean canDrain(EnumFacing from, Fluid fluid) {
			return flags.get(from) && super.canDrain(from, fluid);
		}
	}

	public static class Drain extends GenericFluidHandler.Drain {
		private final IReadableBitMap<EnumFacing> flags;

		public Drain(IReadableBitMap<EnumFacing> flags, IFluidTank tank) {
			super(tank);
			this.flags = flags;
		}

		@Override
		public boolean canFill(EnumFacing from, Fluid fluid) {
			return flags.get(from) && super.canFill(from, fluid);
		}
	}

	private final SyncableFlags flags;

	public SidedFluidHandler(SyncableFlags flags, IFluidTank tank) {
		super(tank);
		this.flags = flags;
	}

	@Override
	public boolean canFill(EnumFacing from, Fluid fluid) {
		return flags.get(from) && super.canFill(from, fluid);
	}

	@Override
	public boolean canDrain(EnumFacing from, Fluid fluid) {
		return flags.get(from) && super.canDrain(from, fluid);
	}
}
