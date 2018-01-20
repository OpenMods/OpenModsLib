package openmods.liquids;

import com.google.common.collect.Lists;
import java.util.List;
import jline.internal.Log;
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
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import org.apache.commons.lang3.tuple.Pair;

public abstract class ContainerBucketFillHandler {

	@ObjectHolder("minecraft:bucket")
	private static final Item EMPTY_BUCKET = null;

	protected abstract boolean canFill(World world, BlockPos pos, TileEntity te);

	private final List<Pair<FluidStack, ItemStack>> buckets = Lists.newArrayList();

	public ContainerBucketFillHandler addFilledBucket(ItemStack filledBucket) {
		FluidStack containedFluid = FluidUtil.getFluidContained(filledBucket);
		if (containedFluid != null) {
			buckets.add(Pair.of(containedFluid.copy(), filledBucket.copy()));
		} else {
			Log.warn("Item %s is not a filled bucket", filledBucket);
		}
		return this;
	}

	@SubscribeEvent
	public void onBucketFill(FillBucketEvent evt) {
		if (evt.getResult() != Result.DEFAULT) return;

		if (evt.getEmptyBucket().getItem() != EMPTY_BUCKET) return;

		final RayTraceResult target = evt.getTarget();
		if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return;

		final TileEntity te = evt.getWorld().getTileEntity(target.getBlockPos());
		if (te == null) return;

		if (!canFill(evt.getWorld(), target.getBlockPos(), te)) { return; }

		if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, target.sideHit)) {
			final IFluidHandler source = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, target.sideHit);

			final FluidStack fluidInContainer = source.drain(Fluid.BUCKET_VOLUME, false);

			if (fluidInContainer != null) {
				final ItemStack filledBucket = getFilledBucket(fluidInContainer);
				if (!filledBucket.isEmpty()) {
					final IFluidHandlerItem container = FluidUtil.getFluidHandler(filledBucket);
					if (container != null) {
						final FluidStack fluidInBucket = container.drain(Integer.MAX_VALUE, false);
						if (fluidInBucket != null && fluidInBucket.isFluidStackIdentical(source.drain(fluidInBucket, false))) {
							source.drain(fluidInBucket, true);
							evt.setFilledBucket(filledBucket.copy());
							evt.setResult(Result.ALLOW);
						}
					}
				}
			}
		}
	}

	private ItemStack getFilledBucket(FluidStack fluid) {
		for (Pair<FluidStack, ItemStack> e : buckets) {
			if (e.getLeft().isFluidEqual(fluid))
				return e.getRight();
		}

		return FluidUtil.getFilledBucket(fluid);
	}

}
