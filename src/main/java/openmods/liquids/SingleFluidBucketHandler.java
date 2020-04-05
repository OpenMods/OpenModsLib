package openmods.liquids;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class SingleFluidBucketHandler implements IFluidHandlerItem {

	private final int volume;

	private final FluidStack contents;

	@Nonnull
	private final ItemStack emptyContainer;

	@Nonnull
	private final ItemStack filledContainer;

	private boolean isFilled = true;

	public SingleFluidBucketHandler(@Nonnull ItemStack filledContainer, @Nonnull ItemStack emptyContainer, @Nonnull Fluid fluid, final int volume) {
		this.volume = volume;
		this.filledContainer = filledContainer;
		this.emptyContainer = emptyContainer;

		this.contents = new FluidStack(fluid, volume);
	}


	private boolean isResourceValid(@Nullable FluidStack resource) {
		return contents.isFluidEqual(resource) && resource.getAmount() >= volume;
	}

	private ItemStack getContainerRaw() {
		return isFilled? filledContainer : emptyContainer;
	}

	private boolean isContainerSingleItem() {
		return getContainerRaw().getCount() == 1;
	}

	@Override public int getTanks() {
		return 1;
	}

	@Nonnull
	@Override
	public FluidStack getFluidInTank(int tank) {
		return isFilled ? contents.copy() : FluidStack.EMPTY;
	}

	@Override
	public int getTankCapacity(int tank) {
		return volume;
	}

	@Override
	public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
		return isResourceValid(stack);
	}

	@Override
	public int fill(@Nullable FluidStack resource, FluidAction action) {
		if (isFilled || !isResourceValid(resource) || !isContainerSingleItem())
			return 0;

		if (action.execute())
			isFilled = true;

		return volume;
	}

	@Override
	@Nullable
	public FluidStack drain(@Nullable FluidStack resource, FluidAction action) {
		if (!isFilled || !isResourceValid(resource) || !isContainerSingleItem())
			return null;

		if (action.execute())
			isFilled = false;

		return contents.copy();
	}

	@Override
	@Nullable
	public FluidStack drain(int maxDrain, FluidAction action) {
		if (!isFilled || maxDrain < volume || !isContainerSingleItem())
			return null;

		if (action.execute())
			isFilled = false;

		return contents.copy();
	}

	@Override
	public ItemStack getContainer() {
		return getContainerRaw().copy();
	}

}
