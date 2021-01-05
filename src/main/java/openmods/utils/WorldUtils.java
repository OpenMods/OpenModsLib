package openmods.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import openmods.OpenMods;

public class WorldUtils {

	public static final Predicate<Entity> NON_PLAYER = entity -> !(entity instanceof PlayerEntity);

	public static World getWorld(LogicalSide side, RegistryKey<World> dimension) {
		final World result;
		if (side == LogicalSide.SERVER) {
			result = LogicalSidedProvider.INSTANCE.<MinecraftServer>get(LogicalSide.SERVER).getWorld(dimension);
		} else {
			result = OpenMods.PROXY.getClientWorld();
			RegistryKey<World> actualDimKey = result.getDimensionKey();
			Preconditions.checkArgument(actualDimKey.equals(dimension), "Invalid client dimension id, expected: %s, has: %s", dimension, actualDimKey);
		}

		Preconditions.checkNotNull(result, "Invalid world dimension %s", dimension);
		return result;
	}

	public static boolean isTileEntityValid(TileEntity te) {
		if (te.isRemoved()) {
			return false;
		}

		final World world = te.getWorld();
		return (world != null) && world.isBlockLoaded(te.getPos());
	}
}
