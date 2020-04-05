package openmods.liquids;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.util.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import openmods.utils.bitmap.IReadableBitMap;

public abstract class SidedFluidCapabilityWrapper {

	private final IFluidHandler tank;

	private final Map<Direction, IFluidHandler> handlers = Maps.newEnumMap(Direction.class);

	private class Handler implements IFluidHandler {
		private final Direction side;

		public Handler(Direction side) {
			this.side = side;
		}

		@Override
		public int getTanks() {
			return canInteract(side) ? 1 : 0;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return canInteract(side) ? SidedFluidCapabilityWrapper.this.tank.getFluidInTank(tank) : FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return canInteract(side) ? SidedFluidCapabilityWrapper.this.tank.getTankCapacity(tank) : 0;
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return canInteract(side) && SidedFluidCapabilityWrapper.this.tank.isFluidValid(tank, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction doFill) {
			if (!canFill(side)) return 0;
			return tank.fill(resource, doFill);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (!canDrain(side)) return FluidStack.EMPTY;
			return tank.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			if (!canDrain(side)) return FluidStack.EMPTY;
			return tank.drain(maxDrain, action);
		}

	}

	private SidedFluidCapabilityWrapper(IFluidHandler tank) {
		this.tank = tank;

		for (Direction side : Direction.values())
			handlers.put(side, new Handler(side));
	}

	protected abstract boolean canFill(Direction side);

	protected abstract boolean canDrain(Direction side);

	protected boolean canInteract(Direction side) {
		return canFill(side) || canDrain(side);
	}

	public boolean hasHandler(Direction side) {
		return side == null || canInteract(side);
	}

	public IFluidHandler getHandler(Direction side) {
		return side != null? handlers.get(side) : tank;
	}

	public static SidedFluidCapabilityWrapper wrap(IFluidHandler tank, final IReadableBitMap<Direction> flags, boolean canDrain, boolean canFill) {
		if (canDrain && canFill) return new SidedFluidCapabilityWrapper(tank) {
			@Override
			protected boolean canFill(Direction side) {
				return flags.get(side);
			}

			@Override
			protected boolean canDrain(Direction side) {
				return flags.get(side);
			}
		};

		if (canDrain) return new SidedFluidCapabilityWrapper(tank) {
			@Override
			protected boolean canFill(Direction side) {
				return false;
			}

			@Override
			protected boolean canDrain(Direction side) {
				return flags.get(side);
			}
		};

		if (canFill) return new SidedFluidCapabilityWrapper(tank) {
			@Override
			protected boolean canFill(Direction side) {
				return flags.get(side);
			}

			@Override
			protected boolean canDrain(Direction side) {
				return false;
			}
		};

		// ... or should I punish that with exception?
		return new SidedFluidCapabilityWrapper(tank) {

			@Override
			protected boolean canFill(Direction side) {
				return false;
			}

			@Override
			protected boolean canDrain(Direction side) {
				return false;
			}
		};

	}
}
