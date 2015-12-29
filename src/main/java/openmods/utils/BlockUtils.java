package openmods.utils;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import openmods.geometry.Orientation;
import openmods.inventory.IInventoryProvider;

public class BlockUtils {

	public static EnumFacing get2dOrientation(EntityLivingBase entity) {
		int l = MathHelper.floor_double(entity.rotationYaw * 4.0F / 360.0F + 0.5D) & 0x3;
		switch (l) {
			case 0:
				return EnumFacing.SOUTH;
			case 1:
				return EnumFacing.WEST;
			case 2:
				return EnumFacing.NORTH;
			case 3:
				return EnumFacing.EAST;
		}
		return EnumFacing.SOUTH;

	}

	public static float getRotationFromDirection(EnumFacing direction) {
		switch (direction) {
			case NORTH:
				return 0F;
			case SOUTH:
				return 180F;
			case WEST:
				return 90F;
			case EAST:
				return -90F;
			case DOWN:
				return -90f;
			case UP:
				return 90f;
			default:
				return 0f;
		}
	}

	public static float getRotationFromOrientation(Orientation orientation) {
		switch (orientation.x) {
			case POS_X:
				return 0f;
			case NEG_X:
				return 180f;
			case NEG_Z:
				return 90f;
			case POS_Z:
				return -90f;
			default:
				return 0f;
		}
	}

	public static EnumFacing get3dOrientation(EntityLivingBase entity) {
		if (entity.rotationPitch > 45.5F) {
			return EnumFacing.DOWN;
		} else if (entity.rotationPitch < -45.5F) { return EnumFacing.UP; }
		return get2dOrientation(entity);
	}

	public static EntityItem dropItemStackInWorld(World worldObj, double x, double y, double z, ItemStack stack) {
		float f = 0.7F;
		float d0 = worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5F;
		float d1 = worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5F;
		float d2 = worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5F;
		EntityItem entityitem = new EntityItem(worldObj, x + d0, y + d1, z + d2, stack);
		entityitem.setDefaultPickupDelay();
		if (stack.hasTagCompound()) {
			entityitem.getEntityItem().setTagCompound((NBTTagCompound)stack.getTagCompound().copy());
		}
		worldObj.spawnEntityInWorld(entityitem);
		return entityitem;
	}

	public static EntityItem ejectItemInDirection(World world, double x, double y, double z, EnumFacing direction, ItemStack stack) {
		EntityItem item = BlockUtils.dropItemStackInWorld(world, x, y, z, stack);
		final Vec3i v = direction.getDirectionVec();
		item.motionX = v.getX() / 5F;
		item.motionY = v.getY() / 5F;
		item.motionZ = v.getZ() / 5F;
		return item;
	}

	public static boolean getTileInventoryDrops(TileEntity tileEntity, List<ItemStack> drops) {
		if (tileEntity == null) return false;

		if (tileEntity instanceof IInventory) {
			drops.addAll(InventoryUtils.getInventoryContents((IInventory)tileEntity));
			return true;
		} else if (tileEntity instanceof IInventoryProvider) {
			drops.addAll(InventoryUtils.getInventoryContents(((IInventoryProvider)tileEntity).getInventory()));
			return true;
		}

		return false;
	}

	public static void dropInventory(IInventory inventory, World world, double x, double y, double z) {
		if (inventory == null) { return; }
		for (int i = 0; i < inventory.getSizeInventory(); ++i) {
			ItemStack itemStack = inventory.getStackInSlot(i);
			if (itemStack != null) {
				dropItemStackInWorld(world, x, y, z, itemStack);
			}
		}
	}

	public static void dropInventory(IInventory inventory, World world, int x, int y, int z) {
		dropInventory(inventory, world, x + 0.5, y + 0.5, z + 0.5);
	}

	public static TileEntity getTileInDirection(TileEntity tile, EnumFacing direction) {
		final BlockPos offset = tile.getPos().offset(direction);
		return tile.getWorld().getTileEntity(offset);
	}

	public static TileEntity getTileInDirection(World world, BlockPos coord, EnumFacing direction) {
		return world.getTileEntity(coord.offset(direction));
	}

}
