package openmods.liquids;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BucketFillHandler {

	private final ItemStack filledBucket;

	private final FluidStack containedFluid;

	public BucketFillHandler(ItemStack filledBucket, FluidStack containedFluid) {
		this.filledBucket = filledBucket;
		this.containedFluid = containedFluid;
	}

	@SubscribeEvent
	public void onBucketFill(FillBucketEvent evt) {
		if (evt.getResult() != Result.DEFAULT) return;

		final RayTraceResult target = evt.getTarget();
		if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return;

		final TileEntity te = evt.getWorld().getTileEntity(target.getBlockPos());
		if (te == null) return;

		if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, target.sideHit)) {
			final IFluidHandler source = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, target.sideHit);

			final FluidStack drained = source.drain(Fluid.BUCKET_VOLUME, false);
			if (containedFluid.isFluidStackIdentical(drained)) {
				source.drain(Fluid.BUCKET_VOLUME, true);
				evt.setFilledBucket(filledBucket.copy());
				evt.setResult(Result.ALLOW);
			}
		}
	}

}
