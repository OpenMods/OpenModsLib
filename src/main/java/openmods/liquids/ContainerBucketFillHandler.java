package openmods.liquids;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

public abstract class ContainerBucketFillHandler {

	@ObjectHolder("minecraft:bucket")
	public static final Item EMPTY_BUCKET = null;

	protected abstract boolean canFill(World world, BlockPos pos, TileEntity te);

	@SubscribeEvent
	public void onBucketFill(FillBucketEvent evt) {
		if (evt.getResult() != Event.Result.DEFAULT) return;

		if (evt.getEmptyBucket().getItem() != EMPTY_BUCKET) return;

		final RayTraceResult target = evt.getTarget();
		if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return;

		final TileEntity te = evt.getWorld().getTileEntity(target.getBlockPos());
		if (te == null) return;

		if (!canFill(evt.getWorld(), target.getBlockPos(), te)) return;

		if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, target.sideHit)) {
			final IFluidHandler source = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, target.sideHit);

			final FluidStack fluidInContainer = source.drain(Fluid.BUCKET_VOLUME, false);

			if (fluidInContainer != null) {
				final ItemStack bucketToFill = evt.getEmptyBucket().copy();
				final IFluidHandler bucketContainer = FluidUtil.getFluidHandler(bucketToFill);
				if (bucketContainer != null) {
					if (bucketContainer.fill(fluidInContainer, false) == fluidInContainer.amount) {
						source.drain(fluidInContainer, true);
						bucketContainer.fill(fluidInContainer, true);
						evt.setFilledBucket(bucketToFill);
						evt.setResult(Event.Result.ALLOW);
					}
				}
			}
		}
	}
}
