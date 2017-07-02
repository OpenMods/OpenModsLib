package openmods.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import openmods.OpenMods;

public class WorldUtils {

	public static final Predicate<Entity> NON_PLAYER = new Predicate<Entity>() {
		@Override
		public boolean apply(Entity entity) {
			return !(entity instanceof EntityPlayer);
		}
	};

	public static World getWorld(Side side, int dimensionId) {
		final World result;
		if (side == Side.SERVER) {
			result = OpenMods.proxy.getServerWorld(dimensionId);
		} else {
			result = OpenMods.proxy.getClientWorld();
			Preconditions.checkArgument(result.provider.getDimension() == dimensionId, "Invalid client dimension id %s", dimensionId);
		}

		Preconditions.checkNotNull(result, "Invalid world dimension %s", dimensionId);
		return result;
	}

	public static boolean isTileEntityValid(TileEntity te) {
		if (te.isInvalid()) return false;

		final World world = te.getWorld();
		return (world != null)? world.isBlockLoaded(te.getPos()) : false;
	}

}
