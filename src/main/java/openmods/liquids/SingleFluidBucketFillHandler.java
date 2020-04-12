package openmods.liquids;

import java.util.Optional;
import javax.annotation.Nonnull;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ObjectHolder;

public class SingleFluidBucketFillHandler {

	@ObjectHolder("minecraft:bucket")
	private static Item EMPTY_BUCKET = null;

	@Nonnull
	private final ItemStack filledBucket;

	private final FluidStack containedFluid;

	public SingleFluidBucketFillHandler(@Nonnull ItemStack filledBucket) {
		this.filledBucket = filledBucket;
		final Optional<FluidStack> fluidContained = FluidUtil.getFluidContained(filledBucket);
		this.containedFluid = fluidContained.orElseThrow(() -> new IllegalStateException("Item " + filledBucket + " is not a filled bucket"));
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onBucketFill(FillBucketEvent evt) {
		if (evt.getResult() != Event.Result.DEFAULT) return;

		if (evt.getEmptyBucket().getItem() != EMPTY_BUCKET) return;

		final RayTraceResult target = evt.getTarget();
		if (target == null || target.getType() != RayTraceResult.Type.BLOCK) return;
		final BlockRayTraceResult blockTarget = (BlockRayTraceResult)target;
		final TileEntity te = evt.getWorld().getTileEntity(blockTarget.getPos());
		if (te == null) return;

		final LazyOptional<IFluidHandler> capability = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, blockTarget.getFace());
		capability.ifPresent(source -> {
			final FluidStack drained = source.drain(containedFluid, IFluidHandler.FluidAction.SIMULATE);
			if (containedFluid.isFluidStackIdentical(drained)) {
				source.drain(containedFluid, IFluidHandler.FluidAction.EXECUTE);
				evt.setFilledBucket(filledBucket.copy());
				evt.setResult(Event.Result.ALLOW);
			}
		});
	}

}
