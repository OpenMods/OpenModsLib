package openmods.utils;

import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.wrappers.FluidHandlerWrapper;

@SuppressWarnings("deprecation")
public class CompatibilityUtils {

	@Nullable
	public static IFluidHandler getFluidHandler(TileEntity te, EnumFacing side) {
		if (te == null) return null;
		final IFluidHandler nativeCapability = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
		if (nativeCapability != null) return nativeCapability;

		if (te instanceof net.minecraftforge.fluids.IFluidHandler) return new FluidHandlerWrapper((net.minecraftforge.fluids.IFluidHandler)te, side);

		return null;
	}

	@Nullable
	public static boolean isFluidHandler(TileEntity te, EnumFacing side) {
		return te != null && (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side) ||
				(te instanceof net.minecraftforge.fluids.IFluidHandler));
	}

}
