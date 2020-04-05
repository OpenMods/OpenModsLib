package openmods.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.LogicalSide;
import openmods.OpenMods;

public class WorldUtils {

	public static final Predicate<Entity> NON_PLAYER = entity -> !(entity instanceof PlayerEntity);

	public static World getWorld(LogicalSide side, DimensionType dimension) {
		final World result;
		if (side == LogicalSide.SERVER) {
			result = OpenMods.proxy.getServerWorld(dimension);
		} else {
			result = OpenMods.proxy.getClientWorld();
			Preconditions.checkArgument(result.getDimension().getType().equals(dimension), "Invalid client dimension id %s", dimension);
		}

		Preconditions.checkNotNull(result, "Invalid world dimension %s", dimension);
		return result;
	}

	public static boolean isTileEntityValid(TileEntity te) {
		if (te.isRemoved()) return false;

		final World world = te.getWorld();
		return (world != null) && world.isBlockLoaded(te.getPos());
	}

}
