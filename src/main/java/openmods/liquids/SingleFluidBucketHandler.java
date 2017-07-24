package openmods.liquids;

import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class SingleFluidBucketHandler implements IFluidHandler {

	private final ItemStack container;

	private final int volume;

	private final Fluid fluid;

	private final ItemStack emptyContainer;

	private final FluidStack contents;

	private final IFluidTankProperties properties;

	public SingleFluidBucketHandler(ItemStack container, String fluidId, int volume, ItemStack emptyContainer) {
		this.container = container;
		this.fluid = FluidRegistry.getFluid(fluidId);
		this.volume = volume;
		this.emptyContainer = emptyContainer.copy();

		this.contents = new FluidStack(fluid, volume);
		this.properties = new FluidTankProperties(contents, volume);
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
		container.deserializeNBT(emptyContainer.serializeNBT());
	}

	@Override
	@Nullable
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if (container.getCount() != 1 || resource == null || resource.amount < volume)
			return null;

		if (resource.getFluid() == fluid)
			if (doDrain)
				switchToEmptyBucket();

		return contents;
	}

	@Override
	@Nullable
	public FluidStack drain(int maxDrain, boolean doDrain) {
		if (container.getCount() != 1 || maxDrain < Fluid.BUCKET_VOLUME)
			return null;

		if (doDrain)
			switchToEmptyBucket();

		return contents;
	}

}