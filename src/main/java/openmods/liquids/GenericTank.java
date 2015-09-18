package openmods.liquids;

import java.util.*;

import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;
import openmods.OpenMods;
import openmods.utils.*;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class GenericTank extends FluidTank {

	private List<ForgeDirection> surroundingTanks = Lists.newArrayList();
	private long lastUpdate = 0;
	private final IFluidFilter filter;

	public interface IFluidFilter {
		public boolean canAcceptFluid(FluidStack stack);
	}

	private static final IFluidFilter NO_RESTRICTIONS = new IFluidFilter() {
		@Override
		public boolean canAcceptFluid(FluidStack stack) {
			return true;
		}
	};
	private static final Function<Fluid, FluidStack> FLUID_CONVERTER = new Function<Fluid, FluidStack>() {
		@Override
		@Nullable
		public FluidStack apply(@Nullable Fluid input) {
			return new FluidStack(input, 0);
		}
	};

	private static IFluidFilter filter(final FluidStack... acceptableFluids) {
		if (acceptableFluids.length == 0) return NO_RESTRICTIONS;

		return new IFluidFilter() {
			@Override
			public boolean canAcceptFluid(FluidStack stack) {
				for (FluidStack acceptableFluid : acceptableFluids)
					if (acceptableFluid.isFluidEqual(stack)) return true;

				return false;
			}
		};
	}

	public GenericTank(int capacity) {
		super(capacity);
		this.filter = NO_RESTRICTIONS;
	}

	public GenericTank(int capacity, FluidStack... acceptableFluids) {
		super(capacity);
		this.filter = filter(acceptableFluids);
	}

	public GenericTank(int capacity, Fluid... acceptableFluids) {
		super(capacity);
		this.filter = filter(CollectionUtils.transform(acceptableFluids, FLUID_CONVERTER));
	}

	private static boolean isNeighbourTank(World world, Coord coord, ForgeDirection dir) {
		TileEntity tile = BlockUtils.getTileInDirection(world, coord, dir);
		return tile instanceof IFluidHandler;
	}

	private static Set<ForgeDirection> getSurroundingTanks(World world, Coord coord, Set<ForgeDirection> sides) {
		EnumSet<ForgeDirection> result = EnumSet.noneOf(ForgeDirection.class);
		if (sides == null) sides = DirUtils.VALID_DIRECTIONS;

		for (ForgeDirection dir : sides)
			if (isNeighbourTank(world, coord, dir)) result.add(dir);

		return result;
	}

	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if (resource == null ||
				fluid == null ||
				fluid.isFluidEqual(resource)) return null;

		return drain(resource.amount, doDrain);
	}

	public int getSpace() {
		return getCapacity() - getFluidAmount();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (resource == null || !filter.canAcceptFluid(resource)) return 0;
		return super.fill(resource, doFill);
	}

	private void periodicUpdateNeighbours(World world, Coord coord, Set<ForgeDirection> sides) {
		long currentTime = OpenMods.proxy.getTicks(world);
		if (currentTime - lastUpdate > 10) {
			surroundingTanks = Lists.newArrayList(getSurroundingTanks(world, coord, sides));
			lastUpdate = currentTime;
		}
	}

	private static int tryFillNeighbour(FluidStack drainedFluid, ForgeDirection side, TileEntity otherTank) {
		final FluidStack toFill = drainedFluid.copy();
		final ForgeDirection fillSide = side.getOpposite();

		if (otherTank instanceof IFluidHandler) return ((IFluidHandler)otherTank).fill(fillSide, toFill, true);
		return 0;
	}

	public void distributeToSides(int amountPerTick, World world, Coord coord, Set<ForgeDirection> sides) {
		if (world == null) return;

		if (getFluidAmount() <= 0) return;

		periodicUpdateNeighbours(world, coord, sides);

		if (surroundingTanks.isEmpty()) return;

		FluidStack drainedFluid = drain(amountPerTick, false);

		if (drainedFluid != null && drainedFluid.amount > 0) {
			int startingAmount = drainedFluid.amount;
			Collections.shuffle(surroundingTanks);

			for (ForgeDirection side : surroundingTanks) {
				if (drainedFluid.amount <= 0) break;

				TileEntity otherTank = BlockUtils.getTileInDirection(world, coord, side);
				if (otherTank != null) drainedFluid.amount -= tryFillNeighbour(drainedFluid, side, otherTank);
			}

			// return any remainder
			int distributed = startingAmount - drainedFluid.amount;
			if (distributed > 0) drain(distributed, true);
		}
	}

	public void fillFromSides(int maxAmount, World world, Coord coord) {
		fillFromSides(maxAmount, world, coord, null);
	}

	public void fillFromSides(int maxAmount, World world, Coord coord, Set<ForgeDirection> sides) {
		if (world == null) return;

		int toDrain = Math.min(maxAmount, getSpace());
		if (toDrain <= 0) return;

		periodicUpdateNeighbours(world, coord, sides);

		if (surroundingTanks.isEmpty()) return;

		Collections.shuffle(surroundingTanks);
		MAIN: for (ForgeDirection side : surroundingTanks) {
			if (toDrain <= 0) break;
			TileEntity otherTank = BlockUtils.getTileInDirection(world, coord, side);
			if (otherTank instanceof IFluidHandler) {
				final ForgeDirection drainSide = side.getOpposite();
				final IFluidHandler handler = (IFluidHandler)otherTank;
				final FluidTankInfo[] infos = handler.getTankInfo(drainSide);

				if (infos == null) {
					/*
					 * Log.debug("Tank %s @ (%d,%d,%d) returned null tank info. Nasty.",
					 * otherTank.getClass(), otherTank.xCoord, otherTank.yCoord, otherTank.zCoord);
					 */// For the moment, mute this output until MinecraftForge#2085 concludes.
					continue;
				}

				for (FluidTankInfo info : infos) {
					if (filter.canAcceptFluid(info.fluid)) {
						FluidStack stack = info.fluid.copy();
						stack.amount = toDrain;

						FluidStack drained = handler.drain(drainSide, stack, true);

						if (drained != null) {
							fill(drained, true);
							toDrain -= drained.amount;
						}

						if (toDrain <= 0) break MAIN;
					}

				}

			}
		}
	}

}
