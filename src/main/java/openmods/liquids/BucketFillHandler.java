package openmods.liquids;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BucketFillHandler {

	public static final BucketFillHandler instance = new BucketFillHandler();

	private BucketFillHandler() {}

	private final Set<Class<? extends IFluidHandler>> whitelist = Sets.newHashSet();

	@SuppressWarnings("unchecked")
	public void addToWhitelist(Class<? extends TileEntity> cls) {
		whitelist.add((Class<? extends IFluidHandler>)cls);
	}

	private boolean shouldFill(Object target) {
		return (target instanceof IFluidHandler) && whitelist.contains(target.getClass());
	}

	private static ItemStack fillTank(IFluidHandler handler, EnumFacing dir, ItemStack container) {
		FluidTankInfo tanks[] = handler.getTankInfo(dir);

		for (FluidTankInfo tank : tanks) {
			FluidStack available = tank.fluid;
			if (available == null || available.amount <= 0) continue;

			ItemStack filledStack = FluidContainerRegistry.fillFluidContainer(available, container);
			FluidStack filled = FluidContainerRegistry.getFluidForFilledItem(filledStack);

			if (filled != null && filled.isFluidEqual(available) && filled.amount <= available.amount) {
				FluidStack drained = handler.drain(dir, filled.amount, false);

				if (drained.isFluidStackIdentical(filled)) {
					handler.drain(dir, filled.amount, true);
					return filledStack;
				}
			}
		}

		return null;
	}

	@SubscribeEvent
	public void onBucketFill(FillBucketEvent evt) {
		if (evt.getResult() != Result.DEFAULT) return;

		final RayTraceResult target = evt.getTarget();
		if (target == null) return;
		if (target.typeOfHit != RayTraceResult.Type.BLOCK) return;

		TileEntity te = evt.getWorld().getTileEntity(target.getBlockPos());

		if (shouldFill(te)) {
			ItemStack result = fillTank((IFluidHandler)te, target.sideHit, evt.getEmptyBucket());
			if (result != null) {
				evt.setFilledBucket(result);
				evt.setResult(Result.ALLOW);
			}
		}
	}

}
