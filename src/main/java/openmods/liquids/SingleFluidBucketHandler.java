package openmods.liquids;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class SingleFluidBucketHandler implements IFluidHandlerItem {

	private final int volume;

	private final FluidStack contents;

	@Nonnull
	private final ItemStack emptyContainer;

	@Nonnull
	private final ItemStack filledContainer;

	private boolean isFilled = true;

	private final IFluidTankProperties properties;

	public SingleFluidBucketHandler(@Nonnull ItemStack filledContainer, @Nonnull ItemStack emptyContainer, @Nonnull Fluid fluid, final int volume) {
		this.volume = volume;
		this.filledContainer = filledContainer;
		this.emptyContainer = emptyContainer;

		this.contents = new FluidStack(fluid, volume);
		this.properties = new IFluidTankProperties() {

			@Override
			public FluidStack getContents() {
				return isFilled? contents.copy() : null;
			}

			@Override
			public int getCapacity() {
				return volume;
			}

			@Override
			public boolean canFillFluidType(FluidStack fluidStack) {
				return contents.isFluidEqual(fluidStack);
			}

			@Override
			public boolean canFill() {
				return true;
			}

			@Override
			public boolean canDrainFluidType(FluidStack fluidStack) {
				return contents.isFluidEqual(fluidStack);
			}

			@Override
			public boolean canDrain() {
				return true;
			}
		};
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[] { properties };
	}

	private boolean isResourceValid(@Nullable FluidStack resource) {
		return contents.isFluidEqual(resource) && resource.amount >= volume;
	}

	private ItemStack getContainerRaw() {
		return isFilled? filledContainer : emptyContainer;
	}

	private boolean isContainerSingleItem() {
		return getContainerRaw().getCount() == 1;
	}

	@Override
	public int fill(@Nullable FluidStack resource, boolean doFill) {
		if (isFilled || !isResourceValid(resource) || !isContainerSingleItem())
			return 0;

		if (doFill)
			isFilled = true;

		return volume;
	}

	@Override
	@Nullable
	public FluidStack drain(@Nullable FluidStack resource, boolean doDrain) {
		if (!isFilled || !isResourceValid(resource) || !isContainerSingleItem())
			return null;

		if (doDrain)
			isFilled = false;

		return contents.copy();
	}

	@Override
	@Nullable
	public FluidStack drain(int maxDrain, boolean doDrain) {
		if (!isFilled || maxDrain < volume || !isContainerSingleItem())
			return null;

		if (doDrain)
			isFilled = false;

		return contents.copy();
	}

	@Override
	public ItemStack getContainer() {
		return getContainerRaw().copy();
	}

}
