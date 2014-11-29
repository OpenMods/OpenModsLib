package openmods.utils;

import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import openmods.OpenMods;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class WorldUtils {

	@SuppressWarnings("unchecked")
	public static <T extends Entity> List<T> getEntitiesWithinAABB(World world, Class<? extends T> cls, AxisAlignedBB aabb) {
		return world.getEntitiesWithinAABB(cls, aabb);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Entity> List<T> getEntitiesWithinAABB(World world, Class<? extends T> cls, AxisAlignedBB aabb, IEntitySelector selector) {
		return world.selectEntitiesWithinAABB(cls, aabb, selector);
	}

	@SuppressWarnings("unchecked")
	public static List<Entity> getEntitiesWithinAABBExcluding(Entity excluding, World world, AxisAlignedBB aabb) {
		return world.getEntitiesWithinAABBExcludingEntity(excluding, aabb);
	}

	@SuppressWarnings("unchecked")
	public static List<Entity> getEntitiesWithinAABB(Entity excluding, World world, AxisAlignedBB aabb, IEntitySelector selector) {
		return world.getEntitiesWithinAABBExcludingEntity(excluding, aabb, selector);
	}

	public static final IEntitySelector NON_PLAYER = new IEntitySelector() {
		@Override
		public boolean isEntityApplicable(Entity entity) {
			return !(entity instanceof EntityPlayer);
		}
	};

	public static World getWorld(int dimensionId) {
		World result;
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
			result = OpenMods.proxy.getServerWorld(dimensionId);
		} else {
			result = OpenMods.proxy.getClientWorld();
			Preconditions.checkArgument(result.provider.dimensionId == dimensionId, "Invalid client dimension id %s", dimensionId);
		}

		Preconditions.checkNotNull(result, "Invalid world dimension %s", dimensionId);
		return result;
	}

	public static boolean isTileEntityValid(TileEntity te) {
		if (te.isInvalid()) return false;

		final World world = te.getWorldObj();
		return (world != null)? world.blockExists(te.xCoord, te.yCoord, te.zCoord) : false;
	}

}
