package openmods.liquids;

import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class SingleFluidBucketHandler implements IFluidHandler {

	private final ItemStack filledContainer;

	private final int volume;

	private final ItemStack emptyContainer;

	private final FluidStack contents;

	private final IFluidTankProperties properties;

	private boolean isEmpty;

	public SingleFluidBucketHandler(ItemStack filledContainer, ItemStack emptyContainer, Fluid fluid, final int volume) {
		this.volume = volume;
		this.filledContainer = filledContainer;
		this.emptyContainer = emptyContainer.copy();

		this.contents = new FluidStack(fluid, volume);
		this.properties = new IFluidTankProperties() {

			@Override
			public FluidStack getContents() {
				return isEmpty? null : contents.copy();
			}

			@Override
			public int getCapacity() {
				return volume;
			}

			@Override
			public boolean canFillFluidType(FluidStack fluidStack) {
				return false;
			}

			@Override
			public boolean canFill() {
				return false;
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

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		return 0;
	}

	protected void switchToEmptyBucket() {
		filledContainer.deserializeNBT(emptyContainer.serializeNBT());
		isEmpty = true;
	}

	private boolean isValidResource(FluidStack resource) {
		return contents.isFluidEqual(resource) && resource.amount >= volume;
	}

	@Override
	@Nullable
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if (isEmpty || filledContainer.stackSize != 1 || !isValidResource(resource))
			return null;

		if (doDrain)
			switchToEmptyBucket();

		return contents.copy();
	}

	@Override
	@Nullable
	public FluidStack drain(int maxDrain, boolean doDrain) {
		if (isEmpty || filledContainer.stackSize != 1 || maxDrain < volume)
			return null;

		if (doDrain)
			switchToEmptyBucket();

		return contents.copy();
	}

}