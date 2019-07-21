package openmods.liquids;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import openmods.utils.bitmap.IReadableBitMap;

public abstract class SidedFluidCapabilityWrapper {

	private final IFluidHandler tank;

	private final Map<Direction, IFluidHandler> handlers = Maps.newEnumMap(Direction.class);

	private static final IFluidTankProperties[] EMPTY = new IFluidTankProperties[0];

	private class Handler implements IFluidHandler {
		private final Direction side;

		public Handler(Direction side) {
			this.side = side;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			if (!canInteract(side)) return EMPTY;
			return tank.getTankProperties();
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			if (!canFill(side)) return 0;
			return tank.fill(resource, doFill);
		}

		@Override
		@Nullable
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			if (!canDrain(side)) return null;
			return tank.drain(resource, doDrain);
		}

		@Override
		@Nullable
		public FluidStack drain(int maxDrain, boolean doDrain) {
			if (!canDrain(side)) return null;
			return tank.drain(maxDrain, doDrain);
		}

	}

	private SidedFluidCapabilityWrapper(IFluidHandler tank) {
		this.tank = tank;

		for (Direction side : Direction.VALUES)
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
