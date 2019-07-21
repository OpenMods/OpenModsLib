package openmods.utils;

import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class CompatibilityUtils {

	@Nullable
	public static IFluidHandler getFluidHandler(TileEntity te, Direction side) {
		if (te == null) return null;
		final IFluidHandler nativeCapability = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
		if (nativeCapability != null) return nativeCapability;

		return null;
	}

	public static boolean isFluidHandler(TileEntity te, Direction side) {
		return te != null && (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side));
	}

}
