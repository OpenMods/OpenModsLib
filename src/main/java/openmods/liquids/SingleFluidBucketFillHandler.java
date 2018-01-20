package openmods.liquids;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

public class SingleFluidBucketFillHandler {

	@ObjectHolder("minecraft:bucket")
	private static final Item EMPTY_BUCKET = null;

	@Nonnull
	private final ItemStack filledBucket;

	private final FluidStack containedFluid;

	public SingleFluidBucketFillHandler(@Nonnull ItemStack filledBucket) {
		this.filledBucket = filledBucket;
		this.containedFluid = FluidUtil.getFluidContained(filledBucket);
		Preconditions.checkState(containedFluid != null, "Item %s is not a filled bucket", filledBucket);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onBucketFill(FillBucketEvent evt) {
		if (evt.getResult() != Result.DEFAULT) return;

		if (evt.getEmptyBucket().getItem() != EMPTY_BUCKET) return;

		final RayTraceResult target = evt.getTarget();
		if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return;

		final TileEntity te = evt.getWorld().getTileEntity(target.getBlockPos());
		if (te == null) return;

		if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, target.sideHit)) {
			final IFluidHandler source = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, target.sideHit);

			final FluidStack drained = source.drain(containedFluid, false);
			if (containedFluid.isFluidStackIdentical(drained)) {
				source.drain(containedFluid, true);
				evt.setFilledBucket(filledBucket.copy());
				evt.setResult(Result.ALLOW);
			}
		}
	}

}
