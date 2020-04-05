package openmods.liquids;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.commons.lang3.tuple.Pair;

public abstract class ContainerBucketFillHandler {

	@ObjectHolder("minecraft:bucket")
	private static Item EMPTY_BUCKET = null;

	protected abstract boolean canFill(World world, BlockPos pos, TileEntity te);

	private final List<Pair<FluidStack, ItemStack>> buckets = Lists.newArrayList();

	public ContainerBucketFillHandler addFilledBucket(ItemStack filledBucket) {
		LazyOptional<FluidStack> containedFluid = FluidUtil.getFluidContained(filledBucket);
		containedFluid.ifPresent(fluidStack -> {
			buckets.add(Pair.of(fluidStack, filledBucket.copy()));
		});
		return this;
	}

	@SubscribeEvent
	public void onBucketFill(FillBucketEvent evt) {
		if (evt.getResult() != Event.Result.DEFAULT) return;

		if (evt.getEmptyBucket().getItem() != EMPTY_BUCKET) return;

		final RayTraceResult target = evt.getTarget();
		if (target == null || target.getType() != RayTraceResult.Type.BLOCK) return;
		final BlockRayTraceResult blockTarget = (BlockRayTraceResult)target;

		final TileEntity te = evt.getWorld().getTileEntity(blockTarget.getPos());
		if (te == null) return;

		if (!canFill(evt.getWorld(), blockTarget.getPos(), te)) { return; }

		final LazyOptional<IFluidHandler> capability = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, blockTarget.getFace());
		capability.ifPresent(source -> {
			final FluidStack fluidInContainer = source.drain(FluidAttributes.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);

			if (!fluidInContainer.isEmpty()) {
				final ItemStack filledBucket = getFilledBucket(fluidInContainer);
				if (!filledBucket.isEmpty()) {
					FluidUtil.getFluidHandler(filledBucket).ifPresent(c -> {
						final FluidStack fluidInBucket = c.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
						if (fluidInBucket.isEmpty() && fluidInBucket.isFluidStackIdentical(source.drain(fluidInBucket, IFluidHandler.FluidAction.SIMULATE))) {
							source.drain(fluidInBucket, IFluidHandler.FluidAction.EXECUTE);
							evt.setFilledBucket(filledBucket.copy());
							evt.setResult(Event.Result.ALLOW);
						}
					});
				}
			}
		});
	}

	private ItemStack getFilledBucket(FluidStack fluid) {
		for (Pair<FluidStack, ItemStack> e : buckets) {
			if (e.getLeft().isFluidEqual(fluid))
				return e.getRight();
		}

		return FluidUtil.getFilledBucket(fluid);
	}

}
